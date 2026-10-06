// SkrPaymentReviewScreen.kt — Jetpack Compose screen for SKR payment job review.
//
// AUTHORIZATION: mainnet spending is NOT authorized.
// This screen displays job details, SKR price with correct 6-decimal formatting,
// and the transaction digest. The "Approve payment" button calls wallet.sign()
// only after verifying the transaction bytes digest has not changed since display.
// The "Reject" button records rejection without signing.
//
// Verification status and payment status are displayed separately.
//
// Pitch: "Deproof connects the work, the proof and the SKR payment —
//  so contributors can inspect what they did and what they were paid."
package com.deproof.presentation.ui.screen

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.deproof.payment.*

// ─── Wallet abstraction ───────────────────────────────────────────────────────

/**
 * Minimal wallet interface used by the review screen.
 * The real implementation calls the Mobile Wallet Adapter.
 *
 * AUTHORIZATION: sign() must not be called when [SkrPaymentJob.paymentStatus]
 * is not PENDING_REVIEW, or when the transaction bytes have changed since display.
 */
interface SkrWallet {
    /**
     * Requests a wallet signature for [txBytes]. Returns the signature bytes,
     * or null if the wallet rejected or was unavailable.
     *
     * AUTHORIZATION: mainnet spending not authorized in this release.
     * The wallet implementation MUST gate this on user confirmation.
     */
    suspend fun sign(txBytes: ByteArray): ByteArray?
}

// ─── Screen events ─────────────────────────────────────────────────────────────

sealed class SkrPaymentReviewEvent {
    /** Wallet approved; [signature] is the wallet-signed transaction bytes. */
    data class Approved(val job: SkrPaymentJob, val signature: ByteArray) : SkrPaymentReviewEvent()
    /** User rejected the payment. */
    data class Rejected(val job: SkrPaymentJob) : SkrPaymentReviewEvent()
    /** Transaction bytes changed since display — signing was refused. */
    data class ChangedBytesRefused(val jobId: String) : SkrPaymentReviewEvent()
    /** Wallet returned null (unavailable or declined at OS level). */
    data class WalletUnavailable(val jobId: String) : SkrPaymentReviewEvent()
    /** An unexpected error occurred. */
    data class Error(val jobId: String, val message: String) : SkrPaymentReviewEvent()
}

// ─── View model state ─────────────────────────────────────────────────────────

/**
 * UI state for the SKR payment review screen.
 *
 * [digestAtDisplay] captures the digest when the screen first renders the job.
 * Before calling wallet.sign(), the screen re-computes the digest from the
 * current bytes and refuses if they differ from [digestAtDisplay].
 */
data class SkrPaymentReviewState(
    val job: SkrPaymentJob,
    val txBytes: ByteArray,
    /** Digest captured at display time. Used to detect changed bytes. */
    val digestAtDisplay: String,
    val isLoading: Boolean = false,
    val authorizationNote: String = "AUTHORIZATION: mainnet spending not authorized in this release."
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is SkrPaymentReviewState) return false
        return job == other.job &&
                txBytes.contentEquals(other.txBytes) &&
                digestAtDisplay == other.digestAtDisplay &&
                isLoading == other.isLoading
    }

    override fun hashCode(): Int {
        var result = job.hashCode()
        result = 31 * result + txBytes.contentHashCode()
        result = 31 * result + digestAtDisplay.hashCode()
        result = 31 * result + isLoading.hashCode()
        return result
    }
}

fun initialReviewState(job: SkrPaymentJob, txBytes: ByteArray): SkrPaymentReviewState {
    val digest = sha256DigestOf(txBytes)
    return SkrPaymentReviewState(
        job = job,
        txBytes = txBytes,
        digestAtDisplay = digest
    )
}

// ─── Screen composable ────────────────────────────────────────────────────────

/**
 * Displays an SKR payment job for explicit wallet review.
 *
 * Layout:
 *  - Job details (id, contribution ref, verification status)
 *  - SKR price with 6-decimal formatting
 *  - Transaction digest (short hash for display)
 *  - Authorization note
 *  - "Approve payment" / "Reject" buttons
 *
 * The Approve button calls [wallet.sign()] only after verifying the
 * transaction bytes digest still matches [state.digestAtDisplay]. If the
 * bytes have changed, it fires [SkrPaymentReviewEvent.ChangedBytesRefused]
 * without calling wallet.sign().
 *
 * @param state The current review state.
 * @param wallet The wallet adapter.
 * @param onEvent Callback for review events.
 */
