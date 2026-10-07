package com.example.chain

import com.google.gson.JsonArray
import com.google.gson.JsonObject
import com.google.gson.JsonParser
import com.squareup.okhttp3.MediaType.Companion.toMediaType
import com.squareup.okhttp3.OkHttpClient
import com.squareup.okhttp3.Request
import com.squareup.okhttp3.RequestBody.Companion.toRequestBody
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.concurrent.TimeUnit

class RpcClient(private val clusterUrl: String = "https://api.mainnet-beta.solana.com") {
    private val httpClient = OkHttpClient.Builder()
        .connectTimeout(10, TimeUnit.SECONDS)
        .readTimeout(10, TimeUnit.SECONDS)
        .writeTimeout(10, TimeUnit.SECONDS)
        .build()

    private var callId = 1

    suspend fun getAccountInfo(pubkey: String): Result<AccountSummary> = withContext(Dispatchers.IO) {
        try {
            val solBalance = getSolBalance(pubkey).getOrNull() ?: 0L
            val skrBalance = getSkrBalance(pubkey).getOrNull() ?: 0L
            val slot = getSlot().getOrNull() ?: 0L

            Result.success(AccountSummary(solBalance, skrBalance, slot))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    private suspend fun getSolBalance(pubkey: String): Result<Long> = withContext(Dispatchers.IO) {
        try {
            val request = JsonObject().apply {
                addProperty("jsonrpc", "2.0")
                addProperty("method", "getBalance")
                addProperty("id", callId++)
                add("params", JsonArray().apply { add(pubkey) })
            }
            val response = call(request)
            val parsed = JsonParser.parseString(response).asJsonObject
            val result = parsed.get("result")?.asLong ?: 0L
            Result.success(result)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    private suspend fun getSkrBalance(pubkey: String): Result<Long> = withContext(Dispatchers.IO) {
        try {
            val skrMint = "SKRbvo6Gf7GondiT3BbTfuRDPqLWei4j2Qy2NPGZhW3"
            val request = JsonObject().apply {
                addProperty("jsonrpc", "2.0")
                addProperty("method", "getTokenAccountsByOwner")
                addProperty("id", callId++)
                add("params", JsonArray().apply {
                    add(pubkey)
                    add(JsonObject().apply { addProperty("mint", skrMint) })
                    add(JsonObject().apply { addProperty("encoding", "jsonParsed") })
                })
            }
            val response = call(request)
            val parsed = JsonParser.parseString(response).asJsonObject
            val value = parsed.get("result")?.asJsonObject?.get("value")?.asJsonArray

            var total = 0L
            value?.forEach { account ->
                try {
                    val info = account.asJsonObject.get("account")?.asJsonObject
                        ?.get("data")?.asJsonObject?.get("parsed")?.asJsonObject
                        ?.get("info")?.asJsonObject
                    val amount = info?.get("tokenAmount")?.asJsonObject?.get("amount")?.asString?.toLong() ?: 0L
                    total += amount
                } catch (e: Exception) {
                    // Skip malformed accounts
                }
            }
            Result.success(total)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    private suspend fun getSlot(): Result<Long> = withContext(Dispatchers.IO) {
        try {
            val request = JsonObject().apply {
                addProperty("jsonrpc", "2.0")
                addProperty("method", "getSlot")
                addProperty("id", callId++)
                add("params", JsonArray())
            }
            val response = call(request)
            val parsed = JsonParser.parseString(response).asJsonObject
            val slot = parsed.get("result")?.asLong ?: 0L
            Result.success(slot)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun getSignatures(pubkey: String, limit: Int = 10): Result<List<SignatureInfo>> =
        withContext(Dispatchers.IO) {
            try {
                val request = JsonObject().apply {
                    addProperty("jsonrpc", "2.0")
                    addProperty("method", "getSignaturesForAddress")
                    addProperty("id", callId++)
                    add("params", JsonArray().apply {
                        add(pubkey)
                        add(JsonObject().apply { addProperty("limit", limit) })
                    })
                }
                val response = call(request)
                val parsed = JsonParser.parseString(response).asJsonObject
                val resultArray = parsed.get("result")?.asJsonArray ?: JsonArray()

                val sigs = resultArray.mapNotNull { sig ->
                    try {
                        val obj = sig.asJsonObject
                        SignatureInfo(
                            signature = obj.get("signature").asString,
                            slot = obj.get("slot").asLong,
                            err = obj.get("err"),
                            memo = obj.get("memo")?.asString,
                            blockTime = obj.get("blockTime")?.asLong
                        )
                    } catch (e: Exception) {
                        null
                    }
                }
                Result.success(sigs)
            } catch (e: Exception) {
                Result.failure(e)
            }
        }

    private suspend fun call(request: JsonObject): String = withContext(Dispatchers.IO) {
        val body = request.toString().toRequestBody("application/json".toMediaType())
        val httpRequest = Request.Builder()
            .url(clusterUrl)
            .post(body)
            .build()

        val response = httpClient.newCall(httpRequest).execute()
        response.body?.string() ?: "{}"
    }
}
