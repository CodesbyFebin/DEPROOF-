package com.deproof.domain

sealed class InstructionPolicy {
    abstract val policyName: String
    abstract val policyVersion: Int

    object ReadOnly : InstructionPolicy() {
        override val policyName = "READ_ONLY"
        override val policyVersion = 1
    }

    data class TokenTransfer(
        val mint: PublicKey,
        val maxAmount: Long,
        val destinationWhitelist: Set<String>,
        override val policyName: String = "TOKEN_TRANSFER",
        override val policyVersion: Int = 1
    ) : InstructionPolicy() {
        init {
            require(maxAmount > 0) { "Max amount must be positive" }
            require(destinationWhitelist.isNotEmpty()) { "Whitelist cannot be empty" }
            require(destinationWhitelist.all { it.isNotEmpty() }) { "All addresses must be non-empty" }
        }
    }

    data class TokenMint(
        val mint: PublicKey,
        val maxAmount: Long,
        override val policyName: String = "TOKEN_MINT",
        override val policyVersion: Int = 1
    ) : InstructionPolicy() {
        init {
            require(maxAmount > 0) { "Max amount must be positive" }
        }
    }

    data class TokenBurn(
        val mint: PublicKey,
        val maxAmount: Long,
        override val policyName: String = "TOKEN_BURN",
        override val policyVersion: Int = 1
    ) : InstructionPolicy() {
        init {
            require(maxAmount > 0) { "Max amount must be positive" }
        }
    }

    data class SolTransfer(
        val maxAmount: Long,
        val destinationWhitelist: Set<String>,
        override val policyName: String = "SOL_TRANSFER",
        override val policyVersion: Int = 1
    ) : InstructionPolicy() {
        init {
            require(maxAmount > 0) { "Max amount must be positive" }
            require(destinationWhitelist.isNotEmpty()) { "Whitelist cannot be empty" }
        }
    }

    data class StakingAction(
        val poolId: PublicKey,
        val allowedOperations: Set<StakingOperationType>,
        val maxSlippageBps: Int = 500,
        val maxAmount: Long? = null,
        override val policyName: String = "STAKING_ACTION",
        override val policyVersion: Int = 1
    ) : InstructionPolicy() {
        init {
            require(allowedOperations.isNotEmpty()) { "At least one operation must be allowed" }
            require(maxSlippageBps in 0..10000) { "Slippage must be 0-10000 bps" }
            if (maxAmount != null) {
                require(maxAmount > 0) { "Max amount must be positive" }
            }
        }
    }

    data class DeFiSwap(
        val poolId: PublicKey,
        val tokenInMint: PublicKey,
        val tokenOutMint: PublicKey,
        val maxAmountIn: Long,
        val minAmountOut: Long,
        val maxSlippageBps: Int = 500,
        override val policyName: String = "DEFI_SWAP",
        override val policyVersion: Int = 1
    ) : InstructionPolicy() {
        init {
            require(maxAmountIn > 0) { "Max amount in must be positive" }
            require(minAmountOut >= 0) { "Min amount out must be non-negative" }
            require(maxSlippageBps in 0..10000) { "Slippage must be 0-10000 bps" }
        }
    }

    data class NftMintPolicy(
        val metaplexProgram: PublicKey,
        val maxSupply: Long,
        val allowedCreators: Set<String>,
        override val policyName: String = "NFT_MINT",
        override val policyVersion: Int = 1
    ) : InstructionPolicy() {
        init {
            require(maxSupply > 0) { "Max supply must be positive" }
            require(allowedCreators.isNotEmpty()) { "At least one creator must be allowed" }
        }
    }

    data class NftTransferPolicy(
        val allowedCollections: Set<String>,
        val allowedRecipients: Set<String>,
        override val policyName: String = "NFT_TRANSFER",
        override val policyVersion: Int = 1
    ) : InstructionPolicy() {
        init {
            require(allowedCollections.isNotEmpty()) { "At least one collection must be allowed" }
            require(allowedRecipients.isNotEmpty()) { "At least one recipient must be allowed" }
        }
    }

