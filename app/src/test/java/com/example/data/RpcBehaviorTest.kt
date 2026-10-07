package com.example.data

import com.example.domain.*
import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.ObjectMapper
import org.junit.Test
import org.junit.Assert.*
import java.math.BigInteger

/**
 * JVM-level tests for Rpc.kt behavior that is verifiable without a live network.
 * Uses the testTransport hook so no HTTP calls are made.
 *
 * Criteria covered (partial—device gate still required for full acceptance):
 *   C005 getTokenAccountsByOwner with mint filter
 *   C006 Integer addition of multiple SKR accounts
 *   C007 SKR displayed with 6 decimal places
 *   C008 Slot from context.slot of the same response
 *   C010 SOL and SKR reads are independent; one failure does not zero the other
 *   C013 History requests exactly 10 signatures with commitment=confirmed
 *   C021 Never fabricates a signature (Rpc refuses a mismatched RPC response)
 *   C023 Empty history is empty, not a fake row
 *   C062 Cluster of a transaction review must match the session cluster
 *   C093 SKR reads use the validated mainnet mint; mainnet-beta cluster enforced
 *   C094 Devnet-only lock for memo signing (submitSignedTransaction blocked on mainnet)
 */
class RpcBehaviorTest {

    private val mainnetGenesis = "5eykt4UsFv8P8NJdTREpY1vzqKqZKvdpKuc147dw2N9d"
    private val devnetGenesis  = "EtWTRABZaYq6iMfeYKouRu166VU2xqa1wcaWoxPkrZBG"
    private val address        = Base58.encode(ByteArray(32) { 7 })
    private val mapper         = ObjectMapper()

    private fun mainnetRpc(handler: (String, List<Any?>) -> JsonNode) =
        Rpc("https://api.mainnet-beta.solana.com", "mainnet-beta") { m, p -> handler(m, p) }

    private fun devnetRpc(handler: (String, List<Any?>) -> JsonNode) =
        Rpc("https://api.devnet.solana.com", "devnet") { m, p -> handler(m, p) }

    private fun fails(code: String, block: () -> Unit) {
        try { block(); fail("Expected Failure($code) but nothing was thrown") }
        catch (e: Failure) { assertEquals(code, e.code) }
    }

    // --- SKR balance: C005, C006, C007, C008 ---

    @Test fun skrBalanceAggregatesMultipleAccountsWithIntegerAddition() {
        // C005: filtered by official SKR mint; C006: integer addition; C007: 6 decimals; C008: slot
        val rpc = mainnetRpc { method, _ ->
            when (method) {
                "getGenesisHash" -> mapper.valueToTree(mainnetGenesis)
                "getAccountInfo" -> mapper.readTree("""
                    {"value":{"owner":"${Programs.TOKEN}","data":{"parsed":
                    {"type":"mint","info":{"decimals":6,"isInitialized":true}}}}}
                """.trimIndent())
                "getTokenAccountsByOwner" -> mapper.readTree("""
                    {"context":{"slot":999},"value":[
                        {"account":{"owner":"${Programs.TOKEN}","data":{"parsed":{"info":{
                            "mint":"${Programs.SKR}","owner":"$address","state":"initialized",
                            "tokenAmount":{"decimals":6,"amount":"1000000"}}}}}},
                        {"account":{"owner":"${Programs.TOKEN}","data":{"parsed":{"info":{
                            "mint":"${Programs.SKR}","owner":"$address","state":"initialized",
                            "tokenAmount":{"decimals":6,"amount":"2000000"}}}}}}
                    ]}
                """.trimIndent())
                else -> throw Failure("UNEXPECTED_METHOD")
            }
        }
        val bal = rpc.skrBalance(address)
        assertEquals("3.000000", bal.value)    // C006 integer sum; C007 6 decimals
        assertEquals("999", bal.slot)           // C008 slot from context.slot
        assertEquals(address, bal.address)      // C009 address matches queried address
    }

    @Test fun skrBalanceZeroWhenNoMatchingAccounts() {
        // C005: only accounts matching the mint filter are included (empty result = 0)
        val rpc = mainnetRpc { method, _ ->
            when (method) {
                "getGenesisHash" -> mapper.valueToTree(mainnetGenesis)
                "getAccountInfo" -> mapper.readTree("""
                    {"value":{"owner":"${Programs.TOKEN}","data":{"parsed":
                    {"type":"mint","info":{"decimals":6,"isInitialized":true}}}}}
                """.trimIndent())
                "getTokenAccountsByOwner" -> mapper.readTree("""{"context":{"slot":1},"value":[]}""")
                else -> throw Failure("UNEXPECTED_METHOD")
            }
        }
        val bal = rpc.skrBalance(address)
        assertEquals("0.000000", bal.value)
    }

