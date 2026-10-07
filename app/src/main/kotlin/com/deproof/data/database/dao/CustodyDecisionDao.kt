package com.deproof.data.database.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.deproof.data.database.entity.CustodyDecisionEntity
import com.deproof.domain.model.CustodyDecisionStatus
import kotlinx.coroutines.flow.Flow

@Dao
interface CustodyDecisionDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(decision: CustodyDecisionEntity): Long

    @Update
    suspend fun update(decision: CustodyDecisionEntity): Int

    @Delete
    suspend fun delete(decision: CustodyDecisionEntity): Int

    @Query("SELECT * FROM custody_decisions WHERE id = :id")
    suspend fun getById(id: String): CustodyDecisionEntity?

    @Query("SELECT * FROM custody_decisions WHERE transactionId = :transactionId")
    suspend fun getByTransactionId(transactionId: String): CustodyDecisionEntity?

    @Query("SELECT * FROM custody_decisions WHERE status = :status ORDER BY createdAt DESC")
    suspend fun getByStatus(status: CustodyDecisionStatus): List<CustodyDecisionEntity>

    @Query("SELECT * FROM custody_decisions ORDER BY createdAt DESC LIMIT :limit OFFSET :offset")
    suspend fun getAll(limit: Int = 100, offset: Int = 0): List<CustodyDecisionEntity>

    @Query("SELECT * FROM custody_decisions WHERE status = :status ORDER BY createdAt DESC")
    fun observeByStatus(status: CustodyDecisionStatus): Flow<List<CustodyDecisionEntity>>

    @Query("SELECT * FROM custody_decisions ORDER BY createdAt DESC")
    fun observeAll(): Flow<List<CustodyDecisionEntity>>

    @Query("SELECT COUNT(*) FROM custody_decisions")
    suspend fun count(): Int

    @Query("SELECT COUNT(*) FROM custody_decisions WHERE status = :status")
    suspend fun countByStatus(status: CustodyDecisionStatus): Int

    @Query("SELECT * FROM custody_decisions WHERE payer = :payer ORDER BY createdAt DESC LIMIT :limit")
    suspend fun getByPayer(payer: String, limit: Int = 50): List<CustodyDecisionEntity>

    @Query("SELECT * FROM custody_decisions WHERE createdAt >= :startTime AND createdAt <= :endTime ORDER BY createdAt DESC")
    suspend fun getByDateRange(startTime: Long, endTime: Long): List<CustodyDecisionEntity>

    @Query("DELETE FROM custody_decisions WHERE chainConfirmedAt = 0 AND createdAt < :olderThanTime")
    suspend fun deleteUnconfirmedOlderThan(olderThanTime: Long): Int

    @Query("UPDATE custody_decisions SET status = :status WHERE id = :id")
    suspend fun updateStatus(id: String, status: CustodyDecisionStatus): Int

    @Query("UPDATE custody_decisions SET policyValidated = :validated, policyValidatedAt = :validatedAt WHERE id = :id")
    suspend fun updatePolicyValidation(id: String, validated: Boolean, validatedAt: Long): Int

    @Query("UPDATE custody_decisions SET signatureVerified = :verified, signatureVerifiedAt = :verifiedAt, signatureHash = :hash WHERE id = :id")
    suspend fun updateSignatureVerification(id: String, verified: Boolean, verifiedAt: Long, hash: String): Int

    @Query("UPDATE custody_decisions SET walletApprovalTime = :approvalTime, walletApprovalStatus = :approvalStatus WHERE id = :id")
    suspend fun updateWalletApproval(id: String, approvalTime: Long, approvalStatus: String): Int

    @Query("UPDATE custody_decisions SET lastError = :error WHERE id = :id")
    suspend fun updateError(id: String, error: String): Int
}
