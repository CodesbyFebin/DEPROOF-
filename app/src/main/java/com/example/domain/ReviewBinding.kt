package com.example.domain

import com.example.util.sha256Text

data class ReviewHash(
    val messageHash: String,
    val canonicalMessage: String,
    val timestamp: Long = System.currentTimeMillis()
)

class ReviewBinding {
    companion object {
        fun canonicalMessage(
            programId: String,
            keys: List<String>,
            data: String,
            network: String = "mainnet-beta",
            feePayer: String = ""
        ): String {
            val lines = mutableListOf<String>()
            lines.add(network)
            if (feePayer.isNotEmpty()) lines.add(feePayer)
            lines.add("program=$programId")
            lines.add("keys=${keys.joinToString(",")}")
            lines.add("data=$data")
            return lines.joinToString("\n")
        }

        fun hashMessage(message: String): String = sha256Text(message)

        fun assertUnchanged(currentHash: String, storedHash: String) {
            if (currentHash != storedHash) {
                throw IllegalStateException("MESSAGE_CHANGED")
            }
        }

        fun prepareReview(
            programId: String,
            keys: List<String>,
            data: String,
            network: String = "mainnet-beta"
        ): ReviewHash {
            val message = canonicalMessage(programId, keys, data, network)
            val hash = hashMessage(message)
            return ReviewHash(
                messageHash = hash,
                canonicalMessage = message
            )
        }
    }
}