    data class GovernanceVotePolicy(
        val governance: PublicKey,
        val allowedVotingOptions: Set<Int>,
        override val policyName: String = "GOVERNANCE_VOTE",
        override val policyVersion: Int = 1
    ) : InstructionPolicy() {
        init {
            require(allowedVotingOptions.isNotEmpty()) { "At least one voting option must be allowed" }
            require(allowedVotingOptions.all { it >= 0 }) { "Voting options must be non-negative" }
        }
    }

    data class LiquidityPoolPolicy(
        val poolId: PublicKey,
        val tokenAMint: PublicKey,
        val tokenBMint: PublicKey,
        val maxAmountA: Long,
        val maxAmountB: Long,
        val allowedOperations: Set<LpOperationType>,
        override val policyName: String = "LIQUIDITY_POOL",
        override val policyVersion: Int = 1
    ) : InstructionPolicy() {
        init {
            require(maxAmountA > 0) { "Max amount A must be positive" }
            require(maxAmountB > 0) { "Max amount B must be positive" }
            require(allowedOperations.isNotEmpty()) { "At least one operation must be allowed" }
        }
    }

    data class CustomProgramPolicy(
        val programId: PublicKey,
        val description: String,
        val allowedInstructionSignatures: Set<String> = emptySet(),
        override val policyName: String = "CUSTOM_PROGRAM",
        override val policyVersion: Int = 1
    ) : InstructionPolicy() {
        init {
            require(description.isNotEmpty()) { "Description cannot be empty" }
        }
    }

    data class CompositePolicy(
        val policies: List<InstructionPolicy>,
        val requireAll: Boolean = true,
        override val policyName: String = "COMPOSITE",
        override val policyVersion: Int = 1
    ) : InstructionPolicy() {
        init {
            require(policies.isNotEmpty()) { "At least one policy must be present" }
        }
    }
}

enum class StakingOperationType {
    STAKE,
    UNSTAKE,
    CLAIM_REWARDS,
    DELEGATE,
    DEACTIVATE,
    WITHDRAW
}

enum class LpOperationType {
    ADD_LIQUIDITY,
    REMOVE_LIQUIDITY,
    SWAP
}

interface PolicySerializer {
    fun serialize(policy: InstructionPolicy): ByteArray
    fun deserialize(bytes: ByteArray): Result<InstructionPolicy>
}

class JsonPolicySerializer : PolicySerializer {
    override fun serialize(policy: InstructionPolicy): ByteArray {
        val json = when (policy) {
            is InstructionPolicy.ReadOnly -> """{"type":"READ_ONLY","version":1}"""
            is InstructionPolicy.TokenTransfer -> {
                """{"type":"TOKEN_TRANSFER","mint":"${policy.mint}","maxAmount":${policy.maxAmount},"whitelist":${policy.destinationWhitelist},"version":${policy.policyVersion}}"""
            }
            is InstructionPolicy.SolTransfer -> {
                """{"type":"SOL_TRANSFER","maxAmount":${policy.maxAmount},"whitelist":${policy.destinationWhitelist},"version":${policy.policyVersion}}"""
            }
            is InstructionPolicy.StakingAction -> {
                """{"type":"STAKING_ACTION","poolId":"${policy.poolId}","operations":${policy.allowedOperations},"maxSlippage":${policy.maxSlippageBps},"version":${policy.policyVersion}}"""
            }
            else -> """{"type":"UNKNOWN","version":1}"""
        }
        return json.toByteArray(Charsets.UTF_8)
    }

    override fun deserialize(bytes: ByteArray): Result<InstructionPolicy> = runCatching {
        val json = String(bytes, Charsets.UTF_8)
        if (json.contains("READ_ONLY")) {
            InstructionPolicy.ReadOnly
        } else {
            throw IllegalArgumentException("Unsupported policy format")
        }
    }
}
