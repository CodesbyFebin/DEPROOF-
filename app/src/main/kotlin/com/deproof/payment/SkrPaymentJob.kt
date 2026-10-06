// SkrPaymentJob.kt — Android-side SKR payment job model and state machine.
//
// AUTHORIZATION: mainnet spending is NOT authorized.
// This file implements construction, review state management and decimal formatting
// for SKR payment jobs. Signing and on-chain submission require explicit wallet
// authorization in a separate step, and are blocked until verificationStatus=VERIFIED.
//
// Pitch: "Deproof connects the work, the proof and the SKR payment —
//  so contributors can inspect what they did and what they were paid."
package com.deproof.payment

import java.math.BigInteger
import java.security.MessageDigest

// ─── Constants ────────────────────────────────────────────────────────────────

/**
 * Official SKR mint address. Verified as of commit 27bffa3.
 * On-chain decimals: [SKR_DECIMALS].
 */
const val SKR_MINT = "SKRbvo6Gf7GondiT3BbTfuRDPqLWei4j2Qy2NPGZhW3"

/**
 * On-chain decimal precision of the SKR token. Verified: decimals=6.
 */
const val SKR_DECIMALS = 6

/**
 * SPL Token Program v1. Consistent with Programs.TOKEN in Core.kt.
 * Requires on-chain verification before mainnet use — see Core.kt audit comment.
 */
// Consistent with Go skr_payment.go and app/build.gradle.kts. Requires on-chain verification before mainnet use.
const val SPL_TOKEN_PROGRAM = "TokenkegQfeZyiNwAJbiyB8671GSjsqyq26BW7NnPGBq"

// ─── Status enumerations ────────────────────────────────────────────────────

/**
 * Verification status of the underlying contribution.
 * Kept separate from [PaymentStatus] to enforce the rule that payment
 * cannot proceed until independent verification has passed.
 */
enum class VerificationStatus {
    /** Verification has not been attempted yet. */
    PENDING,
    /** An independent verifier confirmed the contribution. */
    VERIFIED,
    /** Verification rejected the contribution. */
    FAILED
}

/**
 * Payment lifecycle status. Separate from [VerificationStatus].
 *
 * State machine:
 * [BLOCKED] (initial, when no verified result exists)
 *   → [PENDING_REVIEW] (after verificationStatus=VERIFIED)
 *   → [APPROVED] (wallet holder tapped Approve)
 *   → [SETTLED]  (observed on-chain)
 *   | [REJECTED]  (wallet holder tapped Reject)
 * [DUPLICATE_REJECTED] (jobId already used for a payment request)
 */
enum class PaymentStatus {
    BLOCKED,
    PENDING_REVIEW,
    APPROVED,
    REJECTED,
    SETTLED,
    DUPLICATE_REJECTED
}

// ─── Domain model ─────────────────────────────────────────────────────────────

/**
 * Represents one bounded SKR-priced contribution-job payment request.
 *
 * Key invariants enforced by the data class:
 * - [walletApproved] is never true unless paymentStatus=APPROVED.
 * - [txBytesDigest] is only non-null in PENDING_REVIEW or later states.
 * - Payment status advances independently from verification status.
 * - No staking, escrow, or automatic rewards.
 */
data class SkrPaymentJob(
    /** Stable unique job ID. Duplicate IDs are rejected. */
    val jobId: String,
    /** Optional link to the contribution receipt this payment settles. */
    val contributionRef: String? = null,
    /** Raw SKR amount (6 on-chain decimals). 1 SKR = 1_000_000 raw units. */
    val priceRawLamports: Long,
    /** Independent verification status. Separate from paymentStatus. */
    val verificationStatus: VerificationStatus,
    /** Payment lifecycle status. Starts as BLOCKED until verificationStatus=VERIFIED. */
    val paymentStatus: PaymentStatus,
    /**
     * SHA-256 hex digest of the unsigned transaction bytes shown during review.
     * The wallet MUST refuse to sign if the bytes have changed since display.
     * Only non-null when paymentStatus is PENDING_REVIEW or later.
     */
    val txBytesDigest: String? = null,
    /**
     * True only when the wallet holder explicitly tapped Approve.
     * Never auto-set; never inherited from a prior approval.
     */
    val walletApproved: Boolean = false,
    /** Source SPL token account (base58). */
    val source: String,
    /** Destination SPL token account (base58). */
    val destination: String,
    /** Authority / fee-payer wallet public key (base58). */
    val owner: String,
    /** ISO-8601 creation timestamp. */
    val createdAt: String,
    /** Human-readable note for audit trail. */
    val note: String? = null
) {
    init {
        require(jobId.isNotBlank()) { "jobId must not be blank" }
        require(priceRawLamports > 0) { "priceRawLamports must be positive" }
        require(source != destination) { "source and destination must differ" }
        if (paymentStatus == PaymentStatus.BLOCKED) {
            require(!walletApproved) { "walletApproved must be false when BLOCKED" }
        }
    }
}

// ─── Decimal formatting ────────────────────────────────────────────────────────

/**
 * Formats a raw SKR amount to a human-readable string with exactly [SKR_DECIMALS] decimal places.
 * Uses exact integer arithmetic — no floating point.
 *
 * Examples:
 * - 0L → "0.000000"
 * - 1L → "0.000001"
 * - 1_000_000L → "1.000000"
 * - 1_500_000L → "1.500000"
 */
