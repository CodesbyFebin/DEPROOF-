package com.aistudio.deproof.sdwk.chain

import com.google.gson.JsonObject
import com.squareup.okhttp3.OkHttpClient
import com.squareup.okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.Body
import retrofit2.http.POST

interface RpcService {
    @POST("/")
    suspend fun call(@Body request: JsonObject): String
}

class RpcClient(private val cluster: String = "https://api.mainnet-beta.solana.com") {
    private val retrofit: Retrofit
    private val service: RpcService

    init {
        val httpClient = OkHttpClient.Builder()
            .addInterceptor(HttpLoggingInterceptor().apply {
                level = HttpLoggingInterceptor.Level.BASIC
            })
            .build()

        retrofit = Retrofit.Builder()
            .baseUrl(cluster)
            .client(httpClient)
            .addConverterFactory(GsonConverterFactory.create())
            .build()

        service = retrofit.create(RpcService::class.java)
    }

    suspend fun getAccountInfo(pubkey: String): AccountSummary? {
        return try {
            val solBalance = getSolBalance(pubkey)
            val skrBalance = getSkrBalance(pubkey)
            val slot = getSlot()
            AccountSummary(solBalance, skrBalance, slot)
        } catch (e: Exception) {
            null
        }
    }

    private suspend fun getSolBalance(pubkey: String): Long {
        return try {
            val request = JsonObject().apply {
                addProperty("jsonrpc", "2.0")
                addProperty("method", "getBalance")
                addProperty("id", "1")
                add("params", com.google.gson.JsonArray().apply {
                    add(pubkey)
                })
            }
            val response = service.call(request)
            val parsed = com.google.gson.JsonParser.parseString(response).asJsonObject
            parsed.get("result")?.asLong ?: 0L
        } catch (e: Exception) {
            0L
        }
    }

    private suspend fun getSkrBalance(pubkey: String): Long {
        return try {
            val skrMint = "SKRbvo6Gf7GondiT3BbTfuRDPqLWei4j2Qy2NPGZhW3"
            val request = JsonObject().apply {
                addProperty("jsonrpc", "2.0")
                addProperty("method", "getTokenAccountsByOwner")
                addProperty("id", "1")
                add("params", com.google.gson.JsonArray().apply {
                    add(pubkey)
                    add(JsonObject().apply {
                        addProperty("mint", skrMint)
                    })
                    add(JsonObject().apply {
                        addProperty("encoding", "base64")
                    })
                })
            }
            val response = service.call(request)
            val parsed = com.google.gson.JsonParser.parseString(response).asJsonObject
            val value = parsed.get("result")?.asJsonObject?.get("value")?.asJsonArray
            var total = 0L
            value?.forEach { account ->
                try {
                    val accountObj = account.asJsonObject
                    val accountData = accountObj.get("account")?.asJsonObject
                    val data = accountData?.get("data")?.asJsonArray
                    if (data != null && data.size() >= 2) {
                        val encodedData = data[0].asString
                        val decodedBytes = android.util.Base64.decode(encodedData, android.util.Base64.DEFAULT)
                        if (decodedBytes.size >= 64) {
                            val amount = com.aistudio.deproof.sdwk.util.readU64LE(decodedBytes, 64)
                            total += amount
                        }
                    }
                } catch (e: Exception) {
                    // Skip on error
                }
            }
            total
        } catch (e: Exception) {
            0L
        }
    }

    private suspend fun getSlot(): Long {
        return try {
            val request = JsonObject().apply {
                addProperty("jsonrpc", "2.0")
                addProperty("method", "getSlot")
                addProperty("id", "1")
                add("params", com.google.gson.JsonArray())
            }
            val response = service.call(request)
            val parsed = com.google.gson.JsonParser.parseString(response).asJsonObject
            parsed.get("result")?.asLong ?: 0L
        } catch (e: Exception) {
            0L
        }
    }

