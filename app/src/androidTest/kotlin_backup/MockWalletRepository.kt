package com.deproof.data.wallet

import com.deproof.domain.repository.SignTransactionResult
import com.deproof.domain.repository.WalletAccount
import com.deproof.domain.repository.WalletRepository
import java.util.UUID

class MockWalletRepository : WalletRepository {
    var simulateConnectionFailure = false
    var simulateSigningFailure = false
    var simulateWalletNotInstalled = false

    var mockAccount = WalletAccount(
        publicKey = "9B5X6wq4xCSUQyRjqW37hSrwq3CEQmD2KwMaKNoon5w4",
        name = "Mock Wallet"
    )

    private var connected = false

    override suspend fun connect(): Result<WalletAccount> {
        return when {
            simulateConnectionFailure -> Result.failure(RuntimeException("Connection failed"))
            simulateWalletNotInstalled -> Result.failure(RuntimeException("Wallet not installed"))
            else -> {
                connected = true
                Result.success(mockAccount)
            }
        }
    }

    override suspend fun disconnect(): Result<Unit> {
        return try {
            connected = false
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun isConnected(): Boolean = connected

    override suspend fun getAccount(): Result<WalletAccount> {
        return if (connected) {
            Result.success(mockAccount)
        } else {
            Result.failure(IllegalStateException("Wallet not connected"))
        }
    }

    override suspend fun signTransaction(instruction: String): Result<SignTransactionResult> {
        return when {
            !connected -> Result.failure(IllegalStateException("Wallet not connected"))
            simulateSigningFailure -> Result.failure(RuntimeException("Signing failed"))
            else -> Result.success(
                SignTransactionResult(
                    signature = "sig_${UUID.randomUUID()}",
                    confirmed = false
                )
            )
        }
    }

    override suspend fun signMessage(message: String): Result<String> {
        return when {
            !connected -> Result.failure(IllegalStateException("Wallet not connected"))
            simulateSigningFailure -> Result.failure(RuntimeException("Signing failed"))
            else -> Result.success("sig_${UUID.randomUUID()}")
        }
    }

    override suspend fun getAvailableWallets(): Result<List<String>> {
        return if (simulateWalletNotInstalled) {
            Result.success(emptyList())
        } else {
            Result.success(listOf("com.phantom", "com.solflare"))
        }
    }
}
