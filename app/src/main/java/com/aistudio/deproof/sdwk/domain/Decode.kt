package com.aistudio.deproof.sdwk.domain

import com.aistudio.deproof.sdwk.chain.Instruction
import com.aistudio.deproof.sdwk.chain.Verdict
import com.aistudio.deproof.sdwk.util.readU32LE
import com.aistudio.deproof.sdwk.util.readU64LE
import com.aistudio.deproof.sdwk.util.shortKey

class InstructionDecoder {
    companion object {
        private const val TOKEN_PROGRAM = "TokenkegQfeZyiNwAJbNbGKPFXCWuBvf9Ss623VQ5DA"
        private const val SKR_MINT = "SKRbvo6Gf7GondiT3BbTfuRDPqLWei4j2Qy2NPGZhW3"
        private const val SOL_STAKE_PROGRAM = "Stake11111111111111111111111111111111111111"

        private const val DISCRIMINATOR_TRANSFER_CHECKED = 12
        private const val DISCRIMINATOR_TRANSFER = 3
        private const val DISCRIMINATOR_APPROVE = 4
        private const val DISCRIMINATOR_SET_AUTHORITY = 6

        fun decode(
            programId: String,
            accounts: List<String>,
            data: String,
            instruction: Instruction
        ): Verdict {
            return when {
                programId == TOKEN_PROGRAM -> decodeTokenInstruction(accounts, data, instruction)
                programId == SOL_STAKE_PROGRAM -> decodeSolStakeInstruction(data)
                else -> Verdict(
                    title = "Unknown Program",
                    reason = "Do not sign unknown programs",
                    isPayable = false,
                    summary = "Unknown program ${shortKey(programId)}"
                )
            }
        }

        private fun decodeTokenInstruction(
            accounts: List<String>,
            data: String,
            instruction: Instruction
        ): Verdict {
            return try {
                val bytes = android.util.Base64.decode(data, android.util.Base64.DEFAULT)
                if (bytes.isEmpty()) {
                    return Verdict(
                        title = "Malformed Instruction",
                        reason = "Data is empty",
                        isPayable = false,
                        summary = "Malformed instruction data"
                    )
                }

                val discriminator = bytes[0].toInt() and 0xFF

                return when (discriminator) {
                    DISCRIMINATOR_TRANSFER_CHECKED -> decodeTransferChecked(
                        bytes,
                        accounts,
                        instruction.accounts
                    )
                    DISCRIMINATOR_TRANSFER -> Verdict(
                        title = "Unchecked Transfer",
                        reason = "Do not sign unchecked transfers",
                        isPayable = false,
                        summary = "Unchecked Transfer is not allowed"
                    )
                    DISCRIMINATOR_APPROVE -> Verdict(
                        title = "Approve",
                        reason = "Do not sign Approve",
                        isPayable = false,
                        summary = "Approve is not allowed"
                    )
                    DISCRIMINATOR_SET_AUTHORITY -> Verdict(
                        title = "Set Authority",
                        reason = "Do not sign SetAuthority",
                        isPayable = false,
                        summary = "SetAuthority is not allowed"
                    )
                    else -> Verdict(
                        title = "Unknown Token Instruction",
                        reason = "Unknown discriminator: $discriminator",
                        isPayable = false,
                        summary = "Unknown instruction (discriminator $discriminator)"
                    )
                }
            } catch (e: Exception) {
                Verdict(
                    title = "Malformed Instruction",
                    reason = "Failed to decode: ${e.message}",
                    isPayable = false,
                    summary = "Malformed instruction"
                )
            }
        }

        private fun decodeTransferChecked(
            bytes: ByteArray,
            accounts: List<String>,
            accountIndices: List<Int>
        ): Verdict {
            return try {
                // Check data length: discriminator (1) + amount (8) + decimals (1) = 10 minimum
                if (bytes.size < 10) {
                    return Verdict(
                        title = "Malformed Instruction",
                        reason = "Data too short for TransferChecked",
                        isPayable = false,
                        summary = "Malformed instruction"
                    )
                }

                val amount = readU64LE(bytes, 1)
                val decimals = bytes[9].toInt() and 0xFF

                // Validate decimals
                if (decimals != 6) {
                    return Verdict(
                        title = "Invalid Decimals",
                        reason = "Expected 6 decimals, got $decimals",
                        isPayable = false,
                        summary = "Invalid decimals for SKR"
                    )
                }

                // Validate account structure: source, mint, destination, authority
                if (accountIndices.size < 4) {
                    return Verdict(
                        title = "Invalid Accounts",
                        reason = "TransferChecked requires 4 accounts",
                        isPayable = false,
                        summary = "Invalid account structure"
                    )
                }

                val sourceIdx = accountIndices[0]
                val mintIdx = accountIndices[1]
                val destIdx = accountIndices[2]
                val authorityIdx = accountIndices[3]

                if (sourceIdx >= accounts.size || mintIdx >= accounts.size ||
                    destIdx >= accounts.size || authorityIdx >= accounts.size
                ) {
                    return Verdict(
                        title = "Invalid Accounts",
                        reason = "Account indices out of bounds",
                        isPayable = false,
                        summary = "Invalid account indices"
                    )
                }

                val mint = accounts[mintIdx]
                if (mint != SKR_MINT) {
                    return Verdict(
                        title = "Wrong Mint",
                        reason = "Expected SKR mint $SKR_MINT, got ${shortKey(mint)}",
                        isPayable = false,
                        summary = "TransferChecked with non-SKR mint"
                    )
                }

                val source = accounts[sourceIdx]
                val destination = accounts[destIdx]

                Verdict(
                    title = "SKR Transfer",
                    reason = "Transfer $amount SKR (decimals: 6) to ${shortKey(destination)}",
                    isPayable = true,
                    summary = "Transfer ${amount / 1_000_000.0} SKR to ${shortKey(destination)}"
                )
            } catch (e: Exception) {
                Verdict(
                    title = "Malformed Instruction",
                    reason = "Failed to decode TransferChecked: ${e.message}",
                    isPayable = false,
                    summary = "Malformed instruction"
                )
            }
        }

        private fun decodeSolStakeInstruction(data: String): Verdict {
            return try {
                val bytes = android.util.Base64.decode(data, android.util.Base64.DEFAULT)
                if (bytes.size < 4) {
                    return Verdict(
                        title = "Malformed SOL Stake",
                        reason = "Data too short",
                        isPayable = false,
                        summary = "Malformed SOL stake instruction"
                    )
                }

                val discriminator = readU32LE(bytes, 0)
                val operationName = when (discriminator) {
                    0 -> "Initialize"
                    1 -> "Authorize"
                    2 -> "Delegate"
                    3 -> "Split"
                    4 -> "Withdraw"
                    5 -> "Deactivate"
                    6 -> "SetLockup"
                    7 -> "MergeStake"
                    else -> "Unknown ($discriminator)"
                }

                Verdict(
                    title = "SOL Stake: $operationName",
                    reason = "This is a SOL stake operation, not SKR",
                    isPayable = false,
                    summary = "SOL Stake $operationName - do not sign"
                )
            } catch (e: Exception) {
                Verdict(
                    title = "Malformed SOL Stake",
                    reason = "Failed to decode: ${e.message}",
                    isPayable = false,
                    summary = "Malformed instruction"
                )
            }
        }
    }
}

fun canSign(verdict: Verdict): Boolean = verdict.isPayable
