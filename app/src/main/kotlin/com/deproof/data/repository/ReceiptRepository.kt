package com.deproof.data.repository

import android.util.Log
import androidx.annotation.NonNull
import com.deproof.data.local.ReceiptDao
import com.deproof.data.local.ReceiptEntity
import com.deproof.data.local.toDomain
import com.deproof.data.local.toEntity
import com.deproof.domain.exception.DomainException
import com.deproof.domain.model.Receipt
import com.deproof.domain.model.Result
import com.google.gson.Gson
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

private const val TAG = "ReceiptRepository"

class ReceiptRepository(@NonNull private val dao: ReceiptDao) {

    suspend fun insertReceipt(@NonNull receipt: Receipt): @NonNull Result<String> = withContext(Dispatchers.IO) {
        try {
            dao.insert(receipt.toEntity())
            Log.d(TAG, "Receipt inserted: ${receipt.id}")
            Result.Success(receipt.id)
        } catch (e: Exception) {
            Log.e(TAG, "Error inserting receipt: ${e.message}", e)
            Result.Error(DomainException.StorageError("Failed to insert receipt", e))
        }
    }

    suspend fun getReceiptById(@NonNull id: String): @NonNull Result<Receipt> = withContext(Dispatchers.IO) {
        try {
            val entity = dao.getById(id)
            if (entity != null) {
                Log.d(TAG, "Receipt retrieved: $id")
                Result.Success(entity.toDomain())
            } else {
                Log.w(TAG, "Receipt not found: $id")
                Result.Error(DomainException.NotFoundError("Receipt not found: $id"))
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error retrieving receipt $id: ${e.message}", e)
            Result.Error(DomainException.StorageError("Failed to retrieve receipt", e))
        }
    }

    suspend fun getAllReceipts(): @NonNull Result<List<Receipt>> = withContext(Dispatchers.IO) {
        try {
            val receipts = dao.getAllReceipts().map { it.toDomain() }
            Log.d(TAG, "Retrieved ${receipts.size} receipts")
            Result.Success(receipts)
        } catch (e: Exception) {
            Log.e(TAG, "Error retrieving all receipts: ${e.message}", e)
            Result.Error(DomainException.StorageError("Failed to retrieve receipts", e))
        }
    }

    suspend fun getRecentReceipts(limit: Int = 10): @NonNull Result<List<Receipt>> = withContext(Dispatchers.IO) {
        try {
            val receipts = dao.getRecentReceipts(limit).map { it.toDomain() }
            Log.d(TAG, "Retrieved $limit recent receipts")
            Result.Success(receipts)
        } catch (e: Exception) {
            Log.e(TAG, "Error retrieving recent receipts: ${e.message}", e)
            Result.Error(DomainException.StorageError("Failed to retrieve recent receipts", e))
        }
    }

    suspend fun updateReceipt(@NonNull receipt: Receipt): @NonNull Result<Unit> = withContext(Dispatchers.IO) {
        try {
            dao.update(receipt.toEntity())
            Log.d(TAG, "Receipt updated: ${receipt.id}")
            Result.Success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "Error updating receipt ${receipt.id}: ${e.message}", e)
            Result.Error(DomainException.StorageError("Failed to update receipt", e))
        }
    }

    suspend fun deleteReceipt(@NonNull receipt: Receipt): @NonNull Result<Unit> = withContext(Dispatchers.IO) {
        try {
            dao.delete(receipt.toEntity())
            Log.d(TAG, "Receipt deleted: ${receipt.id}")
            Result.Success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "Error deleting receipt ${receipt.id}: ${e.message}", e)
            Result.Error(DomainException.StorageError("Failed to delete receipt", e))
        }
    }

    suspend fun exportReceiptAsJson(@NonNull receipt: Receipt): @NonNull Result<String> = withContext(Dispatchers.IO) {
        try {
            val json = Gson().toJson(receipt)
            Log.d(TAG, "Receipt exported as JSON: ${receipt.id}")
            Result.Success(json)
        } catch (e: Exception) {
            Log.e(TAG, "Error exporting receipt as JSON: ${e.message}", e)
            Result.Error(DomainException.ParseError("Failed to export receipt as JSON", e))
        }
    }

    suspend fun getReceiptCount(): @NonNull Result<Int> = withContext(Dispatchers.IO) {
        try {
            val count = dao.getReceiptCount()
            Log.d(TAG, "Receipt count: $count")
            Result.Success(count)
        } catch (e: Exception) {
            Log.e(TAG, "Error getting receipt count: ${e.message}", e)
            Result.Error(DomainException.StorageError("Failed to get receipt count", e))
        }
    }
}
