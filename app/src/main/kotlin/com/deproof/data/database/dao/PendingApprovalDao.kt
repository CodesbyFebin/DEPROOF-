package com.deproof.data.database.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.deproof.data.database.entity.PendingApprovalEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface PendingApprovalDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(approval: PendingApprovalEntity): Long

    @Update
    suspend fun update(approval: PendingApprovalEntity): Int

    @Delete
    suspend fun delete(approval: PendingApprovalEntity): Int

    @Query("SELECT * FROM pending_approvals WHERE transactionId = :transactionId")
    suspend fun getByTransactionId(transactionId: String): PendingApprovalEntity?

    @Query("SELECT * FROM pending_approvals WHERE isReconciled = 0 ORDER BY createdAt DESC")
    suspend fun getPending(): List<PendingApprovalEntity>

    @Query("SELECT * FROM pending_approvals WHERE isReconciled = 0 ORDER BY createdAt DESC")
    fun observePending(): Flow<List<PendingApprovalEntity>>

    @Query("SELECT * FROM pending_approvals ORDER BY createdAt DESC")
    suspend fun getAll(): List<PendingApprovalEntity>

    @Query("SELECT COUNT(*) FROM pending_approvals WHERE isReconciled = 0")
    suspend fun countPending(): Int

    @Query("SELECT COUNT(*) FROM pending_approvals")
    suspend fun count(): Int

    @Query("UPDATE pending_approvals SET walletApprovedAt = :approvedAt WHERE transactionId = :transactionId")
    suspend fun updateWalletApproval(transactionId: String, approvedAt: Long): Int

    @Query("UPDATE pending_approvals SET signatureRetrievedAt = :retrievedAt WHERE transactionId = :transactionId")
    suspend fun updateSignatureRetrieval(transactionId: String, retrievedAt: Long): Int

    @Query("UPDATE pending_approvals SET isReconciled = 1, reconciliationStatus = :status WHERE transactionId = :transactionId")
    suspend fun markReconciled(transactionId: String, status: String): Int

    @Query("DELETE FROM pending_approvals WHERE transactionId = :transactionId")
    suspend fun deleteByTransactionId(transactionId: String): Int

    @Query("DELETE FROM pending_approvals WHERE createdAt < :olderThanTime AND isReconciled = 1")
    suspend fun deleteReconciledOlderThan(olderThanTime: Long): Int

    @Query("DELETE FROM pending_approvals WHERE createdAt < :olderThanTime")
    suspend fun deleteOlderThan(olderThanTime: Long): Int

    @Query("SELECT * FROM pending_approvals WHERE isReconciled = 0 AND createdAt < :expiredTime")
    suspend fun getExpiredPending(expiredTime: Long): List<PendingApprovalEntity>
}
