package com.aistudio.deproof.sdwk.wallet

import android.app.Activity
import android.content.Context
import com.solanamobile.mwalib.MobileWalletAdapterClient
import com.solanamobile.mwalib.RpcCluster
import com.solanamobile.mwalib.SigningRequest
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

data class WalletSession(
    val address: String,
    val identityName: String = "Deproof",
    val identityUri: String = "https://deproof.example.com"
)

class WalletManager(private val context: Context) {
    private var session: WalletSession? = null
    private var mwaClient: MobileWalletAdapterClient? = null

    suspend fun connect(activity: Activity): String {
        return suspendCancellableCoroutine { continuation ->
            try {
                mwaClient = MobileWalletAdapterClient(
                    activity = activity,
                    identityName = "Deproof",
                    identityUri = "https://deproof.example.com"
                )

                mwaClient?.authorize(
                    onResult = { result ->
                        result.onSuccess { auth ->
                            val address = auth.publicKey
                            session = WalletSession(
                                address = address,
                                identityName = "Deproof"
                            )
                            continuation.resume(address)
                        }
                        result.onFailure { error ->
                            continuation.resumeWithException(error)
                        }
                    }
                )
            } catch (e: Exception) {
                continuation.resumeWithException(e)
            }
        }
    }

    suspend fun disconnect() {
        mwaClient?.deauthorize()
        session = null
    }

    fun getSession(): WalletSession? = session

    suspend fun signDevnetMemo(memo: String): String {
        val currentSession = session
            ?: throw IllegalStateException("Wallet not connected")

        return suspendCancellableCoroutine { continuation ->
            try {
                val txData = memo.toByteArray(Charsets.UTF_8)
                val signingRequest = SigningRequest(
                    payload = txData,
                    addresses = listOf(currentSession.address),
                    cluster = RpcCluster.Devnet
                )

                mwaClient?.sign(
                    request = signingRequest,
                    onResult = { result ->
                        result.onSuccess { signatures ->
                            val sig = signatures.firstOrNull()
                                ?: throw Exception("No signature returned")
                            continuation.resume(sig)
                        }
                        result.onFailure { error ->
                            continuation.resumeWithException(error)
                        }
                    }
                )
            } catch (e: Exception) {
                continuation.resumeWithException(e)
            }
        }
    }

    suspend fun signTransaction(
        txData: ByteArray,
        cluster: String = "mainnet-beta"
    ): String {
        val currentSession = session
            ?: throw IllegalStateException("Wallet not connected")

        return suspendCancellableCoroutine { continuation ->
            try {
                val rpcCluster = when (cluster) {
                    "devnet" -> RpcCluster.Devnet
                    "testnet" -> RpcCluster.Testnet
                    else -> RpcCluster.Mainnet
                }

                val signingRequest = SigningRequest(
                    payload = txData,
                    addresses = listOf(currentSession.address),
                    cluster = rpcCluster
                )

                mwaClient?.sign(
                    request = signingRequest,
                    onResult = { result ->
                        result.onSuccess { signatures ->
                            val sig = signatures.firstOrNull()
                                ?: throw Exception("No signature returned")
                            continuation.resume(sig)
                        }
                        result.onFailure { error ->
                            continuation.resumeWithException(error)
                        }
                    }
                )
            } catch (e: Exception) {
                continuation.resumeWithException(e)
            }
        }
    }
}

class KeystoreSignatureResult(
    val signature: String,
    val algorithm: String = "ECDSA"
)

suspend fun signHash(
    hash: String,
    context: Context
): KeystoreSignatureResult {
    return suspendCancellableCoroutine { continuation ->
        try {
            val keyStore = java.security.KeyStore.getInstance("AndroidKeyStore")
            keyStore.load(null)

            val keyExists = keyStore.containsAlias("deproof_signing_key")
            if (!keyExists) {
                val keyGen = javax.crypto.KeyGenerator.getInstance(
                    "AES",
                    "AndroidKeyStore"
                )
                val spec = android.security.keystore.KeyGenParameterSpec.Builder(
                    "deproof_signing_key",
                    android.security.keystore.KeyProperties.PURPOSE_SIGN
                ).setDigests(android.security.keystore.KeyProperties.DIGEST_SHA256)
                    .build()
                keyGen.init(spec)
                keyGen.generateKey()
            }

            val key = keyStore.getKey("deproof_signing_key", null)
            val sig = javax.crypto.Mac.getInstance("HmacSHA256")
            sig.init(key as javax.crypto.SecretKey)
            val signatureBytes = sig.doFinal(hash.toByteArray(Charsets.UTF_8))
            val signatureHex = signatureBytes.joinToString("") { "%02x".format(it) }

            continuation.resume(KeystoreSignatureResult(signatureHex))
        } catch (e: Exception) {
            continuation.resumeWithException(e)
        }
    }
}
