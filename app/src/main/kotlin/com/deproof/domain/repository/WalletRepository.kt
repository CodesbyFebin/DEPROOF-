package com.deproof.domain.repository

import java.math.BigDecimal

data class WalletAccount(
    val publicKey: String,
    val name: String,
    val icon: String? = null
)

data class SignTransactionResult(
    val signature: String,
    val confirmed: Boolean
)

interface WalletRepository {
    suspend fun connect(): Result<WalletAccount>
    suspend fun disconnect(): Result<Unit>
    suspend fun isConnected(): Boolean
    suspend fun getAccount(): Result<WalletAccount>
    suspend fun signTransaction(instruction: String): Result<SignTransactionResult>
    suspend fun signMessage(message: String): Result<String>
    suspend fun getAvailableWallets(): Result<List<String>>
}
