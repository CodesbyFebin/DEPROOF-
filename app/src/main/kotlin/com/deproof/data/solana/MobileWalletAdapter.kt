package com.deproof.data.solana

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.util.Log
import com.deproof.domain.InstructionPolicy
import com.deproof.domain.PolicyValidator
import com.deproof.domain.SignatureVerifier
import com.deproof.domain.model.Instruction
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

data class WalletInfo(
    val name: String,
    val packageName: String,
    val icon: String? = null,
    val deeplink: String
)

data class WalletAuthorization(
    val publicKey: String,
    val authToken: String? = null,
    val walletName: String
)

data class SignedTransaction(
    val signature: String,
    val publicKey: String
)

class MobileWalletAdapter(private val context: Context) {
    companion object {
        const val TAG = "MobileWalletAdapter"
        const val SOLANA_WALLET_SCHEME = "solana-wallet"
        const val MAINNET = "https://api.mainnet-beta.solana.com"
        const val DEVNET = "https://api.devnet.solana.com"
    }

    private val policyValidator = PolicyValidator()
    private val signatureVerifier = SignatureVerifier()

    suspend fun discoverWallets(): List<WalletInfo> = withContext(Dispatchers.Default) {
        return@withContext try {
            val intent = Intent(Intent.ACTION_VIEW).apply {
                data = Uri.parse("$SOLANA_WALLET_SCHEME://discover")
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            }

            val packageManager = context.packageManager
            val resolveInfos = packageManager.queryIntentActivities(intent, 0)

            Log.d(TAG, "Discovered ${resolveInfos.size} Solana wallets")

            resolveInfos.mapNotNull { resolveInfo ->
                val packageName = resolveInfo.activityInfo.packageName
                val appName = resolveInfo.loadLabel(packageManager).toString()

                if (packageName.isNotEmpty()) {
                    WalletInfo(
                        name = appName,
                        packageName = packageName,
                        deeplink = "$SOLANA_WALLET_SCHEME://$packageName"
                    )
                } else {
                    null
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error discovering wallets: ${e.message}", e)
            emptyList()
        }
    }

    suspend fun connectWallet(
        wallet: WalletInfo,
        appName: String = "Deproof",
        appIcon: String? = null,
        network: String = MAINNET
    ): WalletAuthorization? = withContext(Dispatchers.Main) {
        return@withContext try {
            Log.d(TAG, "Connecting to wallet: ${wallet.name}")

            val params = mapOf(
                "app_name" to appName,
                "app_icon" to (appIcon ?: ""),
                "rpc_url" to network,
                "required_features" to listOf("signAndSendTransaction", "signTransaction"),
                "chain" to if (network == MAINNET) "mainnet-beta" else "devnet"
            )

            val deeplink = buildDeeplink(wallet.packageName, "connect", params)
            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(deeplink))
            intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP

            context.startActivity(intent)

            Log.d(TAG, "Wallet connection initiated for ${wallet.name}")
            null
        } catch (e: Exception) {
            Log.e(TAG, "Error connecting to wallet: ${e.message}", e)
            null
        }
    }

    suspend fun signTransaction(
        wallet: WalletInfo,
        transaction: String,
        publicKey: String
    ): SignedTransaction? = withContext(Dispatchers.Main) {
        return@withContext try {
            Log.d(TAG, "Requesting signature from ${wallet.name}")

            val params = mapOf(
                "transaction" to transaction,
                "public_key" to publicKey
            )

            val deeplink = buildDeeplink(wallet.packageName, "signTransaction", params)
            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(deeplink))
            intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP

            context.startActivity(intent)

            Log.d(TAG, "Transaction signature requested from ${wallet.name}")
            null
        } catch (e: Exception) {
            Log.e(TAG, "Error signing transaction: ${e.message}", e)
            null
        }
    }

    private fun buildDeeplink(
        packageName: String,
        method: String,
        params: Map<String, Any>
    ): String {
        val baseUri = "$SOLANA_WALLET_SCHEME://$packageName/$method"
        val queryParams = params.entries.joinToString("&") { (key, value) ->
            "$key=${encodeParam(value)}"
        }
        return if (queryParams.isNotEmpty()) "$baseUri?$queryParams" else baseUri
    }

    private fun encodeParam(value: Any): String {
        return when (value) {
            is String -> Uri.encode(value)
            is List<*> -> Uri.encode(value.joinToString(","))
            else -> Uri.encode(value.toString())
        }
    }

    // ========== Phase 3: Policy Validation & Signature Verification ==========

    suspend fun authorizeWithPolicy(
        policy: InstructionPolicy,
        instructions: List<Instruction>
    ): Result<Unit> = withContext(Dispatchers.Default) {
        try {
            // Validate each instruction against policy
            for (instruction in instructions) {
                val result = policyValidator.validateInstructionPolicy(instruction, policy)
                if (result.isFailure) {
                    Log.e(TAG, "Policy validation failed for instruction ${instruction.programId}")
                    return@withContext result
                }
            }

            // Validate no mixed instructions
            val mixedResult = policyValidator.validateMixedInstructions(instructions)
            if (mixedResult.isFailure) {
                Log.e(TAG, "Mixed instruction validation failed")
                return@withContext mixedResult
            }

            Log.d(TAG, "Policy authorization successful for ${instructions.size} instructions")
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "Policy authorization error: ${e.message}", e)
            Result.failure(e)
        }
    }

    suspend fun verifySignature(
        reviewedMessageBytes: ByteArray,
        signedTransaction: SignedTransaction
    ): Result<Boolean> = withContext(Dispatchers.Default) {
        try {
            val result = signatureVerifier.verify(reviewedMessageBytes, signedTransaction)

            if (result.isFailure) {
                Log.e(TAG, "Signature verification failed: ${result.exceptionOrNull()?.message}")
                return@withContext result.map { false }
            }

            val verificationResult = result.getOrNull()
            val isValid = verificationResult?.isValid ?: false

            if (!isValid && verificationResult != null) {
                Log.w(TAG, "Signature verification failed with issues: ${verificationResult.detectedIssues}")
            } else {
                Log.d(TAG, "Signature verified successfully")
            }

            Result.success(isValid)
        } catch (e: Exception) {
            Log.e(TAG, "Signature verification error: ${e.message}", e)
            Result.failure(e)
        }
    }

    suspend fun verifyTransactionNotModified(
        reviewedMessageBytes: ByteArray,
        currentTransactionBytes: ByteArray
    ): Result<Unit> = withContext(Dispatchers.Default) {
        try {
            val result = signatureVerifier.verifyTransactionNotModified(
                reviewedMessageBytes,
                currentTransactionBytes
            )

            if (result.isFailure) {
                Log.e(TAG, "Transaction modification detected: ${result.exceptionOrNull()?.message}")
            } else {
                Log.d(TAG, "Transaction integrity verified")
            }

            result
        } catch (e: Exception) {
            Log.e(TAG, "Transaction verification error: ${e.message}", e)
            Result.failure(e)
        }
    }
}
