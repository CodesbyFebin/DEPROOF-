package com.deproof.domain.model

import java.util.*

// ========== Core Domain Models ==========

sealed class Verdict {
    object Payable : Verdict()
    object DoNotSign : Verdict()
    data class Unknown(val reason: String) : Verdict()
}

data class Transaction(
    val signature: String,
    val blockTime: Long,
    val slot: Long,
    val status: TransactionStatus,
    val instructions: List<Instruction>,
    val accountKeys: List<String>
)

enum class TransactionStatus {
    SUCCESS, FAILURE, PENDING
}

data class Instruction(
    val programId: String,
    val discriminator: Byte,
    val accounts: List<String>,
    val data: ByteArray
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is Instruction) return false
        return programId == other.programId && discriminator == other.discriminator &&
                accounts == other.accounts && data.contentEquals(other.data)
    }

    override fun hashCode(): Int {
        var result = programId.hashCode()
        result = 31 * result + discriminator
        result = 31 * result + accounts.hashCode()
        result = 31 * result + data.contentHashCode()
        return result
    }
}

data class TransferCheckedInstruction(
    val tokenProgramId: String,
    val mint: String,
    val source: String,
    val destination: String,
    val owner: String,
    val amount: Long,
    val decimals: Byte
)

data class ReviewBinding(
    val transactionHash: String,
    val messageText: String,
    val messageHash: String,
    val verdict: Verdict,
    val timestamp: Long = System.currentTimeMillis()
)

data class Receipt(
    val id: String = UUID.randomUUID().toString(),
    val transactionHash: String,
    val verdict: String,
    val messageHash: String,
    val timestamp: Long = System.currentTimeMillis(),
    val signatureStatus: SignatureStatus = SignatureStatus.PENDING,
    val chainSubmitted: Boolean = false,
    val jsonData: String = ""
)

enum class SignatureStatus {
    PENDING, SIGNED, REJECTED, FAILED
}

data class Balance(
    val symbol: String,
    val amount: Long,
    val decimals: Int,
    val displayAmount: String
)

data class SignatureInfo(
    val signature: String,
    val blockTime: Long,
    val slot: Long,
    val status: TransactionStatus
)

data class WalletInfo(
    val publicKey: String,
    val isConnected: Boolean,
    val walletType: String = "Unknown"
)

// ========== UseCase Result Types ==========

sealed class Result<out T> {
    data class Success<T>(val data: T) : Result<T>()
    data class Error(val exception: Exception) : Result<Nothing>()
    object Loading : Result<Nothing>()
}

fun <T> Result<T>.getOrNull(): T? = when (this) {
    is Result.Success -> data
    else -> null
}

fun <T> Result<T>.isSuccess(): Boolean = this is Result.Success

fun <T> Result<T>.isError(): Boolean = this is Result.Error
