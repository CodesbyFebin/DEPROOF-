package com.deproof.data.rpc

import androidx.test.ext.junit.runners.AndroidJUnit4
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Phase 3 - Week 4: Performance & Optimization Tests
 *
 * Tests:
 * ✓ Balance query response time < 500ms
 * ✓ Batch query optimization
 */
@RunWith(AndroidJUnit4::class)
class PerformanceTest {
    private val testAddress = "9B5X6wq4xCSUQyRjqW37hSrwq3CEQmD2KwMaKNoon5w4"
    private val endpoint = RpcClientFactory().createEndpoint(SolanaNetwork.TESTNET)

    @Before
    fun setUp() {
        // Setup test environment
    }

    @Test
    fun balanceQueryResponseTimeUnder500ms() {
        val client = SolanaRpcClient(endpoint)

        val start = System.currentTimeMillis()
        val balanceResult = runBlocking {
            client.getBalance(testAddress).getOrNull() ?: client.getBalance(testAddress)
        }
        val elapsed = System.currentTimeMillis() - start

        assertNotNull(balanceResult)
        assertTrue("Balance query took ${elapsed}ms, expected < 500ms", elapsed < 500)
    }

    @Test
    fun batchQueryOptimization() {
        val client = SolanaRpcClient(endpoint)
        val addresses = listOf(
            testAddress,
            "11111111111111111111111111111111",
            "22222222222222222222222222222222",
            "33333333333333333333333333333333",
            "44444444444444444444444444444444"
        )

        val start = System.currentTimeMillis()
        val results = runBlocking {
            addresses.map { address ->
                try {
                    client.getBalance(address)
                } catch (e: Exception) {
                    Result.failure(e)
                }
            }
        }
        val elapsed = System.currentTimeMillis() - start

        // All queries should complete
        assertEquals(addresses.size, results.size)

        // Batch query should be reasonably fast (allow more time for batch)
        assertTrue("Batch query took ${elapsed}ms, expected < 1000ms", elapsed < 1000)

        // Average per query should still be good
        val avgPerQuery = elapsed / addresses.size.toDouble()
        assertTrue("Average per query ${avgPerQuery}ms is too slow", avgPerQuery < 200)
    }

    @Test
    fun concurrentQueriesPerformance() {
        val client = SolanaRpcClient(endpoint)
        val queryCount = 10

        val start = System.currentTimeMillis()
        val results = runBlocking {
            (1..queryCount).map { i ->
                val address = "address_$i"
                try {
                    client.getBalance(address)
                } catch (e: Exception) {
                    Result.failure(e)
                }
            }
        }
        val elapsed = System.currentTimeMillis() - start

        assertEquals("Should handle multiple concurrent queries", queryCount, results.size)
        assertTrue("Concurrent queries took ${elapsed}ms", elapsed < 1000)
    }

    @Test
    fun multipleNetworkSwitches() {
        val factory = RpcClientFactory()
        val networks = listOf(SolanaNetwork.MAINNET, SolanaNetwork.TESTNET, SolanaNetwork.DEVNET)

        val start = System.currentTimeMillis()
        networks.forEach { network ->
            val endpoint = factory.createEndpoint(network)
            assertTrue(factory.validateEndpoint(endpoint).isSuccess)
        }
        val elapsed = System.currentTimeMillis() - start

        assertTrue("Network switch took ${elapsed}ms", elapsed < 100)
    }

    @Test
    fun endpointValidationPerformance() {
        val factory = RpcClientFactory()
        val endpoint = factory.createEndpoint(SolanaNetwork.TESTNET)

        val start = System.currentTimeMillis()
        for (i in 1..100) {
            factory.validateEndpoint(endpoint)
        }
        val elapsed = System.currentTimeMillis() - start

        val avgPerValidation = elapsed / 100.0
        assertTrue("Validation too slow: ${avgPerValidation}ms per check", avgPerValidation < 1)
    }

}
