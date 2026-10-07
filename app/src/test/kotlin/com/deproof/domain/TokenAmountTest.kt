// TokenAmountTest.kt — Tests for TokenAmount raw integer amount handling.
package com.deproof.domain

import org.junit.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

class TokenAmountTest {

    // ─── Construction ───────────────────────────────────────────────────────

    @Test
    fun `constructor accepts valid raw amount`() {
        val amount = TokenAmount("1000000", 6)
        assertEquals("1000000", amount.rawAmount)
        assertEquals(6, amount.decimals)
    }

    @Test(expected = IllegalArgumentException::class)
    fun `constructor rejects non-digit raw amount`() {
        TokenAmount("1.5", 6)
    }

    @Test(expected = IllegalArgumentException::class)
    fun `constructor rejects negative decimals`() {
        TokenAmount("1000000", -1)
    }

    // ─── toDecimalString ────────────────────────────────────────────────────

    @Test
    fun `toDecimalString returns "0" for zero with any decimals`() {
        assertEquals("0", TokenAmount("0", 0).toDecimalString())
        assertEquals("0", TokenAmount("0", 6).toDecimalString())
        assertEquals("0", TokenAmount("0", 9).toDecimalString())
    }

    @Test
    fun `toDecimalString with zero decimals returns raw amount`() {
        val amount = TokenAmount("1500", 0)
        assertEquals("1500", amount.toDecimalString())
    }

    @Test
    fun `toDecimalString formats correctly with 6 decimals`() {
        assertEquals("1", TokenAmount("1000000", 6).toDecimalString())
        assertEquals("0.000001", TokenAmount("1", 6).toDecimalString())
        assertEquals("1.5", TokenAmount("1500000", 6).toDecimalString())
        assertEquals("0.5", TokenAmount("500000", 6).toDecimalString())
    }

    @Test
    fun `toDecimalString formats correctly with 9 decimals`() {
        assertEquals("1", TokenAmount("1000000000", 9).toDecimalString())
        assertEquals("0.000000001", TokenAmount("1", 9).toDecimalString())
        assertEquals("1.5", TokenAmount("1500000000", 9).toDecimalString())
    }

    @Test
    fun `toDecimalString trims trailing zeros`() {
        assertEquals("1.5", TokenAmount("1500000", 6).toDecimalString())
        assertEquals("1", TokenAmount("1000000", 6).toDecimalString())
    }

    @Test
    fun `toDecimalString handles small amounts correctly`() {
        assertEquals("0.000001", TokenAmount("1", 6).toDecimalString())
        assertEquals("0.000010", TokenAmount("10", 6).toDecimalString())
        assertEquals("0.000100", TokenAmount("100", 6).toDecimalString())
    }

    // ─── toLong ─────────────────────────────────────────────────────────────

    @Test
    fun `toLong returns correct value`() {
        assertEquals(1000000L, TokenAmount("1000000", 6).toLong())
        assertEquals(1L, TokenAmount("1", 6).toLong())
        assertEquals(0L, TokenAmount("0", 6).toLong())
    }

    @Test(expected = NumberFormatException::class)
    fun `toLong throws for non-numeric string`() {
        TokenAmount("abc", 6).toLong()
    }

    // ─── toBigDecimal ───────────────────────────────────────────────────────

    @Test
    fun `toBigDecimal returns correct value`() {
        val amount = TokenAmount("1500000", 6)
        val bd = amount.toBigDecimal()
        assertEquals("1500000", bd.toPlainString())
    }

    @Test
    fun `toBigDecimal handles zero`() {
        val amount = TokenAmount("0", 6)
        assertEquals("0", amount.toBigDecimal().toPlainString())
    }

    // ─── parseFromDecimal with 6 decimals ────────────────────────────────────

    @Test
    fun `parseFromDecimal accepts "0" with 6 decimals`() {
        val amount = TokenAmount.parseFromDecimal("0", 6)
        assertEquals("0", amount?.rawAmount)
    }

    @Test
    fun `parseFromDecimal accepts "1" with 6 decimals`() {
        val amount = TokenAmount.parseFromDecimal("1", 6)
        assertEquals("1000000", amount?.rawAmount)
    }

