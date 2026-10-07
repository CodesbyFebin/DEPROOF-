package com.example.util

import java.math.BigDecimal
import java.math.BigInteger
import java.text.NumberFormat
import java.util.Locale

fun formatSol(lamports: Long): String {
    if (lamports == 0L) return "0.000000000 SOL"
    val bd = BigDecimal(lamports)
    val sol = bd.movePointLeft(9).toPlainString()
    val parts = sol.split(".")
    val intPart = parts[0]
    val decPart = if (parts.size > 1) parts[1].padEnd(9, '0').substring(0, 9) else "000000000"
    return "$intPart.$decPart SOL"
}

fun formatSkr(raw: Long): String {
    if (raw == 0L) return "0.000000 SKR"
    val bd = BigDecimal(raw)
    val skr = bd.movePointLeft(6).toPlainString()
    val parts = skr.split(".")
    val intPart = parts[0].toLong()
    val decPart = if (parts.size > 1) parts[1].padEnd(6, '0').substring(0, 6) else "000000"

    val nf = NumberFormat.getInstance(Locale.US)
    nf.isGroupingUsed = true
    nf.maximumFractionDigits = 0
    val formatted = nf.format(intPart)

    return "$formatted.$decPart SKR"
}

fun isPubkey(input: String): Boolean {
    if (input.length != 44) return false
    return try {
        Base58.decode(input).size == 32
    } catch (e: Exception) {
        false
    }
}

object Base58 {
    private const val ALPHABET = "123456789ABCDEFGHJKLMNPQRSTUVWXYZabcdefghijkmnopqrstuvwxyz"

    fun decode(encoded: String): ByteArray {
        var num = BigInteger.ZERO
        val base = BigInteger.valueOf(58)

        for (c in encoded) {
            val index = ALPHABET.indexOf(c)
            if (index == -1) throw IllegalArgumentException("Invalid Base58 character: $c")
            num = num.multiply(base).add(BigInteger.valueOf(index.toLong()))
        }

        val bytes58 = num.toByteArray()
        val leadingZeros = encoded.takeWhile { it == '1' }.length

        val result = ByteArray(leadingZeros + bytes58.size)
        System.arraycopy(bytes58, 0, result, leadingZeros, bytes58.size)
        return result
    }

    fun encode(data: ByteArray): String {
        var num = BigInteger(1, data)
        val base = BigInteger.valueOf(58)
        val sb = StringBuilder()

        while (num > BigInteger.ZERO) {
            val remainder = num.mod(base).toInt()
            sb.insert(0, ALPHABET[remainder])
            num = num.divide(base)
        }

        for (byte in data) {
            if (byte == 0.toByte()) {
                sb.insert(0, '1')
            } else {
                break
            }
        }

        return sb.toString()
    }
}

fun shortKey(key: String): String {
    return if (key.length > 10) {
        key.take(4) + "..." + key.takeLast(4)
    } else {
        key
    }
}

fun readU64LE(bytes: ByteArray, offset: Int): Long {
    if (offset + 8 > bytes.size) throw IndexOutOfBoundsException("SHORT_BUFFER")
    var value = 0L
    for (i in 0..7) {
        value = value or ((bytes[offset + i].toLong() and 0xFF) shl (i * 8))
    }
    return value
}

fun readU32LE(bytes: ByteArray, offset: Int): Int {
    if (offset + 4 > bytes.size) throw IndexOutOfBoundsException("SHORT_BUFFER")
    var value = 0
    for (i in 0..3) {
        value = value or ((bytes[offset + i].toInt() and 0xFF) shl (i * 8))
    }
    return value
}

fun sha256Text(text: String): String {
    val bytes = text.toByteArray(Charsets.UTF_8)
    val md = java.security.MessageDigest.getInstance("SHA-256")
    val digest = md.digest(bytes)
    return digest.joinToString("") { "%02x".format(it) }
}