fun formatSkrRaw(rawAmount: Long): String {
    require(rawAmount >= 0) { "rawAmount must be non-negative" }
    val divisor = 1_000_000L
    val whole = rawAmount / divisor
    val frac = rawAmount % divisor
    return "$whole.${frac.toString().padStart(SKR_DECIMALS, '0')}"
}

/**
 * Parses a decimal string (up to [SKR_DECIMALS] decimal places) to a raw u64 SKR amount.
 * Returns null if the input is invalid or would overflow.
 *
 * Examples:
 * - "1.5" → 1_500_000L
 * - "0.000001" → 1L
 * - "1" → 1_000_000L
 */
fun parseSkrRaw(s: String): Long? {
    val trimmed = s.trim()
    if (trimmed.isEmpty()) return null

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

    if (intPart.isEmpty()) return null
    if (fracPart.length > SKR_DECIMALS) return null
    if ((intPart + fracPart).any { !it.isDigit() }) return null

    val paddedFrac = fracPart.padEnd(SKR_DECIMALS, '0')
    val combined = intPart + paddedFrac

    return try {
        val n = BigInteger(combined)
        if (n < BigInteger.ZERO || n > BigInteger.valueOf(Long.MAX_VALUE)) null
        else n.toLong()
    } catch (_: NumberFormatException) {
        null
    }
}

// ─── SHA-256 digest ─────────────────────────────────────────────────────────

/**
 * Computes "sha256:<hex>" of [bytes]. Used for transaction digest comparison.
 */
fun sha256DigestOf(bytes: ByteArray): String {
    val md = MessageDigest.getInstance("SHA-256")
    return "sha256:" + md.digest(bytes).joinToString("") { "%02x".format(it.toInt() and 0xFF) }
}

// ─── Payment job state machine ───────────────────────────────────────────────

/**
 * Guards against duplicate payment requests for the same job.
 * Maps jobId → first-issued txBytesDigest.
 * Thread-safe via synchronized block.
 */
private val issuedJobs = mutableMapOf<String, String>()

/** Resets the duplicate-payment guard. For testing only. */
fun resetIssuedJobs() {
    synchronized(issuedJobs) { issuedJobs.clear() }
}

/**
 * Advances an [SkrPaymentJob] to [PaymentStatus.PENDING_REVIEW] when
 * [VerificationStatus.VERIFIED]. Returns a new job with the digest set.
 *
 * Throws [IllegalStateException] if the job is a duplicate (same jobId
 * was already issued with a different or same payment request).
 *
 * Does NOT sign or submit anything. The [txBytes] parameter is only used
 * to compute the digest — they must be the exact bytes the wallet will
 * be shown for signing.
 *
 * AUTHORIZATION: mainnet spending not authorized. Construction only.
 */
fun requestPaymentReview(job: SkrPaymentJob, txBytes: ByteArray): SkrPaymentJob {
    val digest = sha256DigestOf(txBytes)

    synchronized(issuedJobs) {
        val existing = issuedJobs[job.jobId]
        if (existing != null) {
            throw IllegalStateException(
                "DUPLICATE_PAYMENT_REJECTED: jobId=${job.jobId} was already issued with digest=$existing"
            )
        }

        if (job.verificationStatus != VerificationStatus.VERIFIED) {
            return job.copy(paymentStatus = PaymentStatus.BLOCKED)
        }

        issuedJobs[job.jobId] = digest
    }

    return job.copy(
        paymentStatus = PaymentStatus.PENDING_REVIEW,
        txBytesDigest = digest,
        walletApproved = false
    )
}

/**
 * Records wallet approval. The [currentTxBytes] must produce the same digest
 * as the one recorded in [job.txBytesDigest]. If the bytes changed since display,
 * the function throws [IllegalStateException] with CHANGED_BYTES_REJECTED.
 *
 * Does NOT sign or submit. Returns an updated job with paymentStatus=APPROVED.
 *
 * AUTHORIZATION: mainnet spending not authorized. This records approval intent only.
 */
fun recordWalletApproval(job: SkrPaymentJob, currentTxBytes: ByteArray): SkrPaymentJob {
    val currentDigest = sha256DigestOf(currentTxBytes)
    val storedDigest = job.txBytesDigest
        ?: throw IllegalStateException("CHANGED_BYTES_REJECTED: no digest was stored for review")

    if (currentDigest != storedDigest) {
        throw IllegalStateException(
            "CHANGED_BYTES_REJECTED: displayed digest=$storedDigest but current=$currentDigest"
        )
    }

    require(job.paymentStatus == PaymentStatus.PENDING_REVIEW) {
        "WRONG_STATE: can only approve from PENDING_REVIEW, current=${job.paymentStatus}"
    }

    return job.copy(
        paymentStatus = PaymentStatus.APPROVED,
        walletApproved = true
    )
}

/**
 * Records wallet rejection. Returns an updated job with paymentStatus=REJECTED.
 * walletApproved remains false.
 */
fun recordWalletRejection(job: SkrPaymentJob): SkrPaymentJob {
    require(job.paymentStatus == PaymentStatus.PENDING_REVIEW) {
        "WRONG_STATE: can only reject from PENDING_REVIEW, current=${job.paymentStatus}"
    }
    return job.copy(
        paymentStatus = PaymentStatus.REJECTED,
        walletApproved = false
    )
}
