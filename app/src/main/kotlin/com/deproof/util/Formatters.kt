package com.deproof.util

object Formatters {

    fun formatSol(lamports: Long): String {
        val sol = lamports.toDouble() / 1_000_000_000.0
        return String.format("%.9f SOL", sol)
    }

    fun formatSkr(amount: Long, decimals: Int = 4): String {
        val display = amount.toDouble() / Math.pow(10.0, decimals.toDouble())
        return when {
            amount >= 1_000_000 -> String.format("%.${decimals}f", display / 1_000_000)
            amount >= 1_000 -> String.format("%.${decimals}f", display / 1_000)
            else -> String.format("%.${decimals}f", display)
        } + " SKR"
    }

    fun formatAddress(address: String, startChars: Int = 8, endChars: Int = 8): String {
        return if (address.length > startChars + endChars + 2) {
            address.take(startChars) + "..." + address.takeLast(endChars)
        } else {
            address
        }
    }

    fun formatHash(hash: String, startChars: Int = 8, endChars: Int = 8): String {
        return if (hash.length > startChars + endChars + 2) {
            hash.take(startChars) + "..." + hash.takeLast(endChars)
        } else {
            hash
        }
    }

    fun formatTimestamp(timestamp: Long): String {
        val date = java.util.Date(timestamp)
        val format = java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss", java.util.Locale.US)
        return format.format(date)
    }

    fun formatSlot(slot: Long): String = String.format("%,d", slot)

    fun formatAmount(amount: Long, decimals: Int = 9): String {
        val value = amount.toDouble() / Math.pow(10.0, decimals.toDouble())
        return when {
            value >= 1_000_000 -> String.format("%.2f M", value / 1_000_000)
            value >= 1_000 -> String.format("%.2f K", value / 1_000)
            else -> String.format("%.${decimals}f", value)
        }
    }

    fun formatFileSize(bytes: Long): String {
        return when {
            bytes >= 1_000_000_000 -> String.format("%.2f GB", bytes / 1_000_000_000.0)
            bytes >= 1_000_000 -> String.format("%.2f MB", bytes / 1_000_000.0)
            bytes >= 1_000 -> String.format("%.2f KB", bytes / 1_000.0)
            else -> "$bytes B"
        }
    }

    fun formatPercent(value: Double): String = String.format("%.2f%%", value * 100)
}

object Validators {

    fun isPubkey(address: String): Boolean {
        // Solana addresses are base58 encoded 32-byte values
        if (address.length < 34 || address.length > 44) return false

        return try {
            val decoded = com.deproof.crypto.Base58.decode(address)
            decoded.size == 32
        } catch (e: Exception) {
            false
        }
    }

    fun isValidMint(mint: String): Boolean {
        // Mint is also a pubkey
        return isPubkey(mint)
    }

    fun isValidSignature(signature: String): Boolean {
        // Solana signatures are base58 encoded 64-byte values
        if (signature.length < 86 || signature.length > 88) return false

        return try {
            val decoded = com.deproof.crypto.Base58.decode(signature)
            decoded.size == 64
        } catch (e: Exception) {
            false
        }
    }

    fun isValidAmount(amount: Long): Boolean {
        return amount >= 0
    }

    fun isValidDecimals(decimals: Int): Boolean {
        return decimals in 0..18
    }

    fun isValidUrl(url: String): Boolean {
        return try {
            java.net.URL(url)
            true
        } catch (e: Exception) {
            false
        }
    }

    fun isValidEmail(email: String): Boolean {
        return email.matches(Regex("^[A-Za-z0-9+_.-]+@(.+)$"))
    }
}

object TextUtils {

    fun camelToSnake(str: String): String {
        return str.replace(Regex("([a-z])([A-Z])"), "$1_$2").lowercase()
    }

    fun snakeToCamel(str: String): String {
        var result = ""
        var capitalizeNext = false
        for (char in str) {
            when {
                char == '_' -> capitalizeNext = true
                capitalizeNext -> {
                    result += char.uppercaseChar()
                    capitalizeNext = false
                }
                else -> result += char
            }
        }
        return result
    }

    fun capitalizeWords(str: String): String {
        return str.split(" ").joinToString(" ") { word ->
            if (word.isNotEmpty()) word[0].uppercaseChar() + word.substring(1).lowercase()
            else word
        }
    }

    fun truncate(str: String, maxLength: Int, suffix: String = "..."): String {
        return if (str.length > maxLength) {
            str.take(maxLength - suffix.length) + suffix
        } else {
            str
        }
    }
}
