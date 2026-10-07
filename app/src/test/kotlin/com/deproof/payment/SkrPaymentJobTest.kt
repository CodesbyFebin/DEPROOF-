// SkrPaymentJobTest.kt — JVM unit tests for the Android SKR payment job model.
//
// AUTHORIZATION: mainnet spending is NOT authorized.
// These tests cover construction and review state management only.
// No tokens are minted, transferred, or settled by these tests.
//
// Test categories:
//  1. BLOCKED state when no verified result exists
//  2. Duplicate payment prevention
//  3. Changed-bytes rejection
//  4. Decimal boundary formatting
package com.deproof.payment

import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test

// ─── Fixtures ─────────────────────────────────────────────────────────────────
//
// DEVNET TEST FIXTURES — NOT real wallets, NOT official SKR addresses.
// These values are used only for unit testing the Android payment model.

private const val TEST_JOB_ID = "test-job-001"
private const val TEST_SOURCE = "2dn6KSx4dCoMHqqvZYMc9bgXEkCsZJVxYBfHfwVJQXJt"
private const val TEST_DEST = "3Cqk7HnGLKaEnHpFpGLhGSmHfGPVJqvqbfq4t5sNMuHs"
private const val TEST_OWNER = "11111111111111111111111111111111"
private const val TEST_TX_BYTES = "deadbeef"

private fun testTxBytes(): ByteArray = TEST_TX_BYTES.toByteArray()
private fun alteredTxBytes(): ByteArray = "altered_bytes".toByteArray()

private fun pendingJob(jobId: String = TEST_JOB_ID) = SkrPaymentJob(
    jobId = jobId,
    priceRawLamports = 1_500_000L, // 1.500000 SKR
    verificationStatus = VerificationStatus.PENDING,
    paymentStatus = PaymentStatus.BLOCKED,
    source = TEST_SOURCE,
    destination = TEST_DEST,
    owner = TEST_OWNER,
    createdAt = "2026-10-07T00:00:00Z"
)

private fun verifiedJob(jobId: String = TEST_JOB_ID) = pendingJob(jobId).copy(
    verificationStatus = VerificationStatus.VERIFIED
)

// ─── Test class ───────────────────────────────────────────────────────────────

class SkrPaymentJobTest {

    @Before
    fun setUp() {
        resetIssuedJobs()
    }

    @After
    fun tearDown() {
        resetIssuedJobs()
    }

    // ─────────────────────────────────────────────────────────────────────────
    // 1. BLOCKED state when no verified result exists
    // ─────────────────────────────────────────────────────────────────────────

    @Test
    fun `BLOCKED when verification is PENDING`() {
        val job = pendingJob()
        val result = requestPaymentReview(job, testTxBytes())
        assertEquals(PaymentStatus.BLOCKED, result.paymentStatus)
        assertNull(result.txBytesDigest)
        assertFalse(result.walletApproved)
    }

    @Test
    fun `BLOCKED when verification is FAILED`() {
        val job = pendingJob().copy(verificationStatus = VerificationStatus.FAILED)
        val result = requestPaymentReview(job, testTxBytes())
        assertEquals(PaymentStatus.BLOCKED, result.paymentStatus)
        assertFalse(result.walletApproved)
    }

    @Test
    fun `PENDING_REVIEW only when VERIFIED`() {
        val job = verifiedJob()
        val result = requestPaymentReview(job, testTxBytes())
        assertEquals(PaymentStatus.PENDING_REVIEW, result.paymentStatus)
        assertNotNull(result.txBytesDigest)
        assertTrue(result.txBytesDigest!!.startsWith("sha256:"))
        assertFalse(result.walletApproved)
    }

    @Test
    fun `approve is illegal from BLOCKED state`() {
        val job = pendingJob().copy(paymentStatus = PaymentStatus.BLOCKED)
        val ex = assertThrows(IllegalArgumentException::class.java) {
            // BLOCKED jobs cannot be constructed with walletApproved=true
            job.copy(walletApproved = true)
        }
        assertTrue(ex.message!!.contains("walletApproved must be false when BLOCKED"))
    }

    @Test
    fun `recordWalletApproval fails when paymentStatus is not PENDING_REVIEW`() {
        val job = pendingJob() // BLOCKED
        val tx = testTxBytes()
        val digest = sha256DigestOf(tx)
        // Manually set digest to simulate a stale job that somehow has one
        val staleJob = job.copy(txBytesDigest = digest)
        val ex = assertThrows(IllegalArgumentException::class.java) {
            recordWalletApproval(staleJob, tx)
        }
        assertTrue(ex.message!!.contains("WRONG_STATE"))
    }

