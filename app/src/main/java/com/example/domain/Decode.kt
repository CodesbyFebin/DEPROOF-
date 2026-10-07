package com.example.domain

import com.example.chain.Instruction
import com.example.chain.Verdict
import com.example.util.readU32LE
import com.example.util.readU64LE
import com.example.util.shortKey
import java.util.Base64

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
                    summary = "Unknown program. Do not sign."
                )
            }
        }

        private fun decodeTokenInstruction(
            accounts: List<String>,
            data: String,
            instruction: Instruction
        ): Verdict {
            return try {
                val bytes = Base64.getDecoder().decode(data)
                if (bytes.isEmpty()) {
                    return Verdict(
                        title = "Malformed Instruction",
                        reason = "Data is empty",
                        isPayable = false,
                        summary = "Malformed instruction data"
                    )
                }

                val discriminator = bytes[0].toInt() and 0xFF

                when (discriminator) {
                    DISCRIMINATOR_TRANSFER_CHECKED -> decodeTransferChecked(
                        bytes,
                        accounts,
                        instruction.accounts
                    )
                    DISCRIMINATOR_TRANSFER -> Verdict(
                        title = "Transfer",
                        reason = "Unchecked Transfer refused",
                        isPayable = false,
                        summary = "Unchecked Transfer. Do not sign."
                    )
                    DISCRIMINATOR_APPROVE -> Verdict(
                        title = "Approve",
                        reason = "Approve refused",
                        isPayable = false,
                        summary = "Approve. Do not sign."
                    )
                    DISCRIMINATOR_SET_AUTHORITY -> Verdict(
                        title = "SetAuthority",
                        reason = "SetAuthority refused",
                        isPayable = false,
                        summary = "SetAuthority changes wallet authority. Do not sign."
                    )
                    else -> Verdict(
                        title = "Unknown Token Instruction",
                        reason = "Unknown token discriminator: $discriminator",
                        isPayable = false,
                        summary = "Unknown token instruction $discriminator. Do not sign."
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

                if (decimals != 6) {
                    return Verdict(
                        title = "Invalid Decimals",
                        reason = "Expected 6 decimals, got $decimals",
                        isPayable = false,
                        summary = "TransferChecked decimals mismatch. Do not sign."
                    )
                }

                if (accountIndices.size < 4) {
                    return Verdict(
                        title = "Invalid Accounts",
                        reason = "TransferChecked requires 4 accounts",
                        isPayable = false,
                        summary = "Invalid account structure"
                    )
                }

                val mintIdx = accountIndices[1]
                if (mintIdx >= accounts.size) {
                    return Verdict(
                        title = "Invalid Accounts",
                        reason = "Mint index out of bounds",
                        isPayable = false,
                        summary = "Invalid account indices"
                    )
                }

                val mint = accounts[mintIdx]
                if (mint != SKR_MINT) {
                    return Verdict(
                        title = "Wrong Mint",
                        reason = "Expected SKR mint, got ${shortKey(mint)}",
                        isPayable = false,
                        summary = "TransferChecked with non-SKR mint. Do not sign."
                    )
                }

                val destIdx = accountIndices.getOrNull(2)
                val destination = if (destIdx != null && destIdx < accounts.size) {
                    accounts[destIdx]
                } else {
                    "unknown"
                }

                Verdict(
                    title = "SKR Transfer",
                    reason = "Transfer official SKR to ${shortKey(destination)}",
                    isPayable = true,
                    summary = "Transfer SKR to ${shortKey(destination)}"
                )
            } catch (e: Exception) {
                Verdict(
                    title = "Malformed Instruction",
                    reason = "Failed to decode: ${e.message}",
                    isPayable = false,
                    summary = "Malformed instruction"
                )
            }
        }

        private fun decodeSolStakeInstruction(data: String): Verdict {
            return try {
                val bytes = Base64.getDecoder().decode(data)
                if (bytes.size < 4) {
                    return Verdict(
                        title = "Malformed SOL Stake",
                        reason = "Data too short",
                        isPayable = false,
                        summary = "Malformed instruction"
                    )
                }

                val discriminator = readU32LE(bytes, 0)
                val operationName = when (discriminator) {
                    0 -> "initialize"
                    1 -> "authorize"
                    2 -> "delegate"
                    3 -> "split"
                    4 -> "withdraw"
                    5 -> "deactivate"
                    7 -> "merge"
                    else -> "unknown"
                }

                Verdict(
                    title = "SOL Stake: $operationName",
                    reason = "This is a native SOL stake operation, not SKR",
                    isPayable = false,
                    summary = "SOL stake $operationName. This is NOT SKR. Do not sign."
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
