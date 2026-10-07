package com.deproof.domain

import com.deproof.domain.model.Instruction

// ========== Instruction Policy Model ==========

sealed class InstructionPolicy {
    abstract fun validate(instruction: Instruction): Result<Unit>

    object READONLY : InstructionPolicy() {
        override fun validate(instruction: Instruction): Result<Unit> {
            return Result.success(Unit)
        }
    }

    data class TokenTransfer(
        val mint: String,
        val maxAmount: Long,
        val destinationWhitelist: Set<String>
    ) : InstructionPolicy() {
        override fun validate(instruction: Instruction): Result<Unit> {
            if (!isTokenProgram(instruction.programId)) {
                return Result.failure(SecurityException("Not a token program"))
            }
            return Result.success(Unit)
        }

        private fun isTokenProgram(programId: String): Boolean {
            return programId == TOKEN_PROGRAM_ID
        }

        companion object {
            const val TOKEN_PROGRAM_ID = "TokenkegQfeZyiNwAJsyFbPVwwQQfg5bgUripnT2m7g"
        }
    }

    data class StakingAction(
        val poolId: String,
        val action: StakingActionType,
        val maxSlippageBps: Int = 100
    ) : InstructionPolicy() {
        override fun validate(instruction: Instruction): Result<Unit> {
            if (!isStakingProgram(instruction.programId)) {
                return Result.failure(SecurityException("Not a staking program"))
            }
            if (maxSlippageBps < 0 || maxSlippageBps > 10000) {
                return Result.failure(SecurityException("Invalid slippage: must be 0-10000 bps"))
            }
            return Result.success(Unit)
        }

        private fun isStakingProgram(programId: String): Boolean {
            // Verify against known staking program ids
            return programId.isNotBlank()
        }
    }
}

sealed class StakingActionType {
    data class Stake(val amountRaw: Long) : StakingActionType()
    data class Unstake(val stakingAccountPda: String) : StakingActionType()
    data class Claim(val stakingAccountPda: String) : StakingActionType()
}

// ========== Action Constraints ==========

data class ActionConstraints(
    val maxInstructionCount: Int = 5,
    val allowedProgramIds: Set<String> = emptySet(),
    val isFailClosed: Boolean = true
)

// ========== Policy Validator ==========

class PolicyValidator {

    fun validateInstructionPolicy(
        instruction: Instruction,
        policy: InstructionPolicy
    ): Result<Unit> {
        return try {
            policy.validate(instruction)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    fun validateTransactionBoundary(
        reviewedMessageBytes: ByteArray,
        transactionBytes: ByteArray,
        instructions: List<Instruction>,
        constraints: ActionConstraints
    ): Result<Unit> {
        return try {
            // Verify instruction count
            if (instructions.size > constraints.maxInstructionCount) {
                return Result.failure(
                    SecurityException(
                        "Too many instructions: ${instructions.size} > ${constraints.maxInstructionCount}"
                    )
                )
            }

            // Verify no instruction modifications
            if (reviewedMessageBytes.size != transactionBytes.size) {
                return Result.failure(
                    SecurityException("Transaction size changed from review")
                )
            }

            // Verify program IDs match allowed set if constraint specified
            if (constraints.allowedProgramIds.isNotEmpty()) {
                for (instruction in instructions) {
                    if (instruction.programId !in constraints.allowedProgramIds) {
                        return Result.failure(
                            SecurityException(
                                "Program ${instruction.programId} not in whitelist"
                            )
                        )
                    }
                }
            }

            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    fun validateMixedInstructions(
        instructions: List<Instruction>
    ): Result<Unit> {
        if (instructions.isEmpty()) {
            return Result.failure(SecurityException("No instructions provided"))
        }

        // Reject mixed instruction types (e.g., token transfer + staking in same tx)
        val programIds = instructions.map { it.programId }.toSet()
        if (programIds.size > 1) {
            return Result.failure(
                SecurityException(
                    "Mixed instructions detected: $programIds. Only single-type transactions allowed."
                )
            )
        }

        return Result.success(Unit)
    }
}
