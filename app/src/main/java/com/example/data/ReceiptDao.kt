package com.example.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query

@Dao
interface ReceiptDao {
    @Insert
    fun insert(receipt: Receipt): Long

    @Query("SELECT * FROM receipts ORDER BY timestamp DESC LIMIT 10")
    fun getRecentReceipts(): List<Receipt>

    @Query("SELECT * FROM receipts WHERE id = :id")
    fun getReceiptById(id: Int): Receipt?

    @Query("SELECT * FROM receipts ORDER BY timestamp DESC")
    fun getAllReceipts(): List<Receipt>

    @Query("SELECT COUNT(*) FROM receipts")
    fun getReceiptCount(): Int
}
