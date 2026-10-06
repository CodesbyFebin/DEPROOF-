package com.example.data

import com.example.domain.*
import com.fasterxml.jackson.databind.JsonNode
import java.net.URI
import java.net.HttpURLConnection
import java.math.BigInteger
import java.time.Instant
import java.util.Base64
import java.util.concurrent.atomic.AtomicLong

class Rpc(val endpoint: String, val cluster: String, private val testTransport: ((String,List<Any?>) -> JsonNode)? = null) {
    private val sequence = AtomicLong()
    init { val u = URI(endpoint); ensure(u.scheme == "https" && u.host != null && u.userInfo == null && u.fragment == null,"BAD_RPC_URL"); ensure(cluster in listOf("devnet","mainnet-beta"),"CLUSTER_MISMATCH") }
    fun call(method: String, params: List<Any?> = emptyList()): JsonNode {
        testTransport?.let { return it(method,params) }
        val id = sequence.incrementAndGet(); val connection = URI(endpoint).toURL().openConnection() as HttpURLConnection
        try {
            connection.requestMethod = "POST"; connection.connectTimeout = 12000; connection.readTimeout = 15000; connection.doOutput = true; connection.instanceFollowRedirects = false
            connection.setRequestProperty("Content-Type","application/json")
            connection.outputStream.use { it.write(Json.mapper.writeValueAsBytes(mapOf("jsonrpc" to "2.0","id" to id,"method" to method,"params" to params))) }
            ensure(connection.responseCode in 200..299,"HTTP_${connection.responseCode}")
            val raw = connection.inputStream.use { input -> val out = java.io.ByteArrayOutputStream(); val buf = ByteArray(8192); var total = 0; while(true) { val n = input.read(buf); if(n < 0) break; total += n; ensure(total <= 2*1024*1024,"RPC_RESPONSE_TOO_LARGE"); out.write(buf,0,n) }; out.toString("UTF-8") }
            val response = Json.parse(raw); ensure(response["id"]?.asLong() == id && response["jsonrpc"]?.asText() == "2.0","MALFORMED_RPC")
            if(response.hasNonNull("error")) throw Failure("RPC_ERROR", "RPC $method error ${response["error"]["code"]?.asText() ?: "unknown"}")
            return response["result"] ?: throw Failure("MALFORMED_RPC")
        } finally { connection.disconnect() }
    }
    fun genesis(): String {
        val result = call("getGenesisHash").asText()
        val expected = if(cluster == "devnet") "EtWTRABZaYq6iMfeYKouRu166VU2xqa1wcaWoxPkrZBG" else "5eykt4UsFv8P8NJdTREpY1vzqKqZKvdpKuc147dw2N9d"
        ensure(result == expected,"CLUSTER_MISMATCH"); return result
    }
    fun balance(address: String): Balance {
        Base58.pubkey(address); genesis()
        val r = call("getBalance",listOf(address,mapOf("commitment" to "confirmed")))
        val value = r["value"]; ensure(value != null && value.isIntegralNumber && value.bigIntegerValue().signum() >= 0,"MALFORMED_BALANCE")
        return Balance(address,formatSol(value.bigIntegerValue()),r["context"]["slot"].asText(),Instant.now().toString())
    }
    fun skrBalance(address: String): Balance {
        ensure(cluster == "mainnet-beta","CLUSTER_MISMATCH"); Base58.pubkey(address); genesis()
        val mint = call("getAccountInfo",listOf(Programs.SKR,mapOf("encoding" to "jsonParsed","commitment" to "confirmed")))["value"]
        ensure(mint != null && !mint.isNull && mint["owner"].asText() == Programs.TOKEN && mint["data"]["parsed"]["type"].asText() == "mint" && mint["data"]["parsed"]["info"]["decimals"].asInt() == 6 && mint["data"]["parsed"]["info"]["isInitialized"].asBoolean(),"MINT_DEPLOYMENT_UNVERIFIED")
        val r = call("getTokenAccountsByOwner",listOf(address,mapOf("mint" to Programs.SKR),mapOf("encoding" to "jsonParsed","commitment" to "confirmed")))
        var sum = BigInteger.ZERO
        for(a in r["value"]) {
            val account = a["account"]; val info = account["data"]["parsed"]["info"]
            ensure(account["owner"].asText() == Programs.TOKEN && info["mint"].asText() == Programs.SKR && info["owner"].asText() == address && info["state"].asText() in listOf("initialized","frozen") && info["tokenAmount"]["decimals"].asInt() == 6,"TOKEN_ACCOUNT_MISMATCH")
            val amount = BigInteger(info["tokenAmount"]["amount"].asText()); ensure(amount in BigInteger.ZERO..U64_MAX,"BAD_AMOUNT"); sum += amount
        }
        return Balance(address,formatSkr(sum),r["context"]["slot"].asText(),Instant.now().toString())
    }
    fun history(address: String, before: String? = null): JsonNode {
        Base58.pubkey(address); genesis(); val options = mutableMapOf<String,Any>("limit" to 10,"commitment" to "confirmed"); if(before != null) options["before"] = before
        return call("getSignaturesForAddress",listOf(address,options))
    }
    fun transaction(signature: String): Pair<ByteArray,JsonNode> {
        ensure(Base58.decode(signature).size == 64,"BAD_SIGNATURE"); genesis()
        val r = call("getTransaction",listOf(signature,mapOf("encoding" to "base64","maxSupportedTransactionVersion" to 0,"commitment" to "confirmed")))
        ensure(!r.isNull,"TRANSACTION_UNAVAILABLE"); val bytes = Base64.getDecoder().decode(r["transaction"][0].asText()); parseTransaction(bytes); return bytes to r
    }
    fun latestDevnetBlockhash(): JsonNode { ensure(cluster == "devnet","CLUSTER_MISMATCH"); genesis(); return call("getLatestBlockhash",listOf(mapOf("commitment" to "confirmed"))) }
    fun estimateFee(message: ByteArray): JsonNode = call("getFeeForMessage",listOf(Base64.getEncoder().encodeToString(message),mapOf("commitment" to "confirmed"))).also { ensure(it.hasNonNull("value"),"FEE_UNAVAILABLE") }
    fun simulateTransaction(bytes: ByteArray): JsonNode = call("simulateTransaction",listOf(Base64.getEncoder().encodeToString(bytes),mapOf("encoding" to "base64","sigVerify" to false,"replaceRecentBlockhash" to false,"commitment" to "confirmed"))).also { ensure(it["value"]["err"].isNull,"SIMULATION_FAILED") }
    fun blockhashValid(hash: String): Boolean = call("isBlockhashValid",listOf(hash,mapOf("commitment" to "confirmed")))["value"].asBoolean()
    fun submitSignedTransaction(bytes: ByteArray, expectedSignature: String): String {
        ensure(cluster == "devnet","MAINNET_SIGNING_BLOCKED"); genesis()
        val result = call("sendTransaction",listOf(Base64.getEncoder().encodeToString(bytes),mapOf("encoding" to "base64","skipPreflight" to false,"maxRetries" to 0,"preflightCommitment" to "confirmed"))).asText()
        ensure(result == expectedSignature,"RPC_SIGNATURE_MISMATCH"); return result
    }
    fun observeSettlement(signature: String): JsonNode {
        ensure(Base58.decode(signature).size == 64,"BAD_SIGNATURE"); genesis()
        return call("getSignatureStatuses",listOf(listOf(signature),mapOf("searchTransactionHistory" to true)))["value"][0]
    }
}
data class Balance(val address: String, val value: String, val slot: String, val observedAt: String)
