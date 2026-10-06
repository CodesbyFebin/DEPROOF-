package com.deproof.data.local

import androidx.room.*
import com.google.gson.Gson
import java.util.*

@Entity(tableName = "receipts")
data class ReceiptEntity(
    @PrimaryKey
    val id: String = UUID.randomUUID().toString(),

    @ColumnInfo(name = "transaction_hash")
    val transactionHash: String,

    @ColumnInfo(name = "verdict")
    val verdict: String,

    @ColumnInfo(name = "message_hash")
    val messageHash: String,

    @ColumnInfo(name = "timestamp")
    val timestamp: Long = System.currentTimeMillis(),

    @ColumnInfo(name = "signature_status")
    val signatureStatus: String = "PENDING",

    @ColumnInfo(name = "chain_submitted")
    val chainSubmitted: Boolean = false,

    @ColumnInfo(name = "json_data")
    val jsonData: String = ""
)

@Dao
interface ReceiptDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(receipt: ReceiptEntity)

    @Query("SELECT * FROM receipts WHERE id = :id")
    suspend fun getById(id: String): ReceiptEntity?

    @Query("SELECT * FROM receipts ORDER BY timestamp DESC")
    suspend fun getAllReceipts(): List<ReceiptEntity>

    @Query("SELECT * FROM receipts ORDER BY timestamp DESC LIMIT :limit")
    suspend fun getRecentReceipts(limit: Int = 10): List<ReceiptEntity>

    @Update
    suspend fun update(receipt: ReceiptEntity)

    @Delete
    suspend fun delete(receipt: ReceiptEntity)

    @Query("DELETE FROM receipts WHERE timestamp < :beforeTimestamp")
    suspend fun deleteOlderThan(beforeTimestamp: Long)

    @Query("SELECT COUNT(*) FROM receipts")
    suspend fun getReceiptCount(): Int
}

@Database(entities = [ReceiptEntity::class], version = 1, exportSchema = false)
abstract class DepRoofDatabase : RoomDatabase() {
    abstract fun receiptDao(): ReceiptDao

    companion object {
        const val DATABASE_NAME = "deproof_db"
    }
}

// ========== Converter Functions ==========

fun ReceiptEntity.toDomain(): com.deproof.domain.model.Receipt {
    return com.deproof.domain.model.Receipt(
        id = id,
        transactionHash = transactionHash,
        verdict = verdict,
        messageHash = messageHash,
        timestamp = timestamp,
        signatureStatus = com.deproof.domain.model.SignatureStatus.valueOf(signatureStatus),
        chainSubmitted = chainSubmitted,
        jsonData = jsonData
    )
}

fun com.deproof.domain.model.Receipt.toEntity(): ReceiptEntity {
    return ReceiptEntity(
        id = id,
        transactionHash = transactionHash,
        verdict = verdict,
        messageHash = messageHash,
        timestamp = timestamp,
        signatureStatus = signatureStatus.name,
        chainSubmitted = chainSubmitted,
        jsonData = jsonData
    )
}

// ========== Response Models for API ==========

data class BalanceResponse(
    val result: BalanceData
)

data class BalanceData(
    val value: Long,
    val context: Context
)

data class Context(
    val slot: Long
)

data class SignaturesResponse(
    val result: List<SignatureRecord>
)

data class SignatureRecord(
    val signature: String,
    val slot: Long,
    val blockTime: Long?,
    val confirmationStatus: String?,
    val err: String?
)

data class TransactionResponse(
    val result: TransactionData?
)

data class TransactionData(
    val transaction: TransactionContent,
    val meta: TransactionMeta
)

data class TransactionContent(
    val signatures: List<String>,
    val message: Message
)

data class Message(
    val accountKeys: List<String>,
    val instructions: List<InstructionData>,
    val recentBlockhash: String
)

data class InstructionData(
    val programIdIndex: Int,
    val accounts: List<Int>,
    val data: String
)

data class TransactionMeta(
    val status: Status?,
    val slot: Long
)

data class Status(
    val Ok: String?
)

// ========== JSON Serialization ==========

fun ReceiptEntity.toJson(): String = Gson().toJson(this)

fun String.fromJsonToReceipt(): ReceiptEntity? =
    try {
        Gson().fromJson(this, ReceiptEntity::class.java)
    } catch (e: Exception) {
        null
    }
