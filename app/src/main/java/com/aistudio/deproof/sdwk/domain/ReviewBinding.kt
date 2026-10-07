package com.aistudio.deproof.sdwk.domain

import com.aistudio.deproof.sdwk.chain.Instruction
import com.aistudio.deproof.sdwk.chain.TransactionResponse
import com.aistudio.deproof.sdwk.util.sha256Text

data class ReviewHash(
    val messageHash: String,
    val canonicalMessage: String,
    val timestamp: Long = System.currentTimeMillis()
)

class ReviewBinding {
    companion object {
        fun canonicalMessage(
            tx: TransactionResponse,
            cluster: String = "mainnet-beta",
            feePayer: String = ""
        ): String {
            val lines = mutableListOf<String>()

            lines.add(cluster)
            if (feePayer.isNotEmpty()) lines.add(feePayer)

            tx.transaction.message.instructions.forEach { instr ->
                lines.add("Instruction:")
                lines.add("  program=${tx.transaction.message.accountKeys.getOrNull(instr.programIdIndex) ?: "unknown"}")
                lines.add("  accounts=[${instr.accounts.joinToString(",")}]")
                lines.add("  data=${instr.data}")
            }

            return lines.joinToString("\n")
        }

        fun hashMessage(message: String): String = sha256Text(message)

        fun assertUnchanged(currentHash: String, storedHash: String) {
            if (currentHash != storedHash) {
                throw IllegalStateException("MESSAGE_CHANGED: Hash mismatch")
            }
        }

        fun prepareReview(tx: TransactionResponse, cluster: String = "mainnet-beta"): ReviewHash {
            val message = canonicalMessage(tx, cluster)
            val hash = hashMessage(message)
            return ReviewHash(
                messageHash = hash,
                canonicalMessage = message
            )
        }
    }
}
