package com.deproof.data.repository

import android.util.Log
import com.deproof.data.database.dao.CustodyDecisionDao
import com.deproof.data.database.entity.CustodyDecisionEntity
import com.deproof.domain.model.CustodyDecision
import com.deproof.domain.model.CustodyDecisionStatus
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class CustodyDecisionRepository(
    private val dao: CustodyDecisionDao
) {
    companion object {
        const val TAG = "CustodyDecisionRepository"
    }

    suspend fun save(decision: CustodyDecision): Result<Unit> {
        return try {
            val entity = CustodyDecisionEntity.fromDomain(decision)
            dao.insert(entity)
            Log.d(TAG, "Saved custody decision: ${decision.id}")
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to save custody decision: ${e.message}", e)
            Result.failure(e)
        }
    }

    suspend fun update(decision: CustodyDecision): Result<Unit> {
        return try {
            val entity = CustodyDecisionEntity.fromDomain(decision)
            val updated = dao.update(entity)
            if (updated == 0) {
                return Result.failure(Exception("Decision not found: ${decision.id}"))
            }
            Log.d(TAG, "Updated custody decision: ${decision.id}")
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to update custody decision: ${e.message}", e)
            Result.failure(e)
        }
    }

    suspend fun getById(id: String): Result<CustodyDecision?> {
        return try {
            val entity = dao.getById(id)
            Result.success(entity?.toDomain())
        } catch (e: Exception) {
            Log.e(TAG, "Failed to get custody decision: ${e.message}", e)
            Result.failure(e)
        }
    }

    suspend fun getByTransactionId(transactionId: String): Result<CustodyDecision?> {
        return try {
            val entity = dao.getByTransactionId(transactionId)
            Result.success(entity?.toDomain())
        } catch (e: Exception) {
            Log.e(TAG, "Failed to get custody decision by transaction ID: ${e.message}", e)
            Result.failure(e)
        }
    }

    suspend fun getByStatus(status: CustodyDecisionStatus): Result<List<CustodyDecision>> {
        return try {
            val entities = dao.getByStatus(status)
            Result.success(entities.map { it.toDomain() })
        } catch (e: Exception) {
            Log.e(TAG, "Failed to get custody decisions by status: ${e.message}", e)
            Result.failure(e)
        }
    }

    suspend fun getAll(limit: Int = 100, offset: Int = 0): Result<List<CustodyDecision>> {
        return try {
            val entities = dao.getAll(limit, offset)
            Result.success(entities.map { it.toDomain() })
        } catch (e: Exception) {
            Log.e(TAG, "Failed to get all custody decisions: ${e.message}", e)
            Result.failure(e)
        }
    }

    fun observeByStatus(status: CustodyDecisionStatus): Flow<List<CustodyDecision>> {
        return dao.observeByStatus(status).map { entities ->
            entities.map { it.toDomain() }
        }
    }

    fun observeAll(): Flow<List<CustodyDecision>> {
        return dao.observeAll().map { entities ->
            entities.map { it.toDomain() }
        }
    }

    suspend fun updateStatus(id: String, status: CustodyDecisionStatus): Result<Unit> {
        return try {
            val updated = dao.updateStatus(id, status)
            if (updated == 0) {
                return Result.failure(Exception("Decision not found: $id"))
            }
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to update status: ${e.message}", e)
            Result.failure(e)
        }
    }

    suspend fun updatePolicyValidation(id: String, validated: Boolean): Result<Unit> {
        return try {
            val validatedAt = if (validated) System.currentTimeMillis() else 0
            val updated = dao.updatePolicyValidation(id, validated, validatedAt)
            if (updated == 0) {
                return Result.failure(Exception("Decision not found: $id"))
            }
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to update policy validation: ${e.message}", e)
            Result.failure(e)
        }
    }

    suspend fun updateSignatureVerification(id: String, verified: Boolean, hash: String): Result<Unit> {
        return try {
            val verifiedAt = if (verified) System.currentTimeMillis() else 0
            val updated = dao.updateSignatureVerification(id, verified, verifiedAt, hash)
            if (updated == 0) {
                return Result.failure(Exception("Decision not found: $id"))
            }
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to update signature verification: ${e.message}", e)
            Result.failure(e)
        }
    }

    suspend fun updateWalletApproval(id: String, status: String): Result<Unit> {
        return try {
            val approvalTime = System.currentTimeMillis()
            val updated = dao.updateWalletApproval(id, approvalTime, status)
            if (updated == 0) {
                return Result.failure(Exception("Decision not found: $id"))
            }
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to update wallet approval: ${e.message}", e)
            Result.failure(e)
        }
    }

    suspend fun updateError(id: String, error: String): Result<Unit> {
        return try {
            val updated = dao.updateError(id, error)
            if (updated == 0) {
                return Result.failure(Exception("Decision not found: $id"))
            }
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to update error: ${e.message}", e)
            Result.failure(e)
        }
    }

    suspend fun deleteUnconfirmedOlderThan(ageMs: Long): Result<Int> {
        return try {
            val cutoffTime = System.currentTimeMillis() - ageMs
            val deleted = dao.deleteUnconfirmedOlderThan(cutoffTime)
            Log.d(TAG, "Deleted $deleted unconfirmed custody decisions older than $ageMs ms")
            Result.success(deleted)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to delete old custody decisions: ${e.message}", e)
            Result.failure(e)
        }
    }
}
