package com.deproof.data.repository

import android.util.Log
import com.deproof.data.ApprovalReconciliation
import com.deproof.data.ApprovalReconciliationStatus
import com.deproof.data.PendingApproval
import com.deproof.data.database.dao.PendingApprovalDao
import com.deproof.data.database.entity.PendingApprovalEntity
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class PendingApprovalRepository(
    private val dao: PendingApprovalDao
) {
    companion object {
        const val TAG = "PendingApprovalRepository"
        const val APPROVAL_TIMEOUT_MS = 5 * 60 * 1000 // 5 minutes
    }

    suspend fun recordPendingApproval(
        transactionId: String,
        reviewedMessageHash: String
    ): Result<Unit> {
        return try {
            val approval = PendingApprovalEntity(
                transactionId = transactionId,
                createdAt = System.currentTimeMillis(),
                reviewedMessageHash = reviewedMessageHash,
                expectedSignatureStatus = com.deproof.domain.model.SignatureStatus.PENDING
            )
            dao.insert(approval)
            Log.d(TAG, "Recorded pending approval for $transactionId")
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to record pending approval: ${e.message}", e)
            Result.failure(e)
        }
    }

    suspend fun getPendingApprovals(): Result<List<PendingApproval>> {
        return try {
            val entities = dao.getPending()
            Result.success(entities.map { it.toDomain() })
        } catch (e: Exception) {
            Log.e(TAG, "Failed to get pending approvals: ${e.message}", e)
            Result.failure(e)
        }
    }

    fun observePendingApprovals(): Flow<List<PendingApproval>> {
        return dao.observePending().map { entities ->
            entities.map { it.toDomain() }
        }
    }

    suspend fun getByTransactionId(transactionId: String): Result<PendingApproval?> {
        return try {
            val entity = dao.getByTransactionId(transactionId)
            Result.success(entity?.toDomain())
        } catch (e: Exception) {
            Log.e(TAG, "Failed to get pending approval: ${e.message}", e)
            Result.failure(e)
        }
    }

    suspend fun clearPendingApproval(transactionId: String): Result<Unit> {
        return try {
            dao.deleteByTransactionId(transactionId)
            Log.d(TAG, "Cleared pending approval for $transactionId")
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to clear pending approval: ${e.message}", e)
            Result.failure(e)
        }
    }

    suspend fun markReconciled(
        transactionId: String,
        status: ApprovalReconciliationStatus
    ): Result<Unit> {
        return try {
            dao.markReconciled(transactionId, status.name)
            Log.d(TAG, "Marked approval as reconciled: $transactionId with status $status")
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to mark approval as reconciled: ${e.message}", e)
            Result.failure(e)
        }
    }

    suspend fun reconcilePendingApprovals(): Result<List<ApprovalReconciliation>> {
        return try {
            val pendingEntities = dao.getPending()
            val results = mutableListOf<ApprovalReconciliation>()
            val expiredTime = System.currentTimeMillis() - APPROVAL_TIMEOUT_MS

            for (entity in pendingEntities) {
                val reconciliation = if (entity.createdAt < expiredTime) {
                    // Approval expired
                    dao.markReconciled(entity.transactionId, ApprovalReconciliationStatus.ABANDONED.name)
                    ApprovalReconciliation(
                        transactionId = entity.transactionId,
                        status = ApprovalReconciliationStatus.ABANDONED,
                        errorMessage = "Approval timeout (${APPROVAL_TIMEOUT_MS}ms)"
                    )
                } else {
                    // Still pending or unknown
                    ApprovalReconciliation(
                        transactionId = entity.transactionId,
                        status = ApprovalReconciliationStatus.PENDING
                    )
                }
                results.add(reconciliation)
            }

            Log.d(TAG, "Reconciled ${results.size} pending approvals")
            Result.success(results)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to reconcile pending approvals: ${e.message}", e)
            Result.failure(e)
        }
    }

    suspend fun getExpiredPendingApprovals(): Result<List<PendingApproval>> {
        return try {
            val expiredTime = System.currentTimeMillis() - APPROVAL_TIMEOUT_MS
            val entities = dao.getExpiredPending(expiredTime)
            Result.success(entities.map { it.toDomain() })
        } catch (e: Exception) {
            Log.e(TAG, "Failed to get expired approvals: ${e.message}", e)
            Result.failure(e)
        }
    }

    suspend fun cleanupReconciledOlderThan(ageMs: Long): Result<Int> {
        return try {
            val cutoffTime = System.currentTimeMillis() - ageMs
            val deleted = dao.deleteReconciledOlderThan(cutoffTime)
            Log.d(TAG, "Deleted $deleted reconciled approvals older than $ageMs ms")
            Result.success(deleted)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to cleanup approvals: ${e.message}", e)
            Result.failure(e)
        }
    }

    suspend fun cleanupAllOlderThan(ageMs: Long): Result<Int> {
        return try {
            val cutoffTime = System.currentTimeMillis() - ageMs
            val deleted = dao.deleteOlderThan(cutoffTime)
            Log.d(TAG, "Deleted $deleted approvals older than $ageMs ms")
            Result.success(deleted)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to cleanup all approvals: ${e.message}", e)
            Result.failure(e)
        }
    }
}
