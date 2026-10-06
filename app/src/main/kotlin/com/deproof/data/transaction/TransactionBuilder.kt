package com.deproof.data.transaction

import com.deproof.domain.model.Transaction
import com.deproof.domain.model.TransactionInstruction
import java.util.Base64

class TransactionBuilder {
    private val instructions = mutableListOf<TransactionInstruction>()
    private var feePayer: String? = null
    private var recentBlockhash: String? = null

    fun setFeePayer(payer: String) = apply {
        feePayer = payer
    }

    fun setRecentBlockhash(hash: String) = apply {
        recentBlockhash = hash
    }

    fun addInstruction(instruction: TransactionInstruction) = apply {
        instructions.add(instruction)
    }

    fun addSolTransfer(
        from: String,
        to: String,
        lamports: Long
    ) = apply {
        val instruction = TransactionInstruction(
            programId = "11111111111111111111111111111111", // System Program
            accounts = listOf(from, to),
            data = "transfer:$lamports"
        )
        instructions.add(instruction)
    }

    fun addTokenTransfer(
        from: String,
        to: String,
        mint: String,
        amount: Long
    ) = apply {
        val instruction = TransactionInstruction(
            programId = "TokenkegQfeZyiNwAJsyFbPVwwQkYk5LWV2BXVBq", // Token Program
            accounts = listOf(from, to, mint),
            data = "transfer:$amount"
        )
        instructions.add(instruction)
    }

    fun decodeInstruction(base64Data: String): Result<TransactionInstruction> {
        return try {
            val decoded = Base64.getDecoder().decode(base64Data)
            val data = String(decoded)
            val parts = data.split(":")

            Result.success(
                TransactionInstruction(
                    programId = parts.getOrNull(0) ?: "",
                    accounts = parts.getOrNull(1)?.split(",") ?: emptyList(),
                    data = parts.getOrNull(2) ?: ""
                )
            )
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    fun build(): Result<Transaction> {
        return try {
            if (instructions.isEmpty()) {
                return Result.failure(IllegalArgumentException("Transaction must have at least one instruction"))
            }
            if (feePayer == null) {
                return Result.failure(IllegalArgumentException("Fee payer must be set"))
            }

            // Build the Instruction objects from TransactionInstructions
            val builtInstructions = instructions.map { txInstr ->
                com.deproof.domain.model.Instruction(
                    programId = txInstr.programId,
                    discriminator = 0,
                    accounts = txInstr.accounts,
                    data = txInstr.data.toByteArray()
                )
            }

            Result.success(
                Transaction(
                    signature = "", // Will be populated when signed
                    blockTime = 0L,
                    slot = 0L,
                    status = com.deproof.domain.model.TransactionStatus.PENDING,
                    instructions = builtInstructions,
                    accountKeys = (listOf(feePayer!!) + instructions.flatMap { it.accounts }).distinct()
                )
            )
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}

object SystemProgram {
    fun transfer(
        fromPubkey: String,
        toPubkey: String,
        lamports: Long
    ): TransactionInstruction {
        return TransactionInstruction(
            programId = "11111111111111111111111111111111",
            accounts = listOf(fromPubkey, toPubkey),
            data = "transfer:$lamports"
        )
    }
}

object TokenProgram {
    fun transfer(
        sourceToken: String,
        destinationToken: String,
        owner: String,
        amount: Long
    ): TransactionInstruction {
        return TransactionInstruction(
            programId = "TokenkegQfeZyiNwAJsyFbPVwwQkYk5LWV2BXVBq",
            accounts = listOf(sourceToken, destinationToken, owner),
            data = "transfer:$amount"
        )
    }
}
