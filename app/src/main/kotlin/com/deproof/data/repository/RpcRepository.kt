package com.deproof.data.repository

import androidx.annotation.NonNull
import com.deproof.data.local.*
import com.deproof.domain.exception.DomainException
import com.deproof.domain.exception.toDomainException
import com.deproof.domain.model.Balance
import com.deproof.domain.model.Result
import com.deproof.domain.model.SignatureInfo
import com.deproof.domain.model.TransactionStatus
import com.deproof.domain.util.RetryConfig
import com.deproof.domain.util.isNetworkError
import com.deproof.domain.util.retryWithExponentialBackoff
import com.google.gson.Gson
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import timber.log.Timber
import java.util.concurrent.TimeUnit
private const val NETWORK_TIMEOUT_SECONDS = 30L
private const val RETRY_ATTEMPTS = 3

class RpcRepository(
    @NonNull private val rpcUrl: String = "https://api.mainnet-beta.solana.com",
    @NonNull private val tokenMint: String = "SKRbvo6Gf7GoNcKKqqyckfjxN2PEVEqJf3rUKdPbdYu"
) {
    private val client = OkHttpClient.Builder()
        .connectTimeout(NETWORK_TIMEOUT_SECONDS, TimeUnit.SECONDS)
        .readTimeout(NETWORK_TIMEOUT_SECONDS, TimeUnit.SECONDS)
        .writeTimeout(NETWORK_TIMEOUT_SECONDS, TimeUnit.SECONDS)
        .build()
    private val gson = Gson()
    private val retryConfig = RetryConfig(maxRetries = RETRY_ATTEMPTS)

    suspend fun getBalance(@NonNull pubkey: String): @NonNull Result<Balance> = withContext(Dispatchers.IO) {
        try {
            val balance = retryWithExponentialBackoff(
                config = retryConfig,
                shouldRetry = { isNetworkError(it) }
            ) {
                val request = buildJsonRpcRequest(
                    "getBalance",
                    listOf(pubkey)
                )
                val response = executeRpc(request)

                try {
                    val balanceResp = gson.fromJson(response, BalanceResponse::class.java)
                    val lamports = balanceResp.result.value
                    val sol = lamports / 1_000_000_000.0

                    Balance(
                        symbol = "SOL",
                        amount = lamports,
                        decimals = 9,
                        displayAmount = String.format("%.9f", sol)
                    )
                } catch (e: Exception) {
                    Timber.e(e, "Failed to parse balance response")
                    throw DomainException.ParseError("Invalid balance response format", e)
                }
            }
            Result.Success(balance)
        } catch (e: Exception) {
            val domainEx = if (e is DomainException) e else e.toDomainException()
            Timber.e(e, "Error fetching balance for $pubkey: ${e.message}")
            Result.Error(domainEx)
        }
    }

    suspend fun getTokenBalance(@NonNull pubkey: String): @NonNull Result<Balance> = withContext(Dispatchers.IO) {
        try {
            val balance = retryWithExponentialBackoff(
                config = retryConfig,
                shouldRetry = { isNetworkError(it) }
            ) {
                val request = buildJsonRpcRequest(
                    "getTokenAccountBalance",
                    listOf(pubkey)
                )
                val response = executeRpc(request)

                try {
                    val balanceResp = gson.fromJson(response, BalanceResponse::class.java)
                    val amount = balanceResp.result.value

                    Balance(
                        symbol = "SKR",
                        amount = amount,
                        decimals = 4,
                        displayAmount = String.format("%.4f", amount / 10000.0)
                    )
                } catch (e: Exception) {
                    Timber.e(e, "Failed to parse token balance response")
                    throw DomainException.ParseError("Invalid token balance response format", e)
                }
            }
            Result.Success(balance)
        } catch (e: Exception) {
            val domainEx = if (e is DomainException) e else e.toDomainException()
            Timber.e(e, "Error fetching token balance for $pubkey: ${e.message}")
            Result.Error(domainEx)
        }
    }

    suspend fun getSignaturesForAddress(
        @NonNull pubkey: String,
        limit: Int = 10
    ): @NonNull Result<List<SignatureInfo>> = withContext(Dispatchers.IO) {
        try {
            val signatures = retryWithExponentialBackoff(
                config = retryConfig,
                shouldRetry = { isNetworkError(it) }
            ) {
                val request = buildJsonRpcRequest(
                    "getSignaturesForAddress",
                    listOf(pubkey, mapOf("limit" to limit))
                )
                val response = executeRpc(request)

                try {
                    val sigsResp = gson.fromJson(response, SignaturesResponse::class.java)
                    sigsResp.result.map { record ->
                        SignatureInfo(
                            signature = record.signature,
                            blockTime = record.blockTime ?: 0L,
                            slot = record.slot,
                            status = if (record.err == null) TransactionStatus.SUCCESS else TransactionStatus.FAILURE
                        )
                    }
                } catch (e: Exception) {
                    Timber.e(e, "Failed to parse signatures response")
                    throw DomainException.ParseError("Invalid signatures response format", e)
                }
            }
            Result.Success(signatures)
        } catch (e: Exception) {
            val domainEx = if (e is DomainException) e else e.toDomainException()
            Timber.e(e, "Error fetching signatures for $pubkey: ${e.message}")
            Result.Error(domainEx)
        }
    }

    suspend fun getTransaction(@NonNull signature: String): @NonNull Result<String> = withContext(Dispatchers.IO) {
        try {
            val response = retryWithExponentialBackoff(
                config = retryConfig,
                shouldRetry = { isNetworkError(it) }
            ) {
                val request = buildJsonRpcRequest(
                    "getTransaction",
                    listOf(signature, mapOf("encoding" to "json", "commitment" to "confirmed"))
                )
                executeRpc(request)
            }
            Result.Success(response)
        } catch (e: Exception) {
            val domainEx = if (e is DomainException) e else e.toDomainException()
            Timber.e(e, "Error fetching transaction $signature: ${e.message}")
            Result.Error(domainEx)
        }
    }

    private fun buildJsonRpcRequest(method: String, params: List<Any?>): String {
        val payload = mapOf(
            "jsonrpc" to "2.0",
            "id" to System.currentTimeMillis(),
            "method" to method,
            "params" to params
        )
        return gson.toJson(payload)
    }

    private fun executeRpc(jsonRequest: String): String {
        val requestBody = jsonRequest.toRequestBody("application/json".toMediaType())
        val request = Request.Builder()
            .url(rpcUrl)
            .post(requestBody)
            .addHeader("User-Agent", "Deproof/1.0.0")
            .build()

        try {
            client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) {
                    val errorMessage = response.body?.string() ?: "No error details"
                    Timber.w("RPC request failed with status ${response.code}: $errorMessage")
                    throw DomainException.NetworkError(
                        statusCode = response.code,
                        message = "RPC request failed: ${response.code}"
                    )
                }
                return response.body?.string() ?: throw DomainException.NetworkError(
                    message = "Empty response from RPC"
                )
            }
        } catch (e: java.util.concurrent.TimeoutException) {
            Timber.e(e, "RPC request timeout")
            throw DomainException.TimeoutError(cause = e)
        } catch (e: java.net.SocketTimeoutException) {
            Timber.e(e, "RPC socket timeout")
            throw DomainException.TimeoutError(cause = e)
        } catch (e: java.net.ConnectException) {
            Timber.e(e, "RPC connection failed")
            throw DomainException.NetworkError(message = "Connection failed", cause = e)
        } catch (e: java.net.UnknownHostException) {
            Timber.e(e, "RPC host unreachable")
            throw DomainException.NetworkError(message = "Host unreachable", cause = e)
        } catch (e: DomainException) {
            throw e
        } catch (e: Exception) {
            Timber.e(e, "Unexpected error during RPC call")
            throw DomainException.UnknownError(message = e.message ?: "Unknown RPC error", cause = e)
        }
    }
}
