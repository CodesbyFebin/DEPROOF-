package com.deproof.data.solana

import org.junit.Before
import org.junit.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class SolanaRpcClientTest {

    private lateinit var client: SolanaRpcClient

    @Before
    fun setUp() {
        client = SolanaRpcClient("https://api.devnet.solana.com")
    }

    @Test
    fun testRpcClientInitialization() {
        assertNotNull(client)
    }

    @Test
    fun testLatestBlockhashRequest() {
        val request = RpcRequest(
            method = "getLatestBlockhash",
            params = listOf(mapOf("commitment" to "finalized"))
        )
        assertEquals("getLatestBlockhash", request.method)
        assertEquals(1, request.id)
        assertEquals("2.0", request.jsonrpc)
    }

    @Test
    fun testBalanceRequest() {
        val pubkey = "11111111111111111111111111111111"
        val request = RpcRequest(
            method = "getBalance",
            params = listOf(pubkey, mapOf("commitment" to "finalized"))
        )
        assertEquals("getBalance", request.method)
        assertEquals(2, request.params.size)
    }

    @Test
    fun testAccountInfoRequest() {
        val pubkey = "11111111111111111111111111111111"
        val request = RpcRequest(
            method = "getAccountInfo",
            params = listOf(
                pubkey,
                mapOf("encoding" to "jsonParsed", "commitment" to "finalized")
            )
        )
        assertEquals("getAccountInfo", request.method)
        assertEquals(2, request.params.size)
    }

    @Test
    fun testSendTransactionRequest() {
        val tx = "base64encodedtransaction=="
        val request = RpcRequest(
            method = "sendTransaction",
            params = listOf(
                tx,
                mapOf(
                    "encoding" to "base64",
                    "skipPreflight" to false,
                    "preflightCommitment" to "confirmed"
                )
            )
        )
        assertEquals("sendTransaction", request.method)
        assertEquals(2, request.params.size)
    }

    @Test
    fun testSignatureStatusRequest() {
        val sig = "signature123"
        val request = RpcRequest(
            method = "getSignatureStatuses",
            params = listOf(
                listOf(sig),
                mapOf("searchTransactionHistory" to true)
            )
        )
        assertEquals("getSignatureStatuses", request.method)
        assertTrue(request.params[0] is List<*>)
    }
}

class ProofSubmissionServiceTest {

    private lateinit var service: ProofSubmissionService
    private lateinit var rpcClient: SolanaRpcClient

    @Before
    fun setUp() {
        rpcClient = SolanaRpcClient("https://api.devnet.solana.com")
        service = ProofSubmissionService(rpcClient)
    }

    @Test
    fun testProofSubmissionRequestCreation() {
        val request = ProofSubmissionRequest(
            deviceId = "device-001",
            observations = listOf("obs1", "obs2"),
            proofHash = "sha256hash123",
            timestamp = System.currentTimeMillis()
        )

        assertEquals("device-001", request.deviceId)
        assertEquals(2, request.observations.size)
        assertNotNull(request.timestamp)
    }

    @Test
    fun testProofSubmissionResultDefaults() {
        val result = ProofSubmissionResult(
            transactionSignature = "sig123",
            status = SubmissionStatus.CONFIRMED
        )

        assertEquals("sig123", result.transactionSignature)
        assertEquals(SubmissionStatus.CONFIRMED, result.status)
    }

    @Test
    fun testProofSubmissionStatusProgression() {
        val statusProgression = listOf(
            SubmissionStatus.PENDING,
            SubmissionStatus.CONFIRMING,
            SubmissionStatus.CONFIRMED
        )

        assertEquals(SubmissionStatus.PENDING, statusProgression[0])
        assertEquals(SubmissionStatus.CONFIRMING, statusProgression[1])
        assertEquals(SubmissionStatus.CONFIRMED, statusProgression[2])
    }

    @Test
    fun testProofSubmissionFailureStatus() {
        val result = ProofSubmissionResult(
            transactionSignature = null,
            status = SubmissionStatus.FAILED,
            error = "Transaction failed"
        )

        assertEquals(SubmissionStatus.FAILED, result.status)
        assertNotNull(result.error)
    }

