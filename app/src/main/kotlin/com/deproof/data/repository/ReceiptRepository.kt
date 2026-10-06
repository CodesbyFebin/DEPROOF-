package com.deproof.data.repository

import com.deproof.data.local.ReceiptDao
import com.deproof.data.local.ReceiptEntity
import com.deproof.data.local.toDomain
import com.deproof.data.local.toEntity
import com.deproof.domain.model.Receipt
import com.deproof.domain.model.Result
import com.google.gson.Gson
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class ReceiptRepository(private val dao: ReceiptDao) {

    suspend fun insertReceipt(receipt: Receipt): Result<String> = withContext(Dispatchers.IO) {
        try {
            dao.insert(receipt.toEntity())
            Result.Success(receipt.id)
        } catch (e: Exception) {
            Result.Error(e)
        }
    }

    suspend fun getReceiptById(id: String): Result<Receipt> = withContext(Dispatchers.IO) {
        try {
            val entity = dao.getById(id)
            if (entity != null) {
                Result.Success(entity.toDomain())
            } else {
                Result.Error(Exception("Receipt not found"))
            }
        } catch (e: Exception) {
            Result.Error(e)
        }
    }

    suspend fun getAllReceipts(): Result<List<Receipt>> = withContext(Dispatchers.IO) {
        try {
            val receipts = dao.getAllReceipts().map { it.toDomain() }
            Result.Success(receipts)
        } catch (e: Exception) {
            Result.Error(e)
        }
    }

    suspend fun getRecentReceipts(limit: Int = 10): Result<List<Receipt>> = withContext(Dispatchers.IO) {
        try {
            val receipts = dao.getRecentReceipts(limit).map { it.toDomain() }
            Result.Success(receipts)
        } catch (e: Exception) {
            Result.Error(e)
        }
    }

    suspend fun updateReceipt(receipt: Receipt): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            dao.update(receipt.toEntity())
            Result.Success(Unit)
        } catch (e: Exception) {
            Result.Error(e)
        }
    }

    suspend fun deleteReceipt(receipt: Receipt): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            dao.delete(receipt.toEntity())
            Result.Success(Unit)
        } catch (e: Exception) {
            Result.Error(e)
        }
    }

    suspend fun exportReceiptAsJson(receipt: Receipt): Result<String> = withContext(Dispatchers.IO) {
        try {
            val json = Gson().toJson(receipt)
            Result.Success(json)
        } catch (e: Exception) {
            Result.Error(e)
        }
    }

    suspend fun getReceiptCount(): Result<Int> = withContext(Dispatchers.IO) {
        try {
            val count = dao.getReceiptCount()
            Result.Success(count)
        } catch (e: Exception) {
            Result.Error(e)
        }
    }
}