    @Test fun skrBalanceRejectsWrongOwnerOrMintInResponse() {
        // C005: response with non-SKR mint must be rejected (TOKEN_ACCOUNT_MISMATCH)
        val wrongMint = Base58.encode(ByteArray(32) { 99 })
        val rpc = mainnetRpc { method, _ ->
            when (method) {
                "getGenesisHash" -> mapper.valueToTree(mainnetGenesis)
                "getAccountInfo" -> mapper.readTree("""
                    {"value":{"owner":"${Programs.TOKEN}","data":{"parsed":
                    {"type":"mint","info":{"decimals":6,"isInitialized":true}}}}}
                """.trimIndent())
                "getTokenAccountsByOwner" -> mapper.readTree("""
                    {"context":{"slot":1},"value":[
                        {"account":{"owner":"${Programs.TOKEN}","data":{"parsed":{"info":{
                            "mint":"$wrongMint","owner":"$address","state":"initialized",
                            "tokenAmount":{"decimals":6,"amount":"1000000"}}}}}}
                    ]}
                """.trimIndent())
                else -> throw Failure("UNEXPECTED_METHOD")
            }
        }
        fails("TOKEN_ACCOUNT_MISMATCH") { rpc.skrBalance(address) }
    }

    @Test fun skrBalanceRequiresMainnetCluster() {
        // C093: SKR reads are gated to mainnet-beta; devnet throws CLUSTER_MISMATCH
        val rpc = devnetRpc { method, _ ->
            when (method) {
                "getGenesisHash" -> mapper.valueToTree(devnetGenesis)
                else -> throw Failure("UNEXPECTED_METHOD")
            }
        }
        fails("CLUSTER_MISMATCH") { rpc.skrBalance(address) }
    }

    @Test fun skrMintDeploymentMustBeVerifiedBeforeBalance() {
        // C093: unverified mint causes MINT_DEPLOYMENT_UNVERIFIED, never returns balance
        val rpc = mainnetRpc { method, _ ->
            when (method) {
                "getGenesisHash" -> mapper.valueToTree(mainnetGenesis)
                // Mint account returns wrong owner (not SPL Token program)
                "getAccountInfo" -> mapper.readTree("""{"value":{"owner":"wrong","data":{"parsed":{}}}}""")
                else -> throw Failure("UNEXPECTED_METHOD")
            }
        }
        fails("MINT_DEPLOYMENT_UNVERIFIED") { rpc.skrBalance(address) }
    }

    // --- Independence: C010 ---

    @Test fun solFailureDoesNotClearSkrBalanceAndViceVersa() {
        // C010: SOL and SKR are independent reads; one error does not zero the other
        val solFails = mainnetRpc { method, _ ->
            when (method) {
                "getGenesisHash" -> mapper.valueToTree(mainnetGenesis)
                "getBalance" -> throw Failure("RPC_ERROR")
                "getAccountInfo" -> mapper.readTree("""
                    {"value":{"owner":"${Programs.TOKEN}","data":{"parsed":
                    {"type":"mint","info":{"decimals":6,"isInitialized":true}}}}}
                """.trimIndent())
                "getTokenAccountsByOwner" -> mapper.readTree("""
                    {"context":{"slot":1},"value":[
                        {"account":{"owner":"${Programs.TOKEN}","data":{"parsed":{"info":{
                            "mint":"${Programs.SKR}","owner":"$address","state":"initialized",
                            "tokenAmount":{"decimals":6,"amount":"500000"}}}}}}
                    ]}
                """.trimIndent())
                else -> throw Failure("UNEXPECTED_METHOD")
            }
        }
        // SOL balance throws; SKR balance still works
        fails("RPC_ERROR") { solFails.balance(address) }
        val skr = solFails.skrBalance(address)
        assertEquals("0.500000", skr.value)

        // SKR balance throws; SOL balance still works
        val skrFails = mainnetRpc { method, _ ->
            when (method) {
                "getGenesisHash" -> mapper.valueToTree(mainnetGenesis)
                "getBalance" -> mapper.readTree("""{"context":{"slot":5},"value":2000000000}""")
                else -> throw Failure("RPC_ERROR")
            }
        }
        fails("RPC_ERROR") { skrFails.skrBalance(address) }
        val sol = skrFails.balance(address)
        assertEquals("2.000000000", sol.value)
    }

    // --- History: C013, C023 ---

