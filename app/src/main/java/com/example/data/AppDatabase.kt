package com.example.data

import android.content.Context

abstract class AppDatabase {
    abstract fun receiptDao(): ReceiptDao

    companion object {
        @Volatile
        private var instance: AppDatabase? = null

        fun getInstance(context: Context): AppDatabase {
            return instance ?: synchronized(this) {
                instance ?: MockAppDatabase().also { instance = it }
            }
        }
    }
}

class MockAppDatabase : AppDatabase() {
    private val dao = MockReceiptDao()
    override fun receiptDao(): ReceiptDao = dao
}