    // ─────────────────────────────────────────────────────────────────────────
    // 2. Duplicate payment prevention
    // ─────────────────────────────────────────────────────────────────────────

    @Test
    fun `duplicate jobId is rejected`() {
        val job = verifiedJob()
        val tx = testTxBytes()

        // First request succeeds
        val first = requestPaymentReview(job, tx)
        assertEquals(PaymentStatus.PENDING_REVIEW, first.paymentStatus)

        // Second request with same jobId throws
        val ex = assertThrows(IllegalStateException::class.java) {
            requestPaymentReview(job, tx)
        }
        assertTrue(ex.message!!.contains("DUPLICATE_PAYMENT_REJECTED"))
        assertTrue(ex.message!!.contains(TEST_JOB_ID))
    }

    @Test
    fun `different job IDs are each accepted once`() {
        val jobA = verifiedJob("job-A")
        val jobB = verifiedJob("job-B")
        val tx = testTxBytes()

        val resultA = requestPaymentReview(jobA, tx)
        val resultB = requestPaymentReview(jobB, tx)
        assertEquals(PaymentStatus.PENDING_REVIEW, resultA.paymentStatus)
        assertEquals(PaymentStatus.PENDING_REVIEW, resultB.paymentStatus)
    }

    @Test
    fun `BLOCKED job does not register in duplicate guard`() {
        val pendingVerificationJob = pendingJob()
        val tx = testTxBytes()

        // BLOCKED — should not be registered
        val blocked = requestPaymentReview(pendingVerificationJob, tx)
        assertEquals(PaymentStatus.BLOCKED, blocked.paymentStatus)

        // After verification passes, the same jobId can still be used once
        val nowVerified = pendingVerificationJob.copy(
            verificationStatus = VerificationStatus.VERIFIED
        )
        val approved = requestPaymentReview(nowVerified, tx)
        assertEquals(PaymentStatus.PENDING_REVIEW, approved.paymentStatus)
    }

    // ─────────────────────────────────────────────────────────────────────────
    // 3. Changed-bytes rejection
    // ─────────────────────────────────────────────────────────────────────────

    @Test
    fun `approval succeeds when bytes unchanged`() {
        val job = verifiedJob()
        val tx = testTxBytes()

        val pendingReview = requestPaymentReview(job, tx)
        val approved = recordWalletApproval(pendingReview, tx)

        assertEquals(PaymentStatus.APPROVED, approved.paymentStatus)
        assertTrue(approved.walletApproved)
    }

    @Test
    fun `approval fails when bytes changed`() {
        val job = verifiedJob()
        val tx = testTxBytes()
        val alteredTx = alteredTxBytes()

        val pendingReview = requestPaymentReview(job, tx)

        val ex = assertThrows(IllegalStateException::class.java) {
            recordWalletApproval(pendingReview, alteredTx)
        }
        assertTrue(ex.message!!.contains("CHANGED_BYTES_REJECTED"))
    }

    @Test
    fun `approval fails when no digest was stored`() {
        val job = verifiedJob().copy(txBytesDigest = null, paymentStatus = PaymentStatus.PENDING_REVIEW)
        val ex = assertThrows(IllegalStateException::class.java) {
            recordWalletApproval(job, testTxBytes())
        }
        assertTrue(ex.message!!.contains("CHANGED_BYTES_REJECTED"))
    }

    @Test
    fun `rejection advances state without signing`() {
        val job = verifiedJob()
        val tx = testTxBytes()
        val pendingReview = requestPaymentReview(job, tx)

        val rejected = recordWalletRejection(pendingReview)
        assertEquals(PaymentStatus.REJECTED, rejected.paymentStatus)
        assertFalse(rejected.walletApproved)
    }

    @Test
    fun `rejection from non-PENDING_REVIEW state throws`() {
        val job = pendingJob() // BLOCKED
        val ex = assertThrows(IllegalArgumentException::class.java) {
            recordWalletRejection(job)
        }
        assertTrue(ex.message!!.contains("WRONG_STATE"))
    }

    // ─────────────────────────────────────────────────────────────────────────
    // 4. Decimal boundary formatting (formatSkrRaw)
    // ─────────────────────────────────────────────────────────────────────────

