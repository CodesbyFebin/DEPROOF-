// SolanaRpcClientImpl.kt — Real Solana JSON-RPC client using OkHttp.
//
// Key design:
// - JSON-RPC 2.0 spec compliance
// - Exponential backoff retry on transient failures
// - Proper error distinction (network vs RPC vs parsing)
// - Result-based error handling
package com.deproof.data.rpc

import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.node.ArrayNode
import com.fasterxml.jackson.databind.node.JsonNodeFactory
import com.fasterxml.jackson.databind.node.ObjectNode
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import java.util.concurrent.TimeUnit

/**
 * Real Solana JSON-RPC client implementation.
 * Makes actual RPC calls to a Solana node via HTTPS.
 *
 * Key features:
 * - Exponential backoff retry (100ms, 200ms, 400ms, 800ms)
 * - Proper error distinction:
 *   * RpcError: RPC method error (e.g., account not found)
 *   * NetworkError: Connection/timeout issues (transient, retryable)
 *   * ParseError: Response parsing failed
 * - JSON-RPC 2.0 compliance
 */
class SolanaRpcClientImpl(
    private val endpoint: RpcEndpoint,
    private val httpClient: OkHttpClient? = null,
    private val mapper: ObjectMapper = ObjectMapper()
) {
    private val client = httpClient ?: createDefaultClient(endpoint)
    private val requestIdCounter = java.util.concurrent.atomic.AtomicLong(1)

    /**
     * Fetches account info for a given account address.
     * Used to read mint data and token account balances.
     *
     * Returns Result.success with the full account data or Result.failure with:
     * - RpcError: Account not found or other RPC error
     * - NetworkError: Connection/timeout
     * - ParseError: Response parsing failed
     */
    suspend fun getAccountInfo(account: String): Result<AccountInfo> =
        withContext(Dispatchers.IO) {
            callJsonRpc("getAccountInfo", listOf(
                account,
                ObjectNode(JsonNodeFactory.instance).apply {
                    put("encoding", "base64")
                }
            )).mapCatching { response ->
                parseAccountInfo(response as? ObjectNode ?: throw ParseError("Expected ObjectNode"))
            }
        }

    /**
     * Fetches all token accounts for a given owner.
     * Filters for a specific mint if provided.
     *
     * Used to enumerate all SKR token accounts (ATA + non-ATA).
     *
     * Returns Result.success with list of token accounts, or Result.failure with:
     * - RpcError: programId invalid or other RPC error
     * - NetworkError: Connection/timeout
     * - ParseError: Response parsing failed
     *
     * Note: Empty list is valid (owner has no SKR accounts).
     */
    suspend fun getTokenAccountsByOwner(
        owner: String,
        mint: String? = null
    ): Result<List<TokenAccountInfo>> =
        withContext(Dispatchers.IO) {
            val filter = if (mint != null) {
                ObjectNode(JsonNodeFactory.instance).apply {
                    put("mint", mint)
                }
            } else {
                ObjectNode(JsonNodeFactory.instance).apply {
                    put("programId", "TokenkegQfeZyiNwAJbNbGKPFXCWuBvf9Ss623VQ5DA")
                }
            }

            callJsonRpc("getTokenAccountsByOwner", listOf(
                owner,
                filter,
                ObjectNode(JsonNodeFactory.instance).apply {
                    put("encoding", "jsonParsed")
                }
            )).mapCatching { response ->
                parseTokenAccounts(response as? ObjectNode ?: throw ParseError("Expected ObjectNode"))
            }
        }

    /**
     * Gets signature statuses for transaction monitoring (Phase 3A Layer 4).
     */
    suspend fun getSignatureStatuses(signatures: List<String>): Result<List<SignatureStatus?>> =
        withContext(Dispatchers.IO) {
            val sigArray = ArrayNode(JsonNodeFactory.instance)
            signatures.forEach { sigArray.add(it) }

            callJsonRpc("getSignatureStatuses", listOf(sigArray)).mapCatching { response ->
                val arrayResponse = response as? ArrayNode ?: throw ParseError("Expected ArrayNode")
                arrayResponse.mapNotNull { status ->
                    if (status.isNull) null
                    else parseSignatureStatus(status as ObjectNode)
                }
            }
        }

    /**
     * Core JSON-RPC 2.0 call handler.
     * Implements exponential backoff retry on transient failures.
     *
     * Returns Result.success with parsed response or Result.failure with RpcError/NetworkError.
     */
    private suspend fun callJsonRpc(
        method: String,
        params: List<Any>
    ): Result<JsonNode> {
        val requestId = requestIdCounter.incrementAndGet()
        val paramsArray = ArrayNode(JsonNodeFactory.instance)
        params.forEach { param ->
            when (param) {
                is String -> paramsArray.add(param)
                is JsonNode -> paramsArray.add(param)
                else -> paramsArray.add(mapper.valueToTree(param))
            }
        }

        val requestBody = ObjectNode(JsonNodeFactory.instance).apply {
            put("jsonrpc", "2.0")
            put("method", method)
            set<JsonNode>("params", paramsArray)
            put("id", requestId)
        }

        var delay = 100L
        var lastError: Exception? = null

        repeat(4) { attempt ->
            if (attempt > 0) {
                delay(delay)
                delay *= 2
            }

            val result = executeRequest(requestBody.toString())
            if (result.isSuccess) return result
            lastError = result.exceptionOrNull() as? Exception

            // Don't retry on RPC errors (non-transient)
            if (lastError is RpcError) return Result.failure(lastError)
        }

        return Result.failure(lastError ?: RpcError("Unknown RPC error"))
    }

    /**
     * Executes an HTTP request with error handling.
     */
    private suspend fun executeRequest(
        body: String
    ): Result<JsonNode> = withContext(Dispatchers.IO) {
        try {
            val request = Request.Builder()
                .url(endpoint.url)
                .post(body.toRequestBody("application/json".toMediaType()))
                .build()

            val response = client.newCall(request).execute()

            if (!response.isSuccessful) {
                return@withContext Result.failure(
                    NetworkError("HTTP ${response.code}: ${response.message}")
                )
            }

            val responseBody = response.body?.string() ?: ""
            val jsonResponse = mapper.readTree(responseBody) as? ObjectNode
                ?: return@withContext Result.failure(ParseError("Invalid JSON response"))

            // Check for JSON-RPC error
            if (jsonResponse.has("error") && !jsonResponse["error"].isNull) {
                val error = jsonResponse["error"] as ObjectNode
                val code = error.get("code")?.asInt() ?: -1
                val message = error.get("message")?.asText() ?: "Unknown error"
                return@withContext Result.failure(RpcError(message, code))
            }

            if (!jsonResponse.has("result")) {
                return@withContext Result.failure(ParseError("No result in JSON-RPC response"))
            }

            Result.success(jsonResponse["result"])
        } catch (e: java.net.SocketTimeoutException) {
            Result.failure(NetworkError("Request timeout: ${e.message}"))
        } catch (e: java.net.ConnectException) {
            Result.failure(NetworkError("Connection failed: ${e.message}"))
        } catch (e: Exception) {
            Result.failure(NetworkError("Network error: ${e.message}"))
        }
    }

    /**
     * Parses AccountInfo from getAccountInfo response.
     * Extracts executable flag, lamports, owner, and data.
     */
    private fun parseAccountInfo(node: JsonNode): AccountInfo {
        val value = (node as? ObjectNode)?.get("value") as? ObjectNode
            ?: throw ParseError("Invalid account info response")

        return AccountInfo(
            address = "",  // Not included in response, set by caller
            executable = value.get("executable")?.asBoolean() ?: false,
            lamports = value.get("lamports")?.asLong() ?: 0L,
            owner = value.get("owner")?.asText() ?: "",
            data = value.get("data")?.let { data ->
                when {
                    data.isArray -> (data as ArrayNode).joinToString("") { it.asText() }
                    data.isTextual -> data.asText()
                    else -> ""
                }
            } ?: ""
        )
    }

    /**
     * Parses token accounts from getTokenAccountsByOwner response.
     * Extracts address, mint, owner, and amount.
     */
    private fun parseTokenAccounts(node: JsonNode): List<TokenAccountInfo> {
        val value = (node as? ObjectNode)?.get("value") as? ArrayNode
            ?: throw ParseError("Invalid token accounts response")

        return value.mapNotNull { item ->
            val accountNode = (item as? ObjectNode)?.get("account") as? ObjectNode
            val pubkeyNode = item.get("pubkey") as? ObjectNode

            val address = pubkeyNode?.asText() ?: item.get("pubkey")?.asText() ?: return@mapNotNull null
            val data = accountNode?.get("data") as? ObjectNode ?: return@mapNotNull null

            try {
                val parsed = data.get("parsed") as? ObjectNode ?: return@mapNotNull null
                val info = parsed.get("info") as? ObjectNode ?: return@mapNotNull null

                TokenAccountInfo(
                    address = address,
                    mint = info.get("mint")?.asText() ?: "",
                    owner = info.get("owner")?.asText() ?: "",
                    amount = info.get("tokenAmount")?.get("amount")?.asText() ?: "0",
                    decimals = info.get("tokenAmount")?.get("decimals")?.asInt() ?: 0
                )
            } catch (e: Exception) {
                null
            }
        }
    }

    /**
     * Parses signature status from getSignatureStatuses response.
     */
    private fun parseSignatureStatus(node: ObjectNode): SignatureStatus {
        return SignatureStatus(
            slot = node.get("slot")?.asLong() ?: 0L,
            confirmations = node.get("confirmations")?.asInt() ?: 0,
            err = node.get("err")?.isNull?.not()?.let {
                (node.get("err") as? ObjectNode)?.toString()
            },
            confirmationStatus = node.get("confirmationStatus")?.asText() ?: "processed"
        )
    }

    companion object {
        private fun createDefaultClient(endpoint: RpcEndpoint): OkHttpClient {
            return OkHttpClient.Builder()
                .callTimeout(endpoint.timeout.toMillis(), TimeUnit.MILLISECONDS)
                .connectTimeout(endpoint.timeout.toMillis(), TimeUnit.MILLISECONDS)
                .readTimeout(endpoint.timeout.toMillis(), TimeUnit.MILLISECONDS)
                .writeTimeout(endpoint.timeout.toMillis(), TimeUnit.MILLISECONDS)
                .build()
        }
    }
}

/**
 * Solana on-chain account information.
 */
data class AccountInfo(
    val address: String,
    val executable: Boolean,
    val lamports: Long,
    val owner: String,
    val data: String
)

/**
 * Token account parsed from getTokenAccountsByOwner.
 */
data class TokenAccountInfo(
    val address: String,
    val mint: String,
    val owner: String,
    val amount: String,
    val decimals: Int
)

/**
 * Transaction signature status for monitoring confirmations.
 */
data class SignatureStatus(
    val slot: Long,
    val confirmations: Int,
    val err: String? = null,
    val confirmationStatus: String
)

/**
 * RPC-specific error (method call failed).
 * Indicates the RPC method returned an error (non-transient).
 */
class RpcError(
    message: String,
    val code: Int = -1
) : Exception("RPC Error ($code): $message")

/**
 * Network-related error (connection, timeout).
 * Transient and retryable.
 */
class NetworkError(message: String) : Exception("Network Error: $message")

/**
 * Response parsing error.
 * Indicates the response couldn't be parsed as valid JSON-RPC.
 */
class ParseError(message: String) : Exception("Parse Error: $message")
