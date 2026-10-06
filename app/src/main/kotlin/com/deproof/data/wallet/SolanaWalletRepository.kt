// Placeholder — superseded by com.example.wallet.Wallet (the real MWA implementation).
// Every method delegates to MobileWalletAdapterClient which throws UOE; this class
// cannot silently succeed in production.  For test doubles use MockWalletRepository.
@file:Suppress("unused")
package com.deproof.data.wallet

import com.deproof.domain.repository.SignTransactionResult
import com.deproof.domain.repository.WalletAccount
import com.deproof.domain.repository.WalletRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class SolanaWalletRepository(
    private val mwaClient: MobileWalletAdapterClient
) : WalletRepository {
    override suspend fun connect(): Result<WalletAccount> = withContext(Dispatchers.IO) { mwaClient.connect() }
    override suspend fun disconnect(): Result<Unit> = withContext(Dispatchers.IO) { mwaClient.disconnect() }
    override suspend fun isConnected(): Boolean = withContext(Dispatchers.IO) { mwaClient.isConnected() }
    override suspend fun getAccount(): Result<WalletAccount> = withContext(Dispatchers.IO) { mwaClient.getAccount() }
    override suspend fun signTransaction(instruction: String): Result<SignTransactionResult> = withContext(Dispatchers.IO) { mwaClient.signTransaction(instruction) }
    override suspend fun signMessage(message: String): Result<String> = withContext(Dispatchers.IO) { mwaClient.signMessage(message) }
    override suspend fun getAvailableWallets(): Result<List<String>> = withContext(Dispatchers.IO) { mwaClient.getAvailableWallets() }
}