    @Test
    fun `parseFromDecimal accepts "1.5" with 6 decimals`() {
        val amount = TokenAmount.parseFromDecimal("1.5", 6)
        assertEquals("1500000", amount?.rawAmount)
    }

    @Test
    fun `parseFromDecimal accepts "0.000001" with 6 decimals`() {
        val amount = TokenAmount.parseFromDecimal("0.000001", 6)
        assertEquals("1", amount?.rawAmount)
    }

    @Test
    fun `parseFromDecimal accepts "1.23456" with 6 decimals`() {
        val amount = TokenAmount.parseFromDecimal("1.23456", 6)
        assertEquals("1234560", amount?.rawAmount)
    }

    @Test
    fun `parseFromDecimal pads fractional part with zeros`() {
        val amount = TokenAmount.parseFromDecimal("1.5", 6)
        assertEquals("1500000", amount?.rawAmount)
    }

    // ─── parseFromDecimal with 9 decimals ────────────────────────────────────

    @Test
    fun `parseFromDecimal works with 9 decimals`() {
        val amount = TokenAmount.parseFromDecimal("1", 9)
        assertEquals("1000000000", amount?.rawAmount)
    }

    @Test
    fun `parseFromDecimal handles 9 decimals correctly`() {
        val amount = TokenAmount.parseFromDecimal("1.000000001", 9)
        assertEquals("1000000001", amount?.rawAmount)
    }

    // ─── parseFromDecimal error cases ───────────────────────────────────────

    @Test
    fun `parseFromDecimal returns null for empty string`() {
        assertNull(TokenAmount.parseFromDecimal("", 6))
    }

    @Test
    fun `parseFromDecimal returns null for whitespace`() {
        assertNull(TokenAmount.parseFromDecimal("   ", 6))
    }

    @Test
    fun `parseFromDecimal returns null for negative amount`() {
        assertNull(TokenAmount.parseFromDecimal("-1", 6))
    }

    @Test
    fun `parseFromDecimal returns null for too many decimals`() {
        assertNull(TokenAmount.parseFromDecimal("1.0000001", 6))
    }

    @Test
    fun `parseFromDecimal returns null for non-numeric characters`() {
        assertNull(TokenAmount.parseFromDecimal("1.5abc", 6))
        assertNull(TokenAmount.parseFromDecimal("abc", 6))
    }

    @Test
    fun `parseFromDecimal returns null for multiple decimal points`() {
        assertNull(TokenAmount.parseFromDecimal("1.2.3", 6))
    }

    @Test
    fun `parseFromDecimal trims whitespace`() {
        val amount = TokenAmount.parseFromDecimal("  1.5  ", 6)
        assertEquals("1500000", amount?.rawAmount)
    }

    // ─── fromRaw ────────────────────────────────────────────────────────────

    @Test
    fun `fromRaw creates amount from raw value`() {
        val amount = TokenAmount.fromRaw(1_500_000L, 6)
        assertEquals("1500000", amount.rawAmount)
        assertEquals("1.5", amount.toDecimalString())
    }

    @Test
    fun `fromRaw handles zero`() {
        val amount = TokenAmount.fromRaw(0L, 6)
        assertEquals("0", amount.rawAmount)
    }

    @Test(expected = IllegalArgumentException::class)
    fun `fromRaw rejects negative value`() {
        TokenAmount.fromRaw(-1L, 6)
    }

    // ─── Decimal string conversion accuracy ──────────────────────────────────

    @Test
    fun `parseFromDecimal and toDecimalString are inverse operations`() {
        val original = "1.234567"
        val maxDecimals = 9

        // Note: conversion may lose precision beyond 9 decimals
        val amount = TokenAmount.parseFromDecimal("1.234567", maxDecimals)
        assertEquals("1.234567", amount?.toDecimalString())
    }

    @Test
    fun `round-trip conversion preserves value`() {
        val original = "42.123456"
        val decimals = 6

        val amount = TokenAmount.parseFromDecimal(original, decimals)
        val roundTrip = amount?.toDecimalString()
        assertEquals(original, roundTrip)
    }
}
