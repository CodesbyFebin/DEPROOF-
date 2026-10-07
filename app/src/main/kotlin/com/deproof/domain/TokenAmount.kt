// TokenAmount.kt — Type-safe token amount handling with raw integer representation.
//
// This module ensures amounts are always stored and transmitted as raw integer strings
// to avoid floating-point rounding errors. Decimal places are handled via decimals parameter.
package com.deproof.domain

import java.math.BigDecimal
import kotlin.math.pow

/**
 * Represents a token amount as a raw integer string (e.g., "1000000000" for 1 SOL with 9 decimals).
 * Provides methods to convert to/from human-readable decimal strings.
 */
data class TokenAmount(
    /** Raw amount as a string (no decimal point). */
    val rawAmount: String,
    /** Number of decimal places for this token (e.g., 9 for SOL, 6 for SKR). */
    val decimals: Int
) {
    init {
        require(rawAmount.all { it.isDigit() }) { "rawAmount must contain only digits" }
        require(decimals >= 0) { "decimals must be non-negative" }
    }

    /**
     * Converts raw amount to human-readable decimal string.
     * Example: TokenAmount("1000000000", 9) → "1.000000000"
     */
    fun toDecimalString(): String {
        if (decimals == 0) return rawAmount

        val paddedRaw = rawAmount.padStart(decimals + 1, '0')
        val intPart = paddedRaw.substring(0, paddedRaw.length - decimals)
        val fracPart = paddedRaw.substring(paddedRaw.length - decimals)

        val trimmedFrac = fracPart.trimEnd('0')
        return if (trimmedFrac.isEmpty()) intPart else "$intPart.$trimmedFrac"
    }

    /**
     * Returns the raw amount as a Long. Throws if value exceeds Long.MAX_VALUE.
     */
    fun toLong(): Long = rawAmount.toLong()

    /**
     * Returns the raw amount as a BigDecimal for precise arithmetic.
     */
    fun toBigDecimal(): BigDecimal = BigDecimal(rawAmount)

    companion object {
        /**
         * Parses a decimal string (e.g., "1.5" or "1000000000") to a TokenAmount.
         * The raw amount is stored as an integer string without decimal point.
         *
         * Returns null if parsing fails or value is invalid.
         * Examples:
         * - parseFromDecimal("1.5", 6) → TokenAmount("1500000", 6)
         * - parseFromDecimal("1", 9) → TokenAmount("1000000000", 9)
         * - parseFromDecimal("0.000001", 6) → TokenAmount("1", 6)
         */
        fun parseFromDecimal(decimalString: String, decimals: Int): TokenAmount? {
            val trimmed = decimalString.trim()
            if (trimmed.isEmpty()) return null

            try {
                val dotIdx = trimmed.indexOf('.')
                val intPart: String
                val fracPart: String

                if (dotIdx < 0) {
                    intPart = trimmed
                    fracPart = ""
                } else {
                    intPart = trimmed.substring(0, dotIdx)
                    fracPart = trimmed.substring(dotIdx + 1)
                }

                if (intPart.isEmpty() || intPart == "-") return null
                if (intPart.any { !it.isDigit() }) return null
                if (fracPart.any { !it.isDigit() }) return null
                if (fracPart.length > decimals) return null

                val paddedFrac = fracPart.padEnd(decimals, '0')
                val raw = intPart + paddedFrac

                // Validate it's a valid number
                raw.toLong()

                return TokenAmount(raw, decimals)
            } catch (_: Exception) {
                return null
            }
        }

        /**
         * Creates a TokenAmount from a raw integer value.
         * Example: TokenAmount.fromRaw(1_000_000_000, 9) → "1.000000000"
         */
        fun fromRaw(rawValue: Long, decimals: Int): TokenAmount {
            require(rawValue >= 0) { "rawValue must be non-negative" }
            return TokenAmount(rawValue.toString(), decimals)
        }
    }
}
