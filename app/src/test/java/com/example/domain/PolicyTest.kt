package com.example.domain

import org.junit.Test
import org.junit.Assert.*

// C095 (one memo rebuild, no reused approval) and FN062 key-level policy.
// Keystore signing itself is device-only and is not covered here.
class PolicyTest {
    private val account = Base58.encode(ByteArray(32) { 7 })
    private val oldBlockhash = Base58.encode(ByteArray(32) { 9 })
    private val newBlockhash = Base58.encode(ByteArray(32) { 10 })
    private val genesis = "EtWTRABZaYq6iMfeYKouRu166VU2xqa1wcaWoxPkrZBG"
    private fun context(reviewedAt: String) = ReviewContext(account, "devnet", genesis, Policy.DEVNET_MEMO_V1,
        reviewedAt = reviewedAt, lastValidBlockHeight = "123", feeLamports = "5001", feeSlot = "120")
    private val gate = SigningGate(account, "devnet", true, true, true, true, false)
    private fun fails(code: String, block: () -> Unit) { try { block(); fail("Expected $code") } catch (e: Failure) { assertEquals(code, e.code) } }

    @Test fun firstRebuildIsAllowedAndASecondIsNot() {
        assertEquals(1, MAX_MEMO_REBUILDS)
        assertTrue(memoRebuildAllowed(0))
        assertFalse(memoRebuildAllowed(1))
        assertFalse(memoRebuildAllowed(2))
    }

    @Test fun rebuiltDraftDoesNotInheritThePriorApproval() {
        val oldBytes = buildMemo(account, oldBlockhash, 1000)
        val oldReview = Review(oldBytes, context("2026-10-06T00:00:00Z"))
        assertTrue(canSign(oldReview, oldBytes, context("2026-10-06T00:00:00Z"), gate))

        // Blockhash expired: the rebuilt draft is a new message with a new Review.
        val rebuiltBytes = buildMemo(account, newBlockhash, 2000)
        val rebuiltReview = Review(rebuiltBytes, context("2026-10-06T00:05:00Z"))
        assertFalse(canSign(oldReview, rebuiltBytes, context("2026-10-06T00:05:00Z"), gate))
        assertNotEquals(oldReview.messageSha256, rebuiltReview.messageSha256)
        // The rebuilt draft is only signable after its own review.
        assertTrue(canSign(rebuiltReview, rebuiltBytes, rebuiltReview.context, gate))
    }

    @Test fun onlyHardwareBackedKeyLevelsQualify() {
        requireQualifiedKeyLevel("STRONGBOX")
        requireQualifiedKeyLevel("TRUSTED_ENVIRONMENT")
        fails("SOFTWARE_KEY") { requireQualifiedKeyLevel("SOFTWARE") }
        fails("UNQUALIFIED_KEY") { requireQualifiedKeyLevel("HARDWARE_UNSPECIFIED") }
        fails("UNQUALIFIED_KEY") { requireQualifiedKeyLevel("UNAVAILABLE") }
    }
}
