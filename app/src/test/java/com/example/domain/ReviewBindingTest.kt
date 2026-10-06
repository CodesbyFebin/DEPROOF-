package com.example.domain

import org.junit.Test
import org.junit.Assert.*

// Review-binding gaps not covered by CoreTest: timestamp, fee and block-height
// context fields, wallet account on the signing gate, and stored-hash
// stability after the caller's buffer changes.
class ReviewBindingTest {
    private val account = Base58.encode(ByteArray(32) { 7 })
    private val blockhash = Base58.encode(ByteArray(32) { 9 })
    private val genesis = "EtWTRABZaYq6iMfeYKouRu166VU2xqa1wcaWoxPkrZBG"
    private fun context() = ReviewContext(account, "devnet", genesis, Policy.DEVNET_MEMO_V1,
        reviewedAt = "2026-10-06T00:00:00Z", lastValidBlockHeight = "123", feeLamports = "5001", feeSlot = "120")
    private val gate = SigningGate(account, "devnet", true, true, true, true, false)
    private fun fails(code: String, block: () -> Unit) { try { block(); fail("Expected $code") } catch (e: Failure) { assertEquals(code, e.code) } }

    @Test fun reviewTimestampChangeIsRejected() {
        val bytes = buildMemo(account, blockhash, 1000); val review = Review(bytes, context())
        fails("REVIEW_CONTEXT_CHANGED") { review.assertUnchanged(bytes, context().copy(reviewedAt = "2026-10-06T00:00:01Z")) }
    }

    @Test fun feeAndBlockHeightChangesAreRejected() {
        val bytes = buildMemo(account, blockhash, 1000); val review = Review(bytes, context())
        fails("REVIEW_CONTEXT_CHANGED") { review.assertUnchanged(bytes, context().copy(feeLamports = "5002")) }
        fails("REVIEW_CONTEXT_CHANGED") { review.assertUnchanged(bytes, context().copy(lastValidBlockHeight = "124")) }
    }

    @Test fun signingGateRefusesWalletAccountThatIsNotTheFeePayer() {
        val bytes = buildMemo(account, blockhash, 1000); val review = Review(bytes, context())
        val otherAccount = Base58.encode(ByteArray(32) { 11 })
        assertFalse(canSign(review, bytes, context(), gate.copy(account = otherAccount)))
        assertTrue(canSign(review, bytes, context(), gate))
    }

    @Test fun signingGateRefusesMessageChangedAfterReview() {
        val bytes = buildMemo(account, blockhash, 1000); val review = Review(bytes, context())
        val changed = buildMemo(account, Base58.encode(ByteArray(32) { 10 }), 1000)
        assertFalse(canSign(review, changed, context(), gate))
    }

    @Test fun storedHashesAreUnaffectedByCallerBufferChanges() {
        val bytes = buildMemo(account, blockhash, 1000)
        val review = Review(bytes, context())
        val storedMessage = review.messageSha256; val storedCard = review.cardHash
        bytes[bytes.lastIndex] = (bytes[bytes.lastIndex].toInt() xor 1).toByte()
        assertEquals(storedMessage, review.messageSha256)
        assertEquals(storedCard, review.cardHash)
        assertEquals(storedMessage, sha256Hex(parseTransaction(review.unsignedBytes()).messageBytes()))
    }

    @Test fun messageHashIsIndependentOfReviewContextButContextHashIsNot() {
        val bytes = buildMemo(account, blockhash, 1000)
        val a = Review(bytes, context()); val b = Review(bytes, context().copy(reviewedAt = "2026-10-06T01:00:00Z"))
        assertEquals(a.messageSha256, b.messageSha256)
        assertNotEquals(a.contextHash, b.contextHash)
    }
}
