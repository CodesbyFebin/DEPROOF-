package com.deproof.crypto

import java.security.MessageDigest
import java.util.*

class MessageBindingException(message: String) : Exception(message)

object MessageBinding {

    fun createCanonicalMessage(
        transactionHash: String,
        verdict: String,
        metadata: String = ""
    ): String {
        return buildString {
            appendLine("Transaction Hash: $transactionHash")
            appendLine("Verdict: $verdict")
            if (metadata.isNotEmpty()) {
                appendLine("Metadata: $metadata")
            }
            appendLine("Timestamp: ${System.currentTimeMillis()}")
            append("Review completed at: ${Date()}")
        }
    }

    fun calculateMessageHash(message: String): String {
        val bytes = message.toByteArray(Charsets.UTF_8)
        val digest = MessageDigest.getInstance("SHA-256")
        val hashBytes = digest.digest(bytes)
        return hashBytes.joinToString("") { "%02x".format(it) }
    }

    fun assertMessageUnchanged(originalHash: String, currentMessage: String): Boolean {
        val currentHash = calculateMessageHash(currentMessage)
        if (currentHash != originalHash) {
            throw MessageBindingException("MESSAGE_CHANGED: Message has been tampered with. Original hash: $originalHash, Current hash: $currentHash")
        }
        return true
    }

    fun verifyMessageIntegrity(
        message: String,
        expectedHash: String
    ): Boolean {
        val computedHash = calculateMessageHash(message)
        return computedHash == expectedHash
    }

    fun createTamperDetectionAlert(originalHash: String, currentMessage: String): String {
        val currentHash = calculateMessageHash(currentMessage)
        return "ALERT: MESSAGE_CHANGED\n" +
                "Expected hash: $originalHash\n" +
                "Current hash: $currentHash\n" +
                "This transaction has been modified and should not be signed."
    }
}

object Base58 {
    private const val ALPHABET = "123456789ABCDEFGHJKLMNPQRSTUVWXYZabcdefghijkmnopqrstuvwxyz"
    private val BASE = ALPHABET.length

    fun encode(input: ByteArray): String {
        if (input.isEmpty()) return ""

        var num = input.fold(java.math.BigInteger.ZERO) { acc, byte ->
            acc.multiply(java.math.BigInteger.valueOf(256)).add(
                java.math.BigInteger.valueOf((byte.toInt() and 0xff).toLong())
            )
        }

        val encoded = StringBuilder()
        while (num > java.math.BigInteger.ZERO) {
            val remainder = (num.mod(java.math.BigInteger.valueOf(BASE.toLong()))).toInt()
            encoded.insert(0, ALPHABET[remainder])
            num = num.divide(java.math.BigInteger.valueOf(BASE.toLong()))
        }

        for (byte in input) {
            if (byte == 0.toByte()) {
                encoded.insert(0, ALPHABET[0])
            } else {
                break
            }
        }

        return encoded.toString()
    }

    fun decode(input: String): ByteArray {
        if (input.isEmpty()) return byteArrayOf()

        var num = java.math.BigInteger.ZERO
        for (char in input) {
            val digit = ALPHABET.indexOf(char)
            if (digit == -1) {
                throw IllegalArgumentException("Invalid character: $char")
            }
            num = num.multiply(java.math.BigInteger.valueOf(BASE.toLong())).add(
                java.math.BigInteger.valueOf(digit.toLong())
            )
        }

        val decoded = mutableListOf<Byte>()
        while (num > java.math.BigInteger.ZERO) {
            val remainder = num.mod(java.math.BigInteger.valueOf(256)).toInt()
            decoded.add(0, remainder.toByte())
            num = num.divide(java.math.BigInteger.valueOf(256))
        }

        for (char in input) {
            if (char == ALPHABET[0]) {
                decoded.add(0, 0)
            } else {
                break
            }
        }

        return decoded.toByteArray()
    }
}

// Binary parsing utilities
object BinaryParser {
    fun readU64LE(data: ByteArray, offset: Int): Long {
        var result = 0L
        for (i in 0 until 8) {
            result = result or ((data[offset + i].toLong() and 0xFF) shl (8 * i))
        }
        return result
    }

    fun readU32LE(data: ByteArray, offset: Int): Int {
        var result = 0
        for (i in 0 until 4) {
            result = result or ((data[offset + i].toInt() and 0xFF) shl (8 * i))
        }
        return result
    }

    fun readU8(data: ByteArray, offset: Int): Byte = data[offset]

    fun writeU64LE(value: Long): ByteArray {
        val result = ByteArray(8)
        for (i in 0 until 8) {
            result[i] = ((value shr (8 * i)) and 0xFF).toByte()
        }
        return result
    }

    fun readString(data: ByteArray, offset: Int, length: Int): String {
        return String(data, offset, length, Charsets.UTF_8)
    }
}

// Solana instruction discriminator
object InstructionDiscriminator {
    fun getDiscriminator(data: ByteArray): Byte {
        return if (data.isNotEmpty()) data[0] else 0
    }

    const val TRANSFER_CHECKED = 12.toByte()
    const val TRANSFER = 3.toByte()
    const val MINT_TO = 7.toByte()
}
