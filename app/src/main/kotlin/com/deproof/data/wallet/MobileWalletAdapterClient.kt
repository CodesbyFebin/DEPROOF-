// Placeholder — superseded by com.example.wallet.Wallet (the real MWA implementation).
// Every method throws so this class cannot silently succeed in production paths.
// For test doubles use MockWalletRepository in src/test.
@file:Suppress("unused")
package com.deproof.data.wallet

import com.deproof.domain.repository.SignTransactionResult
import com.deproof.domain.repository.WalletAccount

class MobileWalletAdapterClient {
    private val err get() = UnsupportedOperationException(
        "NOT_IMPLEMENTED: use com.example.wallet.Wallet (MWA) for production wallet access"
    )
    suspend fun connect(): Result<WalletAccount> = throw err
    suspend fun disconnect(): Result<Unit> = throw err
    fun isConnected(): Boolean = throw err
    suspend fun getAccount(): Result<WalletAccount> = throw err
    suspend fun signTransaction(transaction: String): Result<SignTransactionResult> = throw err
    suspend fun signAndSendTransaction(transaction: String): Result<String> = throw err
    suspend fun signMessage(message: String): Result<String> = throw err
    suspend fun getAvailableWallets(): Result<List<String>> = throw err
    fun getSessionToken(): String? = throw err
    fun hasPermissions(permissions: List<String>): Boolean = throw err
}