    suspend fun getSignatures(pubkey: String, limit: Int = 10): List<SignatureInfo> {
        return try {
            val request = JsonObject().apply {
                addProperty("jsonrpc", "2.0")
                addProperty("method", "getSignaturesForAddress")
                addProperty("id", "1")
                add("params", com.google.gson.JsonArray().apply {
                    add(pubkey)
                    add(JsonObject().apply {
                        addProperty("limit", limit)
                    })
                })
            }
            val response = service.call(request)
            val parsed = com.google.gson.JsonParser.parseString(response).asJsonObject
            val resultArray = parsed.get("result")?.asJsonArray ?: return emptyList()

            resultArray.mapNotNull { sig ->
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
        } catch (e: Exception) {
            emptyList()
        }
    }

    suspend fun getTransaction(signature: String): TransactionResponse? {
        return try {
            val request = JsonObject().apply {
                addProperty("jsonrpc", "2.0")
                addProperty("method", "getTransaction")
                addProperty("id", "1")
                add("params", com.google.gson.JsonArray().apply {
                    add(signature)
                    add(JsonObject().apply {
                        addProperty("encoding", "json")
                        addProperty("maxSupportedTransactionVersion", 0)
                    })
                })
            }
            val response = service.call(request)
            val parsed = com.google.gson.JsonParser.parseString(response).asJsonObject
            val resultObj = parsed.get("result")?.asJsonObject ?: return null

            val slot = resultObj.get("slot").asLong
            val txObj = resultObj.get("transaction").asJsonObject
            val metaObj = resultObj.get("meta")?.asJsonObject

            val signatures = txObj.get("message").asJsonObject
                .get("header").asJsonObject
                .get("numRequiredSignatures")?.asInt ?: 0

            val signatureList = if (txObj.has("signatures")) {
                txObj.get("signatures").asJsonArray.map { it.asString }
            } else {
                listOf(signature)
            }

            val message = parseMessage(txObj.get("message").asJsonObject)

            TransactionResponse(
                slot = slot,
                transaction = Transaction(
                    signatures = signatureList,
                    message = message
                ),
                meta = metaObj?.let { parseMeta(it) }
            )
        } catch (e: Exception) {
            null
        }
    }

    private fun parseMessage(messageObj: com.google.gson.JsonObject): Message {
        val keys = messageObj.get("accountKeys").asJsonArray.map { it.asString }
        val instructions = messageObj.get("instructions").asJsonArray.mapNotNull { instr ->
            try {
                val instrObj = instr.asJsonObject
                Instruction(
                    programIdIndex = instrObj.get("programIdIndex").asInt,
                    accounts = instrObj.get("accounts").asJsonArray.map { it.asInt },
                    data = instrObj.get("data").asString
                )
            } catch (e: Exception) {
                null
            }
        }
        val recentBlockhash = messageObj.get("recentBlockhash")?.asString ?: ""

        return Message(keys, instructions, recentBlockhash)
    }

    private fun parseMeta(metaObj: com.google.gson.JsonObject): Meta {
        val status = if (metaObj.has("status")) {
            metaObj.get("status").asJsonObject.let {
                TransactionStatus(ok = it.get("Ok"))
            }
        } else {
            null
        }
        val blockTime = metaObj.get("blockTime")?.asLong
        return Meta(
            err = metaObj.get("err"),
            status = status,
            blockTime = blockTime
        )
    }

    suspend fun getSignatureStatuses(signatures: List<String>): List<SignatureStatus?> {
        return try {
            val request = JsonObject().apply {
                addProperty("jsonrpc", "2.0")
                addProperty("method", "getSignatureStatuses")
                addProperty("id", "1")
                add("params", com.google.gson.JsonArray().apply {
                    val sigs = com.google.gson.JsonArray()
                    signatures.forEach { sigs.add(it) }
                    add(sigs)
                    add(JsonObject().apply {
                        addProperty("searchTransactionHistory", true)
                    })
                })
            }
            val response = service.call(request)
            val parsed = com.google.gson.JsonParser.parseString(response).asJsonObject
            val resultObj = parsed.get("result")?.asJsonObject
            val statusArray = resultObj?.get("value")?.asJsonArray ?: return emptyList()

            statusArray.mapNotNull { status ->
                try {
                    if (status.isJsonNull) {
                        null
                    } else {
                        val obj = status.asJsonObject
                        SignatureStatus(
                            slot = obj.get("slot").asLong,
                            err = obj.get("err"),
                            confirmations = obj.get("confirmations")?.asLong,
                            status = if (obj.has("status")) {
                                obj.get("status").asJsonObject.let { mapOf<String, Any>() }
                            } else {
                                null
                            }
                        )
                    }
                } catch (e: Exception) {
                    null
                }
            }
        } catch (e: Exception) {
            emptyList()
        }
    }
}