    @Test
    fun testProofSubmissionTimeoutStatus() {
        val result = ProofSubmissionResult(
            transactionSignature = "sig123",
            status = SubmissionStatus.TIMEOUT,
            error = "Confirmation timeout"
        )

        assertEquals(SubmissionStatus.TIMEOUT, result.status)
    }
}

class RpcRequestTest {

    @Test
    fun testRpcRequestDefaults() {
        val request = RpcRequest(method = "getBalance")

        assertEquals("2.0", request.jsonrpc)
        assertEquals(1, request.id)
        assertEquals("getBalance", request.method)
        assertTrue(request.params.isEmpty())
    }

    @Test
    fun testRpcRequestWithParams() {
        val params = listOf("pubkey123", mapOf("commitment" to "finalized"))
        val request = RpcRequest(
            method = "getBalance",
            params = params
        )

        assertEquals(2, request.params.size)
        assertEquals("pubkey123", request.params[0])
    }

    @Test
    fun testRpcRequestWithMultipleParams() {
        val params = listOf(
            "transaction",
            mapOf("encoding" to "base64", "skipPreflight" to false)
        )
        val request = RpcRequest(
            method = "sendTransaction",
            params = params,
            id = 42
        )

        assertEquals(42, request.id)
        assertEquals(2, request.params.size)
    }
}

class RpcResponseTest {

    @Test
    fun testRpcResponseSuccess() {
        val result = mapOf("blockhash" to "abc123", "lastValidBlockHeight" to 123456)
        val response = RpcResponse<Map<String, Any>>(
            result = result,
            error = null,
            id = 1
        )

        assertEquals(result, response.result)
        assertEquals(null, response.error)
    }

    @Test
    fun testRpcResponseError() {
        val error = RpcError(
            code = -32603,
            message = "Internal error",
            data = "Transaction failed"
        )
        val response = RpcResponse<String>(
            result = null,
            error = error,
            id = 1
        )

        assertEquals(null, response.result)
        assertNotNull(response.error)
        assertEquals(-32603, response.error?.code)
    }

    @Test
    fun testRpcErrorDetails() {
        val error = RpcError(
            code = -32700,
            message = "Parse error",
            data = "Invalid JSON"
        )

        assertEquals(-32700, error.code)
        assertEquals("Parse error", error.message)
        assertEquals("Invalid JSON", error.data)
    }
}

class AccountInfoTest {

    @Test
    fun testAccountInfoCreation() {
        val accountInfo = AccountInfo(
            lamports = 1000000,
            owner = "11111111111111111111111111111111",
            executable = false,
            rentEpoch = 100
        )

        assertEquals(1000000, accountInfo.lamports)
        assertEquals("11111111111111111111111111111111", accountInfo.owner)
        assertEquals(false, accountInfo.executable)
        assertEquals(100, accountInfo.rentEpoch)
    }

    @Test
    fun testAccountInfoSystemAccount() {
        val systemAccount = AccountInfo(
            lamports = 0,
            owner = "11111111111111111111111111111111",
            executable = false,
            rentEpoch = 0
        )

        assertEquals("11111111111111111111111111111111", systemAccount.owner)
    }

    @Test
    fun testAccountInfoExecutableProgram() {
        val program = AccountInfo(
            lamports = 500000000,
            owner = "BPFLoaderUpgradeab1e11111111111111111111111",
            executable = true,
            rentEpoch = 50
        )

        assertEquals(true, program.executable)
    }
}

class LatestBlockhashTest {

    @Test
    fun testLatestBlockhashCreation() {
        val blockhash = LatestBlockhash(
            blockhash = "abc123def456",
            lastValidBlockHeight = 200000000
        )

        assertEquals("abc123def456", blockhash.blockhash)
        assertEquals(200000000, blockhash.lastValidBlockHeight)
    }

    @Test
    fun testBlockhashLength() {
        val blockhash = LatestBlockhash(
            blockhash = "a".repeat(44),
            lastValidBlockHeight = 150000000
        )

        assertEquals(44, blockhash.blockhash.length)
    }
}
