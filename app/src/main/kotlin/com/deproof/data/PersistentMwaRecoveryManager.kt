package com.deproof.data

import android.util.Log
import com.deproof.data.repository.PendingApprovalRepository
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

class PersistentMwaRecoveryManager(
    private val approvalRepository: PendingApprovalRepository
) {

    companion object {
        const val TAG = "PersistentMwaRecoveryManager"
    }

    private val persistenceLock = Mutex()
    private val memoryManager = MwaRecoveryManager(null as Any?)

    suspend fun recordPendingApproval(
        transactionId: String,
        reviewedMessageHash: String
    ): Result<Unit> = persistenceLock.withLock {
        return try {
            // Record in memory
            memoryManager.recordPendingApproval(transactionId, reviewedMessageHash)

            // Also persist to database
            val result = approvalRepository.recordPendingApproval(
                transactionId = transactionId,
                reviewedMessageHash = reviewedMessageHash
            )

            if (result.isFailure) {
                Log.e(TAG, "Failed to persist approval: ${result.exceptionOrNull()?.message}")
            } else {
                Log.d(TAG, "Recorded and persisted pending approval for $transactionId")
            }

            Result.success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "Error recording pending approval: ${e.message}", e)
            Result.failure(e)
        }
    }

    suspend fun clearPendingApproval(transactionId: String): Result<Unit> = persistenceLock.withLock {
        return try {
            // Clear from memory
            memoryManager.clearPendingApproval(transactionId)

            // Also delete from database
            val result = approvalRepository.clearPendingApproval(transactionId)

            if (result.isFailure) {
                Log.e(TAG, "Failed to delete from database: ${result.exceptionOrNull()?.message}")
            } else {
                Log.d(TAG, "Cleared and deleted pending approval for $transactionId")
            }

            Result.success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "Error clearing pending approval: ${e.message}", e)
            Result.failure(e)
        }
    }

    suspend fun reconcilePendingApprovals(): Result<List<ApprovalReconciliation>> = persistenceLock.withLock {
        return try {
            // Reconcile from database (more reliable than memory)
            val result = approvalRepository.reconcilePendingApprovals()

            if (result.isFailure) {
                Log.e(TAG, "Failed to reconcile from database: ${result.exceptionOrNull()?.message}")
                return result
            }

            val reconciliations = result.getOrNull() ?: emptyList()

            // Update in-memory state via memoryManager
            memoryManager.reconcilePendingApprovals()

            // Process reconciliation results
            for (reconciliation in reconciliations) {
                when (reconciliation.status) {
                    ApprovalReconciliationStatus.SIGNED -> {
                        Log.d(TAG, "Approval signed: ${reconciliation.transactionId}")
                    }
                    ApprovalReconciliationStatus.ABANDONED -> {
                        Log.d(TAG, "Approval abandoned: ${reconciliation.transactionId}")
                    }
                    ApprovalReconciliationStatus.ERROR -> {
                        Log.e(TAG, "Approval error: ${reconciliation.errorMessage}")
                    }
                    ApprovalReconciliationStatus.PENDING -> {
                        Log.d(TAG, "Approval still pending: ${reconciliation.transactionId}")
                    }
                }
            }

            Result.success(reconciliations)
        } catch (e: Exception) {
            Log.e(TAG, "Error reconciling approvals: ${e.message}", e)
            Result.failure(e)
        }
    }

    suspend fun loadFromDatabase(): Result<Unit> {
        return try {
            val result = approvalRepository.getPendingApprovals()

            if (result.isFailure) {
                return result.map { }
            }

            val approvals = result.getOrNull() ?: emptyList()

            // Reload in-memory state from database
            for (approval in approvals) {
                memoryManager.recordPendingApproval(
                    transactionId = approval.transactionId,
                    reviewedMessageHash = approval.reviewedMessageHash
                )
            }

            Log.d(TAG, "Loaded ${approvals.size} pending approvals from database")
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to load from database: ${e.message}", e)
            Result.failure(e)
        }
    }

    suspend fun cleanup(ageMs: Long = 24 * 60 * 60 * 1000): Result<Int> {
        return try {
            val result = approvalRepository.cleanupReconciledOlderThan(ageMs)

            if (result.isFailure) {
                Log.e(TAG, "Failed to cleanup: ${result.exceptionOrNull()?.message}")
                return result
            }

            val deleted = result.getOrNull() ?: 0
            Log.d(TAG, "Cleaned up $deleted old approvals")

            Result.success(deleted)
        } catch (e: Exception) {
            Log.e(TAG, "Error during cleanup: ${e.message}", e)
            Result.failure(e)
        }
    }
}
