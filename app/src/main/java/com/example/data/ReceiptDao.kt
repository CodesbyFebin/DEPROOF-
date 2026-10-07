package com.example.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query

@Dao
interface ReceiptDao {
    @Insert
    suspend fun insert(receipt: Receipt): Long

    @Query("SELECT * FROM receipts ORDER BY timestamp DESC LIMIT 10")
    suspend fun getRecentReceipts(): List<Receipt>

    @Query("SELECT * FROM receipts WHERE id = :id")
    suspend fun getReceiptById(id: Int): Receipt?

    @Query("SELECT * FROM receipts ORDER BY timestamp DESC")
    suspend fun getAllReceipts(): List<Receipt>

    @Query("SELECT COUNT(*) FROM receipts")
    suspend fun getReceiptCount(): Int
}
