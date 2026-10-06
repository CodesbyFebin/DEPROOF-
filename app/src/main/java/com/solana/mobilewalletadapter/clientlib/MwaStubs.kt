// Build-time stubs for Solana Mobile Wallet Adapter.
// Wallet features require physical hardware (MWA-compatible wallet app).
// Replace with the real dependency when targeting a physical device release.
@file:Suppress("UNUSED_PARAMETER", "unused")

package com.solana.mobilewalletadapter.clientlib

import android.net.Uri
import androidx.activity.ComponentActivity

class ActivityResultSender(@Suppress("unused") activity: ComponentActivity)

data class ConnectionIdentity(val uri: Uri, val iconUri: Uri, val identityName: String)

object Solana {
    val Devnet: Any = "solana:devnet"
}

data class Account(val publicKey: ByteArray, val chains: Set<String>? = null) {
    override fun equals(other: Any?) = other is Account && publicKey.contentEquals(other.publicKey)
    override fun hashCode() = publicKey.contentHashCode()
}

data class Capabilities(val supportedOptionalFeatures: Set<String> = emptySet())

class SignedTransactions(val signedPayloads: Array<ByteArray>? = null)

data class AuthorizationResult(val accounts: List<Account> = emptyList())

abstract class MobileWalletAdapterClient {
    abstract suspend fun getCapabilities(): Capabilities
    abstract suspend fun signTransactions(transactions: Array<ByteArray>): SignedTransactions
}

sealed class TransactionResult<out T> {
    data class Success<T>(val successPayload: T?) : TransactionResult<T>()
    object NoWalletFound : TransactionResult<Nothing>()
    class Failure : TransactionResult<Nothing>()
}

class MobileWalletAdapter(private val identity: ConnectionIdentity) {
    var blockchain: Any = Solana.Devnet
    var authToken: String? = null

    suspend fun <T> transact(
        sender: ActivityResultSender,
        block: suspend MobileWalletAdapterClient.(AuthorizationResult) -> T
    ): TransactionResult<T> = throw UnsupportedOperationException("MWA_UNAVAILABLE: wallet requires physical hardware")

    suspend fun disconnect(sender: ActivityResultSender): TransactionResult<Unit> =
        throw UnsupportedOperationException("MWA_UNAVAILABLE: wallet requires physical hardware")
}
