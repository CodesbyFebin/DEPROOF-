package com.deproof.data.wallet

import com.deproof.domain.repository.SignTransactionResult
import com.deproof.domain.repository.WalletAccount
import com.deproof.domain.repository.WalletRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class SolanaWalletRepository(
    private val mwaClient: MobileWalletAdapterClient
) : WalletRepository {

    override suspend fun connect(): Result<WalletAccount> = withContext(Dispatchers.IO) {
        try {
            mwaClient.connect()
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun disconnect(): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            mwaClient.disconnect()
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun isConnected(): Boolean = withContext(Dispatchers.IO) {
        mwaClient.isConnected()
    }

    override suspend fun getAccount(): Result<WalletAccount> = withContext(Dispatchers.IO) {
        try {
            mwaClient.getAccount()
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun signTransaction(instruction: String): Result<SignTransactionResult> = withContext(Dispatchers.IO) {
        try {
            mwaClient.signTransaction(instruction)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun signMessage(message: String): Result<String> = withContext(Dispatchers.IO) {
        try {
            mwaClient.signMessage(message)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun getAvailableWallets(): Result<List<String>> = withContext(Dispatchers.IO) {
        try {
            mwaClient.getAvailableWallets()
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
