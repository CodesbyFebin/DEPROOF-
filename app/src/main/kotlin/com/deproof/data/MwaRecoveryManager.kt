package com.deproof.data

import android.util.Log
import com.deproof.domain.model.Receipt
import com.deproof.domain.model.SignatureStatus
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.util.concurrent.ConcurrentHashMap

// ========== Crash Recovery State ==========

data class PendingApproval(
    val transactionId: String,
    val createdAt: Long = System.currentTimeMillis(),
    val reviewedMessageHash: String,
    val expectedSignatureStatus: SignatureStatus = SignatureStatus.PENDING
)

enum class ApprovalReconciliationStatus {
    PENDING,
    SIGNED,
    ABANDONED,
    ERROR
}

data class ApprovalReconciliation(
    val transactionId: String,
    val status: ApprovalReconciliationStatus,
    val finalSignature: String? = null,
    val errorMessage: String? = null,
    val reconciliedAt: Long = System.currentTimeMillis()
)

// ========== Recovery Manager ==========

class MwaRecoveryManager(
    private val walletRepository: Any // WalletRepository interface
) {
    companion object {
        const val TAG = "MwaRecoveryManager"
        const val APPROVAL_TIMEOUT_MS = 5 * 60 * 1000 // 5 minutes
    }

    private val pendingApprovalsLock = Mutex()
    private val pendingApprovals = ConcurrentHashMap<String, PendingApproval>()
    private val reconciliationLog = mutableListOf<ApprovalReconciliation>()

    suspend fun recordPendingApproval(
        transactionId: String,
        reviewedMessageHash: String
    ): Result<Unit> {
        return try {
            pendingApprovalsLock.withLock {
                pendingApprovals[transactionId] = PendingApproval(
                    transactionId = transactionId,
                    reviewedMessageHash = reviewedMessageHash
                )
            }
            Log.d(TAG, "Recorded pending approval for $transactionId")
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to record pending approval: ${e.message}", e)
            Result.failure(e)
        }
    }

    suspend fun clearPendingApproval(transactionId: String): Result<Unit> {
        return try {
            pendingApprovalsLock.withLock {
                pendingApprovals.remove(transactionId)
            }
            Log.d(TAG, "Cleared pending approval for $transactionId")
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to clear pending approval: ${e.message}", e)
            Result.failure(e)
        }
    }

    suspend fun reconcilePendingApprovals(): Result<List<ApprovalReconciliation>> {
        return try {
            val results = mutableListOf<ApprovalReconciliation>()

            pendingApprovalsLock.withLock {
                val expiredTransactionIds = mutableListOf<String>()

                for ((txId, approval) in pendingApprovals) {
                    val reconciliation = reconcileSingleApproval(approval)
                    results.add(reconciliation)
                    reconciliationLog.add(reconciliation)

                    // Clear expired pending approvals
                    if (isApprovalExpired(approval)) {
                        expiredTransactionIds.add(txId)
                    } else if (reconciliation.status == ApprovalReconciliationStatus.SIGNED ||
                        reconciliation.status == ApprovalReconciliationStatus.ABANDONED) {
                        expiredTransactionIds.add(txId)
                    }
                }

                // Clean up processed approvals
                for (txId in expiredTransactionIds) {
                    pendingApprovals.remove(txId)
                }
            }

            Log.d(TAG, "Reconciled ${results.size} pending approvals")
            Result.success(results)
        } catch (e: Exception) {
            Log.e(TAG, "Reconciliation failed: ${e.message}", e)
            Result.failure(e)
        }
    }

    private suspend fun reconcileSingleApproval(
        approval: PendingApproval
    ): ApprovalReconciliation {
        return try {
            // Check wallet for authorization status
            // This is a stub implementation - actual wallet query happens here
            val walletStatus = queryWalletApprovalStatus(approval.transactionId)

            return when {
                walletStatus == "SIGNED" -> {
                    ApprovalReconciliation(
                        transactionId = approval.transactionId,
                        status = ApprovalReconciliationStatus.SIGNED
                    )
                }
                walletStatus == "REJECTED" || isApprovalExpired(approval) -> {
                    ApprovalReconciliation(
                        transactionId = approval.transactionId,
                        status = ApprovalReconciliationStatus.ABANDONED,
                        errorMessage = "Wallet denied or approval expired"
                    )
                }
                walletStatus == "PENDING" -> {
                    ApprovalReconciliation(
                        transactionId = approval.transactionId,
                        status = ApprovalReconciliationStatus.PENDING
                    )
                }
                else -> {
                    ApprovalReconciliation(
                        transactionId = approval.transactionId,
                        status = ApprovalReconciliationStatus.ERROR,
                        errorMessage = "Unknown wallet status: $walletStatus"
                    )
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error reconciling approval ${approval.transactionId}: ${e.message}", e)
            ApprovalReconciliation(
                transactionId = approval.transactionId,
                status = ApprovalReconciliationStatus.ERROR,
                errorMessage = e.message ?: "Unknown error"
            )
        }
    }

    private suspend fun queryWalletApprovalStatus(transactionId: String): String {
        // Stub implementation - actual wallet query would go here
        // In production, this would query the wallet's session state
        return "PENDING"
    }

    private fun isApprovalExpired(approval: PendingApproval): Boolean {
        val elapsedMs = System.currentTimeMillis() - approval.createdAt
        return elapsedMs > APPROVAL_TIMEOUT_MS
    }

    fun getPendingApprovals(): List<PendingApproval> {
        return pendingApprovals.values.toList()
    }

    fun getReconciliationLog(): List<ApprovalReconciliation> {
        return reconciliationLog.toList()
    }

    suspend fun clearReconciliationLog(): Result<Unit> {
        return try {
            pendingApprovalsLock.withLock {
                reconciliationLog.clear()
            }
            Log.d(TAG, "Cleared reconciliation log")
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to clear reconciliation log: ${e.message}", e)
            Result.failure(e)
        }
    }
}