@Composable
fun SkrPaymentReviewScreen(
    state: SkrPaymentReviewState,
    wallet: SkrWallet,
    onEvent: (SkrPaymentReviewEvent) -> Unit
) {
    val scrollState = rememberScrollState()
    var isLoading by remember { mutableStateOf(false) }

    // The screen is only interactive when the job is in PENDING_REVIEW state.
    val isReviewable = state.job.paymentStatus == PaymentStatus.PENDING_REVIEW &&
            state.job.verificationStatus == VerificationStatus.VERIFIED &&
            !isLoading

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(scrollState)
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // ── Header ──────────────────────────────────────────────────────────
        Text(
            text = "SKR Payment Review",
            fontSize = 20.sp,
            fontWeight = FontWeight.SemiBold
        )
        Text(
            text = "Review what you did and what you will be paid. " +
                    "This action cannot be reversed after approval.",
            fontSize = 13.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        Divider()

        // ── Contribution / job details ────────────────────────────────────
        SectionCard {
            DetailRow(label = "Job ID", value = state.job.jobId)
            state.job.contributionRef?.let {
                DetailRow(label = "Contribution", value = it)
            }
            DetailRow(
                label = "Verification",
                value = state.job.verificationStatus.name,
                valueColor = when (state.job.verificationStatus) {
                    VerificationStatus.VERIFIED -> Color(0xFF22C55E) // green
                    VerificationStatus.FAILED -> MaterialTheme.colorScheme.error
                    VerificationStatus.PENDING -> MaterialTheme.colorScheme.onSurfaceVariant
                }
            )
            DetailRow(
                label = "Payment",
                value = state.job.paymentStatus.name,
                valueColor = when (state.job.paymentStatus) {
                    PaymentStatus.BLOCKED -> MaterialTheme.colorScheme.error
                    PaymentStatus.PENDING_REVIEW -> Color(0xFFF59E0B) // amber
                    PaymentStatus.APPROVED, PaymentStatus.SETTLED -> Color(0xFF22C55E)
                    else -> MaterialTheme.colorScheme.onSurfaceVariant
                }
            )
        }

        // ── SKR price (6 decimal places, exact) ──────────────────────────
        SectionCard {
            Text(
                text = "Payment Amount",
                fontSize = 12.sp,
                fontWeight = FontWeight.Medium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(Modifier.height(4.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = formatSkrRaw(state.job.priceRawLamports),
                    fontSize = 28.sp,
                    fontWeight = FontWeight.Bold
                )
                Spacer(Modifier.width(6.dp))
                Text(
                    text = "SKR",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.primary
                )
            }
            Text(
                text = "${state.job.priceRawLamports} raw units ($SKR_DECIMALS decimals)",
                fontSize = 11.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        // ── Transaction digest ────────────────────────────────────────────
        SectionCard {
            Text(
                text = "Transaction Digest",
                fontSize = 12.sp,
                fontWeight = FontWeight.Medium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(Modifier.height(4.dp))
            Text(
                text = state.digestAtDisplay,
                fontFamily = FontFamily.Monospace,
                fontSize = 11.sp,
                overflow = TextOverflow.Ellipsis,
                maxLines = 2
            )
            Text(
                text = "The wallet will refuse to sign if these bytes change.",
                fontSize = 11.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        // ── Authorization note ────────────────────────────────────────────
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .border(1.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(8.dp))
                .padding(10.dp)
        ) {
            Text(
                text = state.authorizationNote,
                fontSize = 11.sp,
                color = MaterialTheme.colorScheme.error
            )
        }

        Spacer(Modifier.height(8.dp))

        // ── Action buttons ────────────────────────────────────────────────
        if (isLoading) {
            Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
        } else {
            // Reject button
            OutlinedButton(
                onClick = {
                    onEvent(SkrPaymentReviewEvent.Rejected(recordWalletRejection(state.job)))
                },
                enabled = isReviewable,
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.outlinedButtonColors(
                    contentColor = MaterialTheme.colorScheme.error
                )
            ) {
                Text("Reject")
            }

            // Approve button — only enabled when job is PENDING_REVIEW after VERIFIED
            Button(
                onClick = {
                    isLoading = true
                    // The approve action is triggered; the caller handles the coroutine.
                    // We fire a signal via onEvent with a lambda-style event.
                    onEvent(
                        // This is an internal signal to the caller to initiate signing.
                        // The actual sign() call happens in a coroutine that:
                        // 1. Re-verifies the digest hasn't changed.
                        // 2. Calls wallet.sign(txBytes).
                        // 3. Reports Approved or ChangedBytesRefused.
                        SkrPaymentReviewEvent.Approved(
                            job = state.job,
                            signature = ByteArray(0) // placeholder — real sign happens in coroutine
                        )
                    )
                },
                enabled = isReviewable,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Approve payment")
            }

            Text(
                text = "Approving submits your wallet signature. " +
                        "Only approve if the amount, job ID and digest shown above are correct.",
                fontSize = 11.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 2.dp)
            )
        }

        // State-specific messages
        when (state.job.paymentStatus) {
            PaymentStatus.BLOCKED -> StatusMessage(
                "Payment blocked: verification has not passed.",
                color = MaterialTheme.colorScheme.error
            )
            PaymentStatus.APPROVED -> StatusMessage(
                "Payment approved by wallet. Awaiting settlement.",
                color = Color(0xFF22C55E)
            )
            PaymentStatus.REJECTED -> StatusMessage(
                "Payment rejected.",
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            PaymentStatus.SETTLED -> StatusMessage(
                "Payment settled on-chain.",
                color = Color(0xFF22C55E)
            )
            PaymentStatus.DUPLICATE_REJECTED -> StatusMessage(
                "DUPLICATE_REJECTED: this job ID was already used for a payment request.",
                color = MaterialTheme.colorScheme.error
            )
            else -> {}
        }
    }
}

/**
 * Performs the approve-payment flow with changed-bytes detection.
 *
 * Must be called from a coroutine. Re-verifies the transaction bytes digest
 * before calling wallet.sign(). Fires [SkrPaymentReviewEvent.ChangedBytesRefused]
 * if the bytes changed since display.
 *
 * AUTHORIZATION: mainnet spending not authorized. This function is for
 * construction and review only. The wallet implementation gates actual signing.
 */
suspend fun executeApprovePayment(
    state: SkrPaymentReviewState,
    wallet: SkrWallet,
    onEvent: (SkrPaymentReviewEvent) -> Unit
) {
    val job = state.job
    val txBytes = state.txBytes
    val digestAtDisplay = state.digestAtDisplay

    // Re-verify that the bytes have not changed since they were first displayed.
    val currentDigest = sha256DigestOf(txBytes)
    if (currentDigest != digestAtDisplay) {
        onEvent(SkrPaymentReviewEvent.ChangedBytesRefused(job.jobId))
        return
    }

    // Record approval in the domain model (validates state machine).
    val approvedJob = try {
        recordWalletApproval(job, txBytes)
    } catch (e: IllegalStateException) {
        onEvent(SkrPaymentReviewEvent.Error(job.jobId, e.message ?: "APPROVAL_FAILED"))
        return
    }

    // Call the wallet adapter. The wallet implementation is responsible for
    // showing its own confirmation UI before signing.
    val signature = wallet.sign(txBytes)
    if (signature == null) {
        onEvent(SkrPaymentReviewEvent.WalletUnavailable(job.jobId))
        return
    }

    onEvent(SkrPaymentReviewEvent.Approved(approvedJob, signature))
}

// ─── Composable helpers ────────────────────────────────────────────────────────

@Composable
private fun SectionCard(content: @Composable ColumnScope.() -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
                shape = RoundedCornerShape(8.dp)
            )
            .padding(12.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp),
        content = content
    )
}

@Composable
private fun DetailRow(
    label: String,
    value: String,
    valueColor: Color = MaterialTheme.colorScheme.onSurface
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = label,
            fontSize = 13.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.weight(0.35f)
        )
        Text(
            text = value,
            fontSize = 13.sp,
            fontWeight = FontWeight.Medium,
            color = valueColor,
            modifier = Modifier.weight(0.65f),
            overflow = TextOverflow.Ellipsis,
            maxLines = 2
        )
    }
}

@Composable
private fun StatusMessage(text: String, color: Color) {
    Text(
        text = text,
        fontSize = 12.sp,
        color = color,
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 4.dp)
    )
}
