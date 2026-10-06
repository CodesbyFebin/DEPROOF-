package com.deproof.data.repository

import com.deproof.data.local.*
import com.deproof.domain.model.Balance
import com.deproof.domain.model.Result
import com.deproof.domain.model.SignatureInfo
import com.deproof.domain.model.TransactionStatus
import com.google.gson.Gson
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody

class RpcRepository(
    private val rpcUrl: String = "https://api.mainnet-beta.solana.com",
    private val tokenMint: String = "SKRbvo6Gf7GoNcKKqqyckfjxN2PEVEqJf3rUKdPbdYu"
) {
    private val client = OkHttpClient()
    private val gson = Gson()

    suspend fun getBalance(pubkey: String): Result<Balance> = withContext(Dispatchers.IO) {
        try {
            val request = buildJsonRpcRequest(
                "getBalance",
                listOf(pubkey)
            )
            val response = executeRpc(request)

            val balanceResp = gson.fromJson(response, BalanceResponse::class.java)
            val lamports = balanceResp.result.value
            val sol = lamports / 1_000_000_000.0

            Result.Success(
                Balance(
                    symbol = "SOL",
                    amount = lamports,
                    decimals = 9,
                    displayAmount = String.format("%.9f", sol)
                )
            )
        } catch (e: Exception) {
            Result.Error(e)
        }
    }

    suspend fun getTokenBalance(pubkey: String): Result<Balance> = withContext(Dispatchers.IO) {
        try {
            // Get token account balance - simplified implementation
            val request = buildJsonRpcRequest(
                "getTokenAccountBalance",
                listOf(pubkey)
            )
            val response = executeRpc(request)

            val balanceResp = gson.fromJson(response, BalanceResponse::class.java)
            val amount = balanceResp.result.value

            Result.Success(
                Balance(
                    symbol = "SKR",
                    amount = amount,
                    decimals = 4,
                    displayAmount = String.format("%.4f", amount / 10000.0)
                )
            )
        } catch (e: Exception) {
            Result.Error(e)
        }
    }

    suspend fun getSignaturesForAddress(
        pubkey: String,
        limit: Int = 10
    ): Result<List<SignatureInfo>> = withContext(Dispatchers.IO) {
        try {
            val request = buildJsonRpcRequest(
                "getSignaturesForAddress",
                listOf(pubkey, mapOf("limit" to limit))
            )
            val response = executeRpc(request)

            val sigsResp = gson.fromJson(response, SignaturesResponse::class.java)
            val signatures = sigsResp.result.map { record ->
                SignatureInfo(
                    signature = record.signature,
                    blockTime = record.blockTime ?: 0L,
                    slot = record.slot,
                    status = if (record.err == null) TransactionStatus.SUCCESS else TransactionStatus.FAILURE
                )
            }

            Result.Success(signatures)
        } catch (e: Exception) {
            Result.Error(e)
        }
    }

    suspend fun getTransaction(signature: String): Result<String> = withContext(Dispatchers.IO) {
        try {
            val request = buildJsonRpcRequest(
                "getTransaction",
                listOf(signature, mapOf("encoding" to "json", "commitment" to "confirmed"))
            )
            val response = executeRpc(request)
            Result.Success(response)
        } catch (e: Exception) {
            Result.Error(e)
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

        client.newCall(request).execute().use { response ->
            if (!response.isSuccessful) {
                throw Exception("RPC request failed: ${response.code}")
            }
            return response.body?.string() ?: throw Exception("Empty response")
        }
    }
}
