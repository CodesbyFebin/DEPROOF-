package com.deproof.data.solana

import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.net.URL
import java.io.OutputStreamWriter

data class RpcRequest(
    val jsonrpc: String = "2.0",
    val id: Int = 1,
    val method: String,
    val params: List<Any?> = emptyList()
)

data class RpcResponse<T>(
    val result: T?,
    val error: RpcError?,
    val id: Int
)

data class RpcError(
    val code: Int,
    val message: String,
    val data: String? = null
)

data class AccountInfo(
    val lamports: Long,
    val owner: String,
    val executable: Boolean,
    val rentEpoch: Long
)

data class LatestBlockhash(
    val blockhash: String,
    val lastValidBlockHeight: Long
)

class SolanaRpcClient(
    private val endpoint: String = "https://api.mainnet-beta.solana.com"
) {
    private companion object {
        const val TAG = "SolanaRpcClient"
    }

    suspend fun getLatestBlockhash(): LatestBlockhash? = withContext(Dispatchers.IO) {
        return@withContext try {
            val request = RpcRequest(
                method = "getLatestBlockhash",
                params = listOf(mapOf("commitment" to "finalized"))
            )
            val response = executeRpc<Map<String, Any>>(request)
            val value = response.result?.get("value") as? Map<String, Any>
            if (value != null) {
                LatestBlockhash(
                    blockhash = value["blockhash"] as? String ?: "",
                    lastValidBlockHeight = (value["lastValidBlockHeight"] as? Number)?.toLong() ?: 0L
                )
            } else null
        } catch (e: Exception) {
            Log.e(TAG, "Failed to get latest blockhash: ${e.message}", e)
            null
        }
    }

    suspend fun getBalance(pubkey: String): Long? = withContext(Dispatchers.IO) {
        return@withContext try {
            val request = RpcRequest(
                method = "getBalance",
                params = listOf(pubkey, mapOf("commitment" to "finalized"))
            )
            val response = executeRpc<Number>(request)
            response.result?.toLong()
        } catch (e: Exception) {
            Log.e(TAG, "Failed to get balance for $pubkey: ${e.message}", e)
            null
        }
    }

    suspend fun getAccountInfo(pubkey: String): AccountInfo? = withContext(Dispatchers.IO) {
        return@withContext try {
            val request = RpcRequest(
                method = "getAccountInfo",
                params = listOf(pubkey, mapOf("encoding" to "jsonParsed", "commitment" to "finalized"))
            )
            val response = executeRpc<Map<String, Any>>(request)
            val value = response.result?.get("value") as? Map<String, Any>
            if (value != null) {
                AccountInfo(
                    lamports = (value["lamports"] as? Number)?.toLong() ?: 0L,
                    owner = value["owner"] as? String ?: "",
                    executable = value["executable"] as? Boolean ?: false,
                    rentEpoch = (value["rentEpoch"] as? Number)?.toLong() ?: 0L
                )
            } else null
        } catch (e: Exception) {
            Log.e(TAG, "Failed to get account info for $pubkey: ${e.message}", e)
            null
        }
    }

    suspend fun sendTransaction(transaction: String): String? = withContext(Dispatchers.IO) {
        return@withContext try {
            val request = RpcRequest(
                method = "sendTransaction",
                params = listOf(
                    transaction,
                    mapOf(
                        "encoding" to "base64",
                        "skipPreflight" to false,
                        "preflightCommitment" to "confirmed"
                    )
                )
            )
            val response = executeRpc<String>(request)
            response.result
        } catch (e: Exception) {
            Log.e(TAG, "Failed to send transaction: ${e.message}", e)
            null
        }
    }

    suspend fun getSignatureStatus(signature: String): Map<String, Any>? = withContext(Dispatchers.IO) {
        return@withContext try {
            val request = RpcRequest(
                method = "getSignatureStatuses",
                params = listOf(
                    listOf(signature),
                    mapOf("searchTransactionHistory" to true)
                )
            )
            val response = executeRpc<Map<String, Any>>(request)
            val values = response.result?.get("value") as? List<Any>
            values?.firstOrNull() as? Map<String, Any>
        } catch (e: Exception) {
            Log.e(TAG, "Failed to get signature status for $signature: ${e.message}", e)
            null
        }
    }

    private suspend fun <T> executeRpc(request: RpcRequest): RpcResponse<T> {
        return withContext(Dispatchers.IO) {
            val url = URL(endpoint)
            val connection = url.openConnection()
            connection.doOutput = true
            connection.setRequestProperty("Content-Type", "application/json")

            val requestJson = buildJsonRequest(request)
            Log.d(TAG, "RPC Request: $requestJson")

            OutputStreamWriter(connection.outputStream).use { writer ->
                writer.write(requestJson)
                writer.flush()
            }

            val statusCode = (connection as java.net.HttpURLConnection).responseCode
            val inputStream = if (statusCode == 200) {
                connection.inputStream
            } else {
                connection.errorStream
            }

            val responseText = inputStream.bufferedReader().use { it.readText() }
            Log.d(TAG, "RPC Response: $responseText")

            parseRpcResponse(responseText)
        }
    }

    private fun buildJsonRequest(request: RpcRequest): String {
        val json = JSONObject()
        json.put("jsonrpc", request.jsonrpc)
        json.put("id", request.id)
        json.put("method", request.method)

        val paramsArray = org.json.JSONArray()
        request.params.forEach { param ->
            when (param) {
                is String -> paramsArray.put(param)
                is Number -> paramsArray.put(param)
                is Map<*, *> -> paramsArray.put(JSONObject(param as Map<String, Any>))
                is List<*> -> paramsArray.put(org.json.JSONArray(param))
                null -> paramsArray.put(JSONObject.NULL)
                else -> paramsArray.put(param.toString())
            }
        }
        json.put("params", paramsArray)

        return json.toString()
    }

    @Suppress("UNCHECKED_CAST")
    private fun <T> parseRpcResponse(response: String): RpcResponse<T> {
        val json = JSONObject(response)

        val result = if (json.has("result") && !json.isNull("result")) {
            when (json.get("result")) {
                is JSONObject -> json.getJSONObject("result").toMap() as T
                is org.json.JSONArray -> json.getJSONArray("result").toList() as T
                is String -> json.getString("result") as T
                is Number -> json.get("result") as T
                else -> json.get("result") as T
            }
        } else null

        val error = if (json.has("error") && !json.isNull("error")) {
            val errorJson = json.getJSONObject("error")
            RpcError(
                code = errorJson.getInt("code"),
                message = errorJson.getString("message"),
                data = errorJson.optString("data")
            )
        } else null

        val id = json.optInt("id", 1)

        return RpcResponse(result, error, id)
    }
}

private fun JSONObject.toMap(): Map<String, Any> {
    val map = mutableMapOf<String, Any>()
    val keys = this.keys()
    while (keys.hasNext()) {
        val key = keys.next()
        val value = this.get(key)
        map[key] = when (value) {
            is JSONObject -> value.toMap()
            is org.json.JSONArray -> value.toList()
            else -> value
        }
    }
    return map
}

private fun org.json.JSONArray.toList(): List<Any> {
    val list = mutableListOf<Any>()
    for (i in 0 until this.length()) {
        val value = this.get(i)
        list.add(when (value) {
            is JSONObject -> value.toMap()
            is org.json.JSONArray -> value.toList()
            else -> value
        })
    }
    return list
}
