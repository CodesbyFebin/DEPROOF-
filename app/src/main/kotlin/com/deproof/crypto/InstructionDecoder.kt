package com.deproof.crypto

import com.deproof.domain.model.Instruction
import com.deproof.domain.model.TransferCheckedInstruction
import com.deproof.domain.model.Verdict

object InstructionDecoder {

    fun decodeInstruction(instruction: Instruction): Verdict {
        val discriminator = InstructionDiscriminator.getDiscriminator(instruction.data)

        return when (discriminator) {
            InstructionDiscriminator.TRANSFER_CHECKED -> decodeTransferChecked(instruction)
            InstructionDiscriminator.TRANSFER -> Verdict.Payable
            InstructionDiscriminator.MINT_TO -> Verdict.DoNotSign
            else -> Verdict.Unknown("Unknown instruction discriminator: $discriminator")
        }
    }

    private fun decodeTransferChecked(instruction: Instruction): Verdict {
        return try {
            val decoded = parseTransferChecked(instruction)

            // Validate the TransferChecked instruction
            if (!isValidTransferChecked(decoded)) {
                return Verdict.DoNotSign
            }

            // Check if this is a legitimate token transfer
            if (isSuspiciousTransfer(decoded)) {
                return Verdict.DoNotSign
            }

            Verdict.Payable
        } catch (e: Exception) {
            Verdict.Unknown("Failed to decode TransferChecked: ${e.message}")
        }
    }

    private fun parseTransferChecked(instruction: Instruction): TransferCheckedInstruction {
        val data = instruction.data

        // Skip discriminator (1 byte)
        var offset = 1

        // Read amount (U64LE, 8 bytes)
        val amount = BinaryParser.readU64LE(data, offset)
        offset += 8

        // Read decimals (U8, 1 byte)
        val decimals = BinaryParser.readU8(data, offset).toByte()

        // Account keys from instruction.accounts
        val tokenProgramId = instruction.programId
        val source = if (instruction.accounts.size > 0) instruction.accounts[0] else ""
        val mint = if (instruction.accounts.size > 1) instruction.accounts[1] else ""
        val destination = if (instruction.accounts.size > 2) instruction.accounts[2] else ""
        val owner = if (instruction.accounts.size > 3) instruction.accounts[3] else ""

        return TransferCheckedInstruction(
            tokenProgramId = tokenProgramId,
            mint = mint,
            source = source,
            destination = destination,
            owner = owner,
            amount = amount,
            decimals = decimals
        )
    }

    private fun isValidTransferChecked(instruction: TransferCheckedInstruction): Boolean {
        // Validate required fields are present
        if (instruction.mint.isEmpty() ||
            instruction.source.isEmpty() ||
            instruction.destination.isEmpty()) {
            return false
        }

        // Validate amount is positive
        if (instruction.amount <= 0) {
            return false
        }

        // Validate decimals is reasonable (0-18 for Solana tokens)
        if (instruction.decimals < 0 || instruction.decimals > 18) {
            return false
        }

        return true
    }

    private fun isSuspiciousTransfer(instruction: TransferCheckedInstruction): Boolean {
        // Check for extremely large amounts (potential hack/rug pull)
        val maxAmount = Long.MAX_VALUE / 2
        if (instruction.amount > maxAmount) {
            return true
        }

        // Check for suspicious program IDs (would need a list of known malicious programs)
        // For now, just verify it looks like a token program
        if (!instruction.tokenProgramId.contains("TokenkegQfeZyiNwAJsyFbPVwwQkYk5LWV2BXVBq") &&
            !instruction.tokenProgramId.contains("TOKEN")) {
            // Not the standard token program, but not necessarily malicious
            return false
        }

        return false
    }

    fun validateTransferCheckedAccounts(instruction: Instruction): Boolean {
        // Must have at least 4 accounts: source, mint, destination, owner
        if (instruction.accounts.size < 4) {
            return false
        }

        // Verify accounts are valid Solana addresses (base58, proper length)
        for (account in instruction.accounts.take(4)) {
            if (!isValidSolanaAddress(account)) {
                return false
            }
        }

        return true
    }

    private fun isValidSolanaAddress(address: String): Boolean {
        // Solana addresses are base58 encoded 32-byte values
        // When encoded, they're typically 34-44 characters
        if (address.length < 34 || address.length > 44) {
            return false
        }

        // Try to decode - if it fails, invalid address
        return try {
            Base58.decode(address).size == 32
        } catch (e: Exception) {
            false
        }
    }

    fun extractAccountsFromTransaction(accountKeys: List<String>, instruction: Instruction): Map<String, String> {
        return mapOf(
            "source" to if (instruction.accounts.size > 0) instruction.accounts[0] else "Unknown",
            "mint" to if (instruction.accounts.size > 1) instruction.accounts[1] else "Unknown",
            "destination" to if (instruction.accounts.size > 2) instruction.accounts[2] else "Unknown",
            "owner" to if (instruction.accounts.size > 3) instruction.accounts[3] else "Unknown"
        )
    }
}
