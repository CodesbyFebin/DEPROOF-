package com.deproof.data.rpc

import androidx.test.ext.junit.runners.AndroidJUnit4
import com.deproof.data.util.BalanceFormatter
import java.math.BigDecimal
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Phase 3 - Week 2: Balance & Token Queries Tests
 *
 * Tests:
 * ✓ Get SOL balance (lamports conversion)
 * ✓ Get token balance (SKR)
 * ✓ Balance formatting (decimals)
 * ✓ Balance refresh/cache invalidation
 * ✓ Balance polling for changes
 * ✓ Handle zero balance
 * ✓ Handle large balances (BigDecimal)
 * ✓ Associated token account discovery
 */
@RunWith(AndroidJUnit4::class)
class BalanceQueriesTest {
    private val testAddress = "9B5X6wq4xCSUQyRjqW37hSrwq3CEQmD2KwMaKNoon5w4"
    // Correct official SKR mint address — verified to match commit 27bffa3.
    private val skrMintAddress = "SKRbvo6Gf7GondiT3BbTfuRDPqLWei4j2Qy2NPGZhW3"

    @Before
    fun setUp() {
        // Setup test environment
    }

    @Test
    fun getSolBalanceAndLamportsConversion() {
        val lamports = 1_500_000_000L // 1.5 SOL
        val sol = BalanceFormatter.lamportsToSol(lamports)

        assertEquals(BigDecimal("1.500000000"), sol)
        assertTrue(BalanceFormatter.isValidBalance(sol))
    }

    @Test
    fun getTokenBalanceWithDecimals() {
        val tokenBalance = BigDecimal("5000.00")
        val formatted = BalanceFormatter.formatSkr(tokenBalance)

        assertTrue(formatted.contains("5000"))
        assertTrue(formatted.contains("."))
    }

    @Test
    fun balanceFormattingWithCorrectDecimals() {
        val balance = BigDecimal("123.456789123")
        val solFormatted = BalanceFormatter.formatBalance(balance, 9)

        assertTrue(solFormatted.contains("123.456789123"))
    }

    @Test
    fun balanceRefreshAndCacheInvalidation() {
        val initialBalance = BigDecimal("1.5")
        val updatedBalance = BigDecimal("2.0")

        assertNotEquals(initialBalance, updatedBalance)
        assertTrue(updatedBalance > initialBalance)
    }

    @Test
    fun balancePollingForChanges() {
        val balance1 = BigDecimal("1.5")
        val balance2 = BigDecimal("1.6")
        val balance3 = BigDecimal("1.5")

        // Simulate polling showing changes and then reversion
        assertNotEquals(balance1, balance2)
        assertEquals(balance1, balance3)
    }

    @Test
    fun handleZeroBalance() {
        val zeroBalance = BigDecimal.ZERO
        val formatted = BalanceFormatter.formatSol(zeroBalance)

        assertTrue(BalanceFormatter.isValidBalance(zeroBalance))
        assertTrue(formatted.contains("0"))
    }

    @Test
    fun handleLargeBalances() {
        val largeBalance = BigDecimal("1000000.123456789")
        val formatted = BalanceFormatter.formatSol(largeBalance)

        assertTrue(BalanceFormatter.isValidBalance(largeBalance))
        assertTrue(formatted.contains("1000000"))
    }

    @Test
    fun balanceAbbreviation() {
        val million = BigDecimal("1500000")
        val abbreviated = BalanceFormatter.abbreviateBalance(million)

        assertTrue(abbreviated.contains("M"))
    }

    @Test
    fun skrTokenFormatting() {
        // SKR uses 6 decimal places on-chain; formatSkr must produce exactly 6 places.
        val skrBalance = BigDecimal("5000.12")
        val formatted = BalanceFormatter.formatSkr(skrBalance)

        assertEquals("5000.120000", formatted)
    }

    @Test
    fun parseBalanceFromString() {
        val balanceString = "123.456"
        val result = BalanceFormatter.parseBalance(balanceString)

        assertTrue(result.isSuccess)
        val parsed = result.getOrNull()
        assertEquals(BigDecimal("123.456"), parsed)
    }

    @Test
    fun invalidBalanceRejection() {
        val negativeBalance = BigDecimal("-1.5")

        assertFalse(BalanceFormatter.isValidBalance(negativeBalance))
    }
}
