package com.aistudio.deproof.sdwk.domain

import androidx.room.Dao
import androidx.room.Database
import androidx.room.Entity
import androidx.room.Insert
import androidx.room.PrimaryKey
import androidx.room.Query
import androidx.room.RoomDatabase
import androidx.room.TypeConverter
import androidx.room.TypeConverters
import com.google.gson.Gson
import java.util.concurrent.TimeUnit

@Entity(tableName = "receipts")
data class ReceiptEntity(
    @PrimaryKey val id: String = "",
    val signature: String?,
    val verdict: String,
    val summary: String,
    val messageHash: String = "",
    val canonicalMessage: String = "",
    val timestamp: Long = System.currentTimeMillis(),
    val evidence: String = "",
    val status: String = "recorded"
)

@Dao
interface ReceiptDao {
    @Insert
    suspend fun insert(receipt: ReceiptEntity)

    @Query("SELECT * FROM receipts ORDER BY timestamp DESC")
    suspend fun getAll(): List<ReceiptEntity>

    @Query("SELECT * FROM receipts WHERE id = :id")
    suspend fun getById(id: String): ReceiptEntity?

    @Query("DELETE FROM receipts")
    suspend fun deleteAll()

    @Query("SELECT COUNT(*) FROM receipts")
    suspend fun count(): Int
}

@Database(entities = [ReceiptEntity::class], version = 1, exportSchema = false)
@TypeConverters
abstract class AppDatabase : RoomDatabase() {
    abstract fun receiptDao(): ReceiptDao

    companion object {
        private var instance: AppDatabase? = null

        fun getInstance(context: android.content.Context): AppDatabase {
            if (instance == null) {
                synchronized(AppDatabase::class) {
                    if (instance == null) {
                        instance = androidx.room.Room.databaseBuilder(
                            context,
                            AppDatabase::class.java,
                            "deproof-db"
                        ).build()
                    }
                }
            }
            return instance!!
        }
    }
}

class ReceiptExporter {
    companion object {
        fun exportReceipt(receipt: ReceiptEntity): String {
            val json = Gson().toJson(receipt)
            return json
        }

        fun createReceipt(
            signature: String?,
            verdict: String,
            summary: String,
            messageHash: String = "",
            canonicalMessage: String = ""
        ): ReceiptEntity {
            val id = java.util.UUID.randomUUID().toString()
            return ReceiptEntity(
                id = id,
                signature = signature,
                verdict = verdict,
                summary = summary,
                messageHash = messageHash,
                canonicalMessage = canonicalMessage,
                timestamp = System.currentTimeMillis()
            )
        }
    }
}

fun cooldownRemaining(startMs: Long, nowMs: Long = System.currentTimeMillis()): String {
    val elapsed = nowMs - startMs
    val cooldownMs = TimeUnit.HOURS.toMillis(24)
    if (elapsed >= cooldownMs) {
        return "Withdraw ready"
    }
    val remaining = cooldownMs - elapsed
    val hours = TimeUnit.MILLISECONDS.toHours(remaining)
    return "$hours hours remaining"
}
