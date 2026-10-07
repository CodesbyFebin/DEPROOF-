package com.deproof.data.wallet

import android.content.Context
import android.content.Intent
import android.net.Uri
import com.deproof.domain.repository.SignTransactionResult
import com.deproof.domain.repository.WalletAccount
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import java.util.UUID
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

@Serializable
data class MWARequest(
    val method: String,
    val params: Map<String, String> = emptyMap(),
    val id: String = UUID.randomUUID().toString()
)

@Serializable
data class MWAResponse(
    val result: Map<String, String>? = null,
    val error: String? = null
)

class MobileWalletAdapterClient(private val context: Context? = null) {
    private var connected = false
    private var selectedAccount: WalletAccount? = null
    private var sessionToken: String? = null
    private var selectedWallet: String = "com.phantom"

    companion object {
        private const val MWA_PROTOCOL_VERSION = "1.0.0"
        private val SUPPORTED_WALLETS = listOf(
            "com.phantom",
            "com.solflare.mobile",
            "com.ledger.live",
            "com.coinbase.wallet"
        )
    }

    suspend fun connect(walletPackage: String = "com.phantom"): Result<WalletAccount> {
        return try {
            selectedWallet = walletPackage
            val account = authorizeWallet(walletPackage)
            selectedAccount = account
            connected = true
            sessionToken = UUID.randomUUID().toString()
            Result.success(account)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun disconnect(): Result<Unit> {
        return try {
            if (connected) {
                val request = MWARequest(
                    method = "wallet_revoke_session",
                    params = mapOf("sessionToken" to (sessionToken ?: ""))
                )
                invokeWallet(selectedWallet, request)
            }
            connected = false
            selectedAccount = null
            sessionToken = null
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    fun isConnected(): Boolean = connected && selectedAccount != null

    suspend fun getAccount(): Result<WalletAccount> {
        return if (isConnected() && selectedAccount != null) {
            Result.success(selectedAccount!!)
        } else {
            Result.failure(IllegalStateException("Wallet not connected"))
        }
    }

    suspend fun signTransaction(transactionData: String): Result<SignTransactionResult> {
        return try {
            if (!isConnected()) {
                return Result.failure(IllegalStateException("Wallet not connected"))
            }

            val request = MWARequest(
                method = "wallet_signTransaction",
                params = mapOf(
                    "transaction" to transactionData,
                    "sessionToken" to (sessionToken ?: "")
                )
            )

            val response = invokeWallet(selectedWallet, request)
            val signature = response["signature"] ?: return Result.failure(Exception("No signature returned"))

            Result.success(
                SignTransactionResult(
                    signature = signature,
                    confirmed = false
                )
            )
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun signAndSendTransaction(transactionData: String): Result<String> {
        return try {
            if (!isConnected()) {
                return Result.failure(IllegalStateException("Wallet not connected"))
            }

            val request = MWARequest(
                method = "wallet_signAndSendTransaction",
                params = mapOf(
                    "transaction" to transactionData,
                    "sessionToken" to (sessionToken ?: "")
                )
            )

            val response = invokeWallet(selectedWallet, request)
            val signature = response["signature"] ?: return Result.failure(Exception("Transaction failed"))

            Result.success(signature)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun signMessage(message: String): Result<String> {
        return try {
            if (!isConnected()) {
                return Result.failure(IllegalStateException("Wallet not connected"))
            }

            val request = MWARequest(
                method = "wallet_signMessage",
                params = mapOf(
                    "message" to message,
                    "sessionToken" to (sessionToken ?: "")
                )
            )

            val response = invokeWallet(selectedWallet, request)
            val signature = response["signature"] ?: return Result.failure(Exception("Sign failed"))

            Result.success(signature)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun getAvailableWallets(): Result<List<String>> {
        return try {
            val installedWallets = SUPPORTED_WALLETS.filter { isWalletInstalled(it) }
            Result.success(installedWallets.ifEmpty { listOf("com.phantom") })
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    fun getSessionToken(): String? = sessionToken

    fun hasPermissions(permissions: List<String>): Boolean = connected

    // Private helper methods

    private suspend fun authorizeWallet(walletPackage: String): WalletAccount {
        val request = MWARequest(
            method = "wallet_authorize",
            params = mapOf(
                "cluster" to "mainnet-beta",
                "appName" to "Deproof",
                "appIcon" to "https://deproof.dev/assets/depr.svg"
            )
        )

        val response = invokeWallet(walletPackage, request)
        return WalletAccount(
            publicKey = response["publicKey"] ?: throw Exception("No public key"),
            name = response["walletName"] ?: "Connected Wallet",
            icon = response["walletIcon"] ?: ""
        )
    }

    private suspend fun invokeWallet(walletPackage: String, request: MWARequest): Map<String, String> {
        return suspendCancellableCoroutine { continuation ->
            try {
                val requestJson = Json.encodeToString(MWARequest.serializer(), request)
                val encodedRequest = Uri.encode(requestJson)

                val intent = Intent("com.solanobile.ACTION_SIGN_MESSAGE").apply {
                    `package` = walletPackage
                    putExtra("request", encodedRequest)
                }

                if (context != null) {
                    // Production path: MWA wallet callback integration requires a physical device
                    // with an installed MWA-compatible wallet app (Phantom, Solflare, etc.).
                    // The Intent-based round-trip is not implemented in this build.
                    // No fabricated success is returned here.
                    continuation.resumeWithException(
                        UnsupportedOperationException(
                            "MWA_WALLET_CALLBACK_NOT_IMPLEMENTED: " +
                            "Wallet interaction requires a physical device with an MWA-compatible wallet app. " +
                            "Use Wallet.kt (com.solana.mobilewalletadapter.clientlib) for production wallet flows."
                        )
                    )
                } else {
                    // Null-context path used in unit tests — always fails to prevent test false-positives.
                    continuation.resumeWithException(
                        UnsupportedOperationException(
                            "MWA_NO_CONTEXT: invokeWallet called without Android Context. " +
                            "Physical device and Context required for wallet operations."
                        )
                    )
                }
            } catch (e: Exception) {
                continuation.resumeWithException(e)
            }
        }
    }

    private fun isWalletInstalled(walletPackage: String): Boolean {
        return context != null && try {
            context.packageManager.getPackageInfo(walletPackage, 0)
            true
        } catch (e: Exception) {
            false
        }
    }
}
