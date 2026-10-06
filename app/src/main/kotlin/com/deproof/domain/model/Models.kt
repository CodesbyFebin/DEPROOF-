package com.deproof.domain.model

import androidx.annotation.NonNull
import androidx.annotation.Nullable
import java.util.*

// ========== Core Domain Models ==========

sealed class Verdict {
    object Payable : Verdict()
    object DoNotSign : Verdict()
    data class Unknown(val reason: @NonNull String) : Verdict()
}

data class Transaction(
    @NonNull val signature: String,
    val blockTime: Long,
    val slot: Long,
    @NonNull val status: TransactionStatus,
    @NonNull val instructions: List<Instruction>,
    @NonNull val accountKeys: List<String>
)

enum class TransactionStatus {
    SUCCESS, FAILURE, PENDING
}

data class Instruction(
    @NonNull val programId: String,
    val discriminator: Byte,
    @NonNull val accounts: List<String>,
    @NonNull val data: ByteArray
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
    @NonNull val tokenProgramId: String,
    @NonNull val mint: String,
    @NonNull val source: String,
    @NonNull val destination: String,
    @NonNull val owner: String,
    val amount: Long,
    val decimals: Byte
)

data class Receipt(
    @NonNull val id: String = UUID.randomUUID().toString(),
    @NonNull val transactionHash: String,
    @NonNull val verdict: String,
    @NonNull val messageHash: String,
    val timestamp: Long = System.currentTimeMillis(),
    @NonNull val signatureStatus: SignatureStatus = SignatureStatus.PENDING,
    val chainSubmitted: Boolean = false,
    @NonNull val jsonData: String = ""
)

enum class SignatureStatus {
    PENDING, SIGNED, REJECTED, FAILED
}

data class Balance(
    @NonNull val symbol: String,
    val amount: Long,
    val decimals: Int,
    @NonNull val displayAmount: String
)

data class SignatureInfo(
    @NonNull val signature: String,
    val blockTime: Long,
    val slot: Long,
    @NonNull val status: TransactionStatus
)

data class WalletInfo(
    @NonNull val publicKey: String,
    val isConnected: Boolean,
    @NonNull val walletType: String = "Unknown"
)

// ========== UseCase Result Types ==========

sealed class Result<out T> {
    data class Success<T>(@NonNull val data: T) : Result<T>()
    data class Error(@NonNull val exception: Exception) : Result<Nothing>()
    object Loading : Result<Nothing>()
}

fun <T> Result<T>.getOrNull(): @Nullable T? = when (this) {
    is Result.Success -> data
    else -> null
}

fun <T> Result<T>.isSuccess(): Boolean = this is Result.Success

fun <T> Result<T>.isError(): Boolean = this is Result.Error
