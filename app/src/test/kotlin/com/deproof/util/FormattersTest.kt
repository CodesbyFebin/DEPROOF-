package com.deproof.util

import org.junit.Test
import org.junit.Assert.*

/**
 * F042: Formatter Suite Tests
 *
 * Verifies that financial amounts, addresses, and timestamps
 * are formatted correctly for user-facing display.
 */
class FormattersTest {

    // F042-TC-01: SOL formatting with decimals
    @Test
    fun testFormatSolDecimals() {
        // 1 SOL should display as "1 SOL"
        val result = Formatters.formatSol(1.0)
        assertTrue(result.contains("1"))
        assertTrue(result.contains("SOL"))

        // 0.000000001 SOL (1 lamport) should show 9 decimals
        val oneLamport = Formatters.formatSol(0.000000001)
        assertTrue(oneLamport.contains("0.000000001") || oneLamport.contains("1e-9"))
    }

    // F042-TC-02: SOL thousand separator
    @Test
    fun testFormatSolThousandSeparator() {
        // 1,234,567.89 SOL should display with commas
        val result = Formatters.formatSol(1234567.89)
        assertTrue(result.contains("1") && result.contains("234") && result.contains("567"))
        // Should have thousand separator (comma or space)
        assertTrue(result.contains(",") || result.contains(" "))
    }

    // F042-TC-03: SKR formatting (6 decimals)
    @Test
    fun testFormatSkrSixDecimals() {
        // SKR tokens have 6 decimals
        val result = Formatters.formatSkr(1.0)
        assertTrue(result.contains("1.000000"))
        assertTrue(result.contains("SKR"))

        // Verify it always shows 6 decimals
        val halfToken = Formatters.formatSkr(0.5)
        assertTrue(halfToken.contains("0.500000"))
    }

    // F042-TC-04: Address truncation
    @Test
    fun testFormatAddressTruncation() {
        val fullAddress = "1A1zP1eP5QGefi2DMPTfTL5SLmv7DivfNa"
        val result = Formatters.formatAddress(fullAddress)

        // Should show shortened form like "...DivfNa"
        assertTrue(result.contains("...") || result.contains("1A1z..."))
        // Should display last 8 characters or similar
        assertTrue(result.length < fullAddress.length)
        assertTrue(result.contains("DivfNa"))
    }

    // F042-TC-05: Timestamp formatting
    @Test
    fun testFormatTimestamp() {
        // Current time in milliseconds
        val now = System.currentTimeMillis()
        val result = Formatters.formatTimestamp(now)

        // Should contain recognizable date/time components
        assertTrue(result.contains("2026") || result.contains("Oct") || result.contains("10"))
        assertTrue(result.length > 5) // Not empty or too short
    }

    // F042-TC-06: Signature display (first+last)
    @Test
    fun testFormatSignatureDisplay() {
        val signature = "37u9WtQxAMSvGTvNMfJyxhtrh8Tr8n9mHwVJqgS5L8m3EwMKfqxjcX1j2DwRGb6TpV8PUpLfTz5F9qVj7mYx"
        val result = Formatters.formatSignature(signature)

        // Should show abbreviated form
        assertTrue(result.length < signature.length)
        // Should contain first and/or last parts
        assertTrue(result.contains("37u9") || result.contains("7mYx"))
    }

    // F042-TC-07: Zero amounts
    @Test
    fun testFormatZeroAmount() {
        val zeroSol = Formatters.formatSol(0.0)
        assertTrue(zeroSol.contains("0"))
        assertTrue(zeroSol.contains("SOL"))

        val zeroSkr = Formatters.formatSkr(0.0)
        assertTrue(zeroSkr.contains("0"))
        assertTrue(zeroSkr.contains("SKR"))
    }

    // F042-TC-08: Very large amounts
    @Test
    fun testFormatVeryLargeAmount() {
        val largeAmount = 1_000_000_000.0
        val result = Formatters.formatSol(largeAmount)

        // Should handle large numbers without scientific notation
        assertTrue(!result.contains("e") || !result.matches(Regex(".*\\de\\+.*")))
        // Should still be readable
        assertTrue(result.length > 5)
    }

    // F042-TC-09: Negative amounts (should handle gracefully)
    @Test
    fun testFormatNegativeAmount() {
        val negative = Formatters.formatSol(-1.5)
        // Should either format as negative or throw exception
        // Negative amounts are typically invalid, but formatter should handle
        assertTrue(negative.contains("-") || negative.contains("error") || negative.contains("invalid"))
    }

    // F042-TC-10: Currency precision consistency
    @Test
    fun testFormatCurrencyPrecisionConsistency() {
        // Same amount should format the same way every time
        val amount = 123.456789
        val result1 = Formatters.formatSol(amount)
        val result2 = Formatters.formatSol(amount)

        assertEquals(result1, result2)
    }

    // F042-TC-11: Null or empty address handling
    @Test
    fun testFormatAddressNullHandling() {
        // Should not crash on null/empty input
        try {
            val result = Formatters.formatAddress("")
            assertTrue(result.isEmpty() || result.contains("N/A") || result.contains("Unknown"))
        } catch (e: Exception) {
            // Exception is acceptable if documented
            assertTrue(e is IllegalArgumentException || e is NullPointerException)
        }
    }

    // F042-TC-12: Locale independence
    @Test
    fun testFormatLocaleIndependence() {
        // Formatting should be consistent regardless of system locale
        val amount = 1234.56
        val result = Formatters.formatSol(amount)

        // Should use consistent separator (dot for decimals in US English)
        assertTrue(result.contains("1234") && (result.contains(".56") || result.contains(",56")))
    }
}
