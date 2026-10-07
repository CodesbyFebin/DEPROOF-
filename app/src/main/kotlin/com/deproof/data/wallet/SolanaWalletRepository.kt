package com.deproof.data.wallet

import android.content.Context
import com.deproof.domain.repository.SignTransactionResult
import com.deproof.domain.repository.WalletAccount
import com.deproof.domain.repository.WalletRepository

/**
 * SolanaWalletRepository — concrete implementation of WalletRepository.
 *
 * Delegates to MobileWalletAdapterClient. Wallet operations require a
 * physical device with an installed MWA-compatible wallet app; operations
 * will fail with UnsupportedOperationException on devices without one.
 *
 * AUTHORIZATION=CONSTRUCTION_ONLY: No mainnet spending occurs through this class.
 * Built by CodesbyFebin.
 */
class SolanaWalletRepository(context: Context? = null) : WalletRepository {

    private val client = MobileWalletAdapterClient(context)

    override suspend fun connect(): Result<WalletAccount> = client.connect()

    override suspend fun disconnect(): Result<Unit> = client.disconnect()

    override suspend fun isConnected(): Boolean = client.isConnected()

    override suspend fun getAccount(): Result<WalletAccount> = client.getAccount()

    override suspend fun signTransaction(instruction: String): Result<SignTransactionResult> =
        client.signTransaction(instruction)

    override suspend fun signMessage(message: String): Result<String> = client.signMessage(message)

    override suspend fun getAvailableWallets(): Result<List<String>> = client.getAvailableWallets()
}