    @Test fun historyRequestsLimitTenWithConfirmedCommitment() {
        // C013: getSignaturesForAddress with limit=10 and commitment=confirmed
        var capturedParams: List<Any?> = emptyList()
        val rpc = mainnetRpc { method, params ->
            when (method) {
                "getGenesisHash" -> mapper.valueToTree(mainnetGenesis)
                "getSignaturesForAddress" -> { capturedParams = params; mapper.readTree("[]") }
                else -> throw Failure("UNEXPECTED_METHOD")
            }
        }
        rpc.history(address)
        assertTrue("params must include address", capturedParams.isNotEmpty())
        assertEquals(address, capturedParams[0])
        @Suppress("UNCHECKED_CAST")
        val options = capturedParams[1] as Map<*, *>
        assertEquals(10, options["limit"])
        assertEquals("confirmed", options["commitment"])
    }

    @Test fun emptyHistoryReturnsEmptyArrayNotFakeRow() {
        // C023: empty history = empty array; no fabricated entries
        val rpc = mainnetRpc { method, _ ->
            when (method) {
                "getGenesisHash" -> mapper.valueToTree(mainnetGenesis)
                "getSignaturesForAddress" -> mapper.readTree("[]")
                else -> throw Failure("UNEXPECTED_METHOD")
            }
        }
        val result = rpc.history(address)
        assertTrue("empty history must be an array", result.isArray)
        assertEquals("empty array must have zero elements", 0, result.size())
    }

    @Test fun historyReturnsAllSignaturesFromRpc() {
        // C013: returns the raw signature list from RPC without fabrication
        val sig = Base58.encode(ByteArray(64) { 1 })
        val rpc = mainnetRpc { method, _ ->
            when (method) {
                "getGenesisHash" -> mapper.valueToTree(mainnetGenesis)
                "getSignaturesForAddress" -> mapper.readTree("""[{"signature":"$sig","slot":10,"err":null}]""")
                else -> throw Failure("UNEXPECTED_METHOD")
            }
        }
        val result = rpc.history(address)
        assertEquals(1, result.size())
        assertEquals(sig, result[0]["signature"].asText())
    }

    // --- Transaction: C021, submission lock ---

    @Test fun submitSignedTransactionIsBlockedOnMainnet() {
        // C094: devnet-only lock; mainnet cluster throws MAINNET_SIGNING_BLOCKED
        val mainnet = mainnetRpc { _, _ -> throw Failure("SHOULD_NOT_REACH") }
        val dummyBytes = buildMemo(address, Base58.encode(ByteArray(32) { 9 }), 1000)
        fails("MAINNET_SIGNING_BLOCKED") { mainnet.submitSignedTransaction(dummyBytes, "sig") }
    }

    @Test fun transactionUnavailableRefusesNullResult() {
        // When getTransaction returns null the Rpc layer throws TRANSACTION_UNAVAILABLE
        val sig = Base58.encode(ByteArray(64) { 1 })
        val rpc = mainnetRpc { method, _ ->
            when (method) {
                "getGenesisHash" -> mapper.valueToTree(mainnetGenesis)
                "getTransaction"  -> mapper.nullNode()
                else -> throw Failure("UNEXPECTED_METHOD")
            }
        }
        fails("TRANSACTION_UNAVAILABLE") { rpc.transaction(sig) }
    }

    // --- C062: Cluster mismatch in canSign ---

    @Test fun canSignRefusesWhenReviewClusterDiffersFromSigningGate() {
        // C062: cluster on the gate must match cluster on the review context
        val account = Base58.encode(ByteArray(32) { 7 })
        val blockhash = Base58.encode(ByteArray(32) { 9 })
        val bytes = buildMemo(account, blockhash, 1000)
        val ctx = ReviewContext(
            account, "devnet", "EtWTRABZaYq6iMfeYKouRu166VU2xqa1wcaWoxPkrZBG",
            Policy.DEVNET_MEMO_V1,
            reviewedAt = "2026-10-06T00:00:00Z", lastValidBlockHeight = "123",
            feeLamports = "5001", feeSlot = "120"
        )
        val review = Review(bytes, ctx)
        val gate = SigningGate(account, "devnet", true, true, true, true, false)

        assertTrue(canSign(review, bytes, ctx, gate))
        // Gate says mainnet-beta but review says devnet → must not sign
        assertFalse(canSign(review, bytes, ctx, gate.copy(cluster = "mainnet-beta")))
        // Gate says null cluster → must not sign
        assertFalse(canSign(review, bytes, ctx, gate.copy(cluster = null)))
    }

    // --- C039: shortKey ---

    @Test fun shortKeyProducesEllipsisAbbreviationForLongAddresses() {
        // C039: keys longer than 8 chars → 4 + "…" + 4
        val full = "abcdefghijklmnop"
        assertEquals("abcd…mnop", shortKey(full))
        // Keys ≤ 8 chars are returned unchanged
        assertEquals("abcdefgh", shortKey("abcdefgh"))
        assertEquals("abc", shortKey("abc"))
    }
}
