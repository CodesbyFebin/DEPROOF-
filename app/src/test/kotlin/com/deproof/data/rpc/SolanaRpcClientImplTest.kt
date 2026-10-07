// SolanaRpcClientImplTest.kt — Unit tests for real Solana RPC client.
//
// Tests cover:
// - Successful RPC calls with proper parsing
// - Error handling (RPC errors, network errors, parse errors)
// - Exponential backoff retry behavior
// - JSON-RPC 2.0 compliance
package com.deproof.data.rpc

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.node.JsonNodeFactory
import com.fasterxml.jackson.databind.node.ObjectNode
import kotlinx.coroutines.runBlocking
import okhttp3.OkHttpClient
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import okhttp3.mockwebserver.SocketPolicy
import org.junit.After
import org.junit.Before
import org.junit.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertIs
import kotlin.test.assertTrue

class SolanaRpcClientImplTest {
    private lateinit var server: MockWebServer
    private lateinit var endpoint: RpcEndpoint
    private lateinit var client: SolanaRpcClientImpl

    @Before
    fun setup() {
        server = MockWebServer()
        server.start()
        endpoint = RpcEndpoint(
            network = SolanaNetwork.DEVNET,
            url = server.url("").toString().dropLast(1)
        )
        val httpClient = OkHttpClient.Builder()
            .connectTimeout(java.time.Duration.ofSeconds(5).toMillis(), java.util.concurrent.TimeUnit.MILLISECONDS)
            .build()
        client = SolanaRpcClientImpl(endpoint, httpClient)
    }

    @After
    fun tearDown() {
        server.shutdown()
    }

    @Test
    fun getAccountInfo_success() {
        val response = createJsonRpcResponse("""
            {
                "value": {
                    "lamports": 1000000,
                    "owner": "TokenkegQfeZyiNwAJbNbGKPFXCWuBvf9Ss623VQ5DA",
                    "executable": false,
                    "data": ["base64string", "0"]
                }
            }
        """)
        server.enqueue(MockResponse().setBody(response))

        val result = runBlocking {
            client.getAccountInfo("SKRbvo6Gf7GondiT3BbTfuRDPqLWei4j2Qy2NPGZhW3")
        }

        assertTrue(result.isSuccess)
        val info = result.getOrNull()!!
        assertEquals("TokenkegQfeZyiNwAJbNbGKPFXCWuBvf9Ss623VQ5DA", info.owner)
        assertEquals(1000000L, info.lamports)
        assertFalse(info.executable)
    }

    @Test
    fun getAccountInfo_notFound() {
        val response = createJsonRpcErrorResponse(
            code = -32602,
            message = "Account not found"
        )
        server.enqueue(MockResponse().setBody(response))

        val result = runBlocking {
            client.getAccountInfo("InvalidAddress")
        }

        assertTrue(result.isFailure)
        assertIs<RpcError>(result.exceptionOrNull())
    }

    @Test
    fun getTokenAccountsByOwner_empty() {
        val response = createJsonRpcResponse("""
            {
                "value": []
            }
        """)
        server.enqueue(MockResponse().setBody(response))

        val result = runBlocking {
            client.getTokenAccountsByOwner("9B5X...", "SKRbvo6Gf7GondiT3BbTfuRDPqLWei4j2Qy2NPGZhW3")
        }

        assertTrue(result.isSuccess)
        val accounts = result.getOrNull()!!
        assertEquals(0, accounts.size)
    }

    @Test
    fun getTokenAccountsByOwner_withAccounts() {
        val response = createJsonRpcResponse("""
            {
                "value": [
                    {
                        "pubkey": "Ata1...",
                        "account": {
                            "lamports": 2039280,
                            "data": {
                                "parsed": {
                                    "info": {
                                        "mint": "SKRbvo6Gf7GondiT3BbTfuRDPqLWei4j2Qy2NPGZhW3",
                                        "owner": "9B5X...",
                                        "tokenAmount": {
                                            "amount": "1500000",
                                            "decimals": 6
                                        }
                                    }
                                }
                            }
                        }
                    },
                    {
                        "pubkey": "Ata2...",
                        "account": {
                            "lamports": 2039280,
                            "data": {
                                "parsed": {
                                    "info": {
                                        "mint": "SKRbvo6Gf7GondiT3BbTfuRDPqLWei4j2Qy2NPGZhW3",
                                        "owner": "9B5X...",
                                        "tokenAmount": {
                                            "amount": "500000",
                                            "decimals": 6
                                        }
                                    }
                                }
                            }
                        }
                    }
                ]
            }
        """)
        server.enqueue(MockResponse().setBody(response))

        val result = runBlocking {
            client.getTokenAccountsByOwner("9B5X...", "SKRbvo6Gf7GondiT3BbTfuRDPqLWei4j2Qy2NPGZhW3")
        }

        assertTrue(result.isSuccess)
        val accounts = result.getOrNull()!!
        assertEquals(2, accounts.size)
        assertEquals("1500000", accounts[0].amount)
        assertEquals("500000", accounts[1].amount)
    }