    @Test
    fun `format zero`() {
        assertEquals("0.000000", formatSkrRaw(0L))
    }

    @Test
    fun `format one smallest unit`() {
        assertEquals("0.000001", formatSkrRaw(1L))
    }

    @Test
    fun `format one whole SKR`() {
        assertEquals("1.000000", formatSkrRaw(1_000_000L))
    }

    @Test
    fun `format fractional SKR`() {
        assertEquals("1.500000", formatSkrRaw(1_500_000L))
    }

    @Test
    fun `format large value`() {
        // 1,000 SKR
        assertEquals("1000.000000", formatSkrRaw(1_000_000_000L))
    }

    @Test
    fun `format preserves leading zeros in fraction`() {
        // 0.000100 — fraction part is 100, needs 3 leading zeros
        assertEquals("0.000100", formatSkrRaw(100L))
    }

    @Test
    fun `format 999999 raw units`() {
        // Just under 1 SKR
        assertEquals("0.999999", formatSkrRaw(999_999L))
    }

    @Test
    fun `negative rawAmount throws`() {
        val ex = assertThrows(IllegalArgumentException::class.java) {
            formatSkrRaw(-1L)
        }
        assertTrue(ex.message!!.contains("non-negative"))
    }

    // ─────────────────────────────────────────────────────────────────────────
    // 5. parseSkrRaw round-trip
    // ─────────────────────────────────────────────────────────────────────────

    @Test
    fun `parse round-trips with format`() {
        val cases = listOf(0L, 1L, 100L, 999_999L, 1_000_000L, 1_500_000L, 1_000_000_000L)
        for (raw in cases) {
            val formatted = formatSkrRaw(raw)
            val parsed = parseSkrRaw(formatted)
            assertEquals("Round-trip failed for $raw", raw, parsed)
        }
    }

    @Test
    fun `parse integer string`() {
        assertEquals(1_000_000L, parseSkrRaw("1"))
    }

    @Test
    fun `parse too many decimals returns null`() {
        assertNull(parseSkrRaw("1.1234567")) // 7 decimal places, max is 6
    }

    @Test
    fun `parse empty string returns null`() {
        assertNull(parseSkrRaw(""))
    }

    @Test
    fun `parse non-numeric returns null`() {
        assertNull(parseSkrRaw("abc"))
    }

    // ─────────────────────────────────────────────────────────────────────────
    // 6. SHA-256 digest
    // ─────────────────────────────────────────────────────────────────────────

    @Test
    fun `sha256DigestOf returns sha256-prefixed hex`() {
        val digest = sha256DigestOf("test".toByteArray())
        assertTrue(digest.startsWith("sha256:"))
        assertEquals(7 + 64, digest.length) // "sha256:" + 64 hex chars
    }

    @Test
    fun `sha256DigestOf is deterministic`() {
        val bytes = "deproof-skr".toByteArray()
        assertEquals(sha256DigestOf(bytes), sha256DigestOf(bytes))
    }

    @Test
    fun `sha256DigestOf different bytes differ`() {
        val a = sha256DigestOf("bytes-a".toByteArray())
        val b = sha256DigestOf("bytes-b".toByteArray())
        assertNotEquals(a, b)
    }

    // ─────────────────────────────────────────────────────────────────────────
    // 7. Data class invariants
    // ─────────────────────────────────────────────────────────────────────────

    @Test
    fun `blank jobId throws`() {
        val ex = assertThrows(IllegalArgumentException::class.java) {
            pendingJob().copy(jobId = "   ")
        }
        assertTrue(ex.message!!.contains("jobId"))
    }

    @Test
    fun `zero priceRawLamports throws`() {
        val ex = assertThrows(IllegalArgumentException::class.java) {
            pendingJob().copy(priceRawLamports = 0L)
        }
        assertTrue(ex.message!!.contains("priceRawLamports"))
    }

    @Test
    fun `source equals destination throws`() {
        val ex = assertThrows(IllegalArgumentException::class.java) {
            pendingJob().copy(source = TEST_SOURCE, destination = TEST_SOURCE)
        }
        assertTrue(ex.message!!.contains("source and destination"))
    }

    @Test
    fun `SKR_MINT constant is official value`() {
        assertEquals("SKRbvo6Gf7GondiT3BbTfuRDPqLWei4j2Qy2NPGZhW3", SKR_MINT)
    }

    @Test
    fun `SKR_DECIMALS is 6`() {
        assertEquals(6, SKR_DECIMALS)
    }
}