    @Test
    fun getSignatureStatuses_mixed() {
        val response = createJsonRpcResponse("""
            [
                {
                    "slot": 12345,
                    "confirmations": 10,
                    "err": null,
                    "confirmationStatus": "finalized"
                },
                null,
                {
                    "slot": 12344,
                    "confirmations": 9,
                    "err": null,
                    "confirmationStatus": "confirmed"
                }
            ]
        """)
        server.enqueue(MockResponse().setBody(response))

        val result = runBlocking {
            client.getSignatureStatuses(listOf("sig1", "sig2", "sig3"))
        }

        assertTrue(result.isSuccess)
        val statuses = result.getOrNull()!!
        assertEquals(2, statuses.size)  // null is filtered out
        assertEquals(10, statuses[0].confirmations)
    }

    @Test
    fun networkError_connectionTimeout() {
        server.enqueue(MockResponse().setSocketPolicy(SocketPolicy.NO_RESPONSE))

        val result = runBlocking {
            client.getAccountInfo("SKRbvo6Gf7GondiT3BbTfuRDPqLWei4j2Qy2NPGZhW3")
        }

        assertTrue(result.isFailure)
        assertIs<NetworkError>(result.exceptionOrNull())
    }

    @Test
    fun networkError_httpError() {
        server.enqueue(MockResponse().setResponseCode(500).setBody("Internal Server Error"))

        val result = runBlocking {
            client.getAccountInfo("SKRbvo6Gf7GondiT3BbTfuRDPqLWei4j2Qy2NPGZhW3")
        }

        assertTrue(result.isFailure)
        assertIs<NetworkError>(result.exceptionOrNull())
    }

    @Test
    fun parseError_invalidJson() {
        server.enqueue(MockResponse().setBody("not valid json"))

        val result = runBlocking {
            client.getAccountInfo("SKRbvo6Gf7GondiT3BbTfuRDPqLWei4j2Qy2NPGZhW3")
        }

        assertTrue(result.isFailure)
        assertIs<ParseError>(result.exceptionOrNull())
    }

    @Test
    fun parseError_missingResult() {
        server.enqueue(MockResponse().setBody("""
            {"jsonrpc": "2.0", "id": 1}
        """))

        val result = runBlocking {
            client.getAccountInfo("SKRbvo6Gf7GondiT3BbTfuRDPqLWei4j2Qy2NPGZhW3")
        }

        assertTrue(result.isFailure)
        assertIs<ParseError>(result.exceptionOrNull())
    }

    @Test
    fun rpcError_noRetry() {
        val response = createJsonRpcErrorResponse(
            code = -32600,
            message = "Invalid Request"
        )
        // Only enqueue one response; if retry happens, test will fail
        server.enqueue(MockResponse().setBody(response))

        val result = runBlocking {
            client.getAccountInfo("InvalidAddress")
        }

        assertTrue(result.isFailure)
        assertIs<RpcError>(result.exceptionOrNull())
        assertEquals(1, server.requestCount)  // No retry
    }

    // Helper functions
    private fun createJsonRpcResponse(result: String): String {
        val mapper = ObjectMapper()
        val resultNode = mapper.readTree(result)
        val response = ObjectNode(JsonNodeFactory.instance).apply {
            put("jsonrpc", "2.0")
            set<Any>("result", resultNode)
            put("id", 1)
        }
        return mapper.writeValueAsString(response)
    }

    private fun createJsonRpcErrorResponse(code: Int, message: String): String {
        val mapper = ObjectMapper()
        val error = ObjectNode(JsonNodeFactory.instance).apply {
            put("code", code)
            put("message", message)
        }
        val response = ObjectNode(JsonNodeFactory.instance).apply {
            put("jsonrpc", "2.0")
            set<Any>("error", error)
            put("id", 1)
        }
        return mapper.writeValueAsString(response)
    }
}
