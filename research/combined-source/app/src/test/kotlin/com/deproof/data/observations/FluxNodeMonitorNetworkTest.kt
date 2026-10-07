package com.deproof.data.observations

import kotlinx.coroutines.runBlocking
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.Assert.*

class FluxNodeMonitorNetworkTest {

    private lateinit var mockWebServer: MockWebServer

    @Before
    fun setup() {
        mockWebServer = MockWebServer()
        mockWebServer.start()
    }

    @After
    fun tearDown() {
        mockWebServer.shutdown()
    }

    @Test
    fun testSuccessfulNodeHealthQuery() = runBlocking {
        val responseBody = """{
            "nodeId": "flux-node-001",
            "tier": "Cumulus",
            "status": "synced",
            "uptime": 1234567,
            "cpuUsage": 45.2,
            "memoryUsage": 62.8,
            "storageUsage": 78.5,
            "networkBandwidth": 100,
            "collateralStatus": "LOCKED",
            "benchmarkScore": 85000,
            "lastSeen": 1700000000000
        }"""

        mockWebServer.enqueue(MockResponse().setBody(responseBody))

        val endpoint = mockWebServer.hostName + ":" + mockWebServer.port
        val observation = FluxNodeMonitor.getNodeHealth("flux-node-001", endpoint)

        assertNotNull(observation)
        observation?.let { obs ->
            assertEquals("flux-node-001", obs.nodeMetrics.nodeId)
            assertEquals("Cumulus", obs.nodeMetrics.tier)
            assertEquals(85000L, obs.nodeMetrics.benchmarkScore)
            assertEquals(1234567L, obs.nodeMetrics.uptime)
            assertEquals("LOCKED", obs.nodeMetrics.collateralStatus)
        }
    }

    @Test
    fun testRetryOnHttpError() = runBlocking {
        // First two attempts fail with 500 error
        mockWebServer.enqueue(MockResponse().setResponseCode(500))
        mockWebServer.enqueue(MockResponse().setResponseCode(500))

        // Third attempt succeeds
        val successBody = """{
            "nodeId": "flux-node-002",
            "tier": "Stratus",
            "status": "synced",
            "uptime": 9876543,
            "cpuUsage": 32.1,
            "memoryUsage": 48.5,
            "storageUsage": 65.0,
            "networkBandwidth": 200,
            "collateralStatus": "LOCKED",
            "benchmarkScore": 75000,
            "lastSeen": 1700000000000
        }"""
        mockWebServer.enqueue(MockResponse().setBody(successBody))

        val endpoint = mockWebServer.hostName + ":" + mockWebServer.port
        val observation = FluxNodeMonitor.getNodeHealth("flux-node-002", endpoint)

        assertNotNull(observation)
        observation?.let { obs ->
            assertEquals("flux-node-002", obs.nodeMetrics.nodeId)
            assertEquals("Stratus", obs.nodeMetrics.tier)
            assertEquals(75000L, obs.nodeMetrics.benchmarkScore)
        }

        // Verify 3 requests were made (2 failures + 1 success)
        assertEquals(3, mockWebServer.requestCount)
    }

    @Test
    fun testFailureAfterMaxRetries() = runBlocking {
        // All 3 attempts fail
        mockWebServer.enqueue(MockResponse().setResponseCode(500))
        mockWebServer.enqueue(MockResponse().setResponseCode(500))
        mockWebServer.enqueue(MockResponse().setResponseCode(500))

        val endpoint = mockWebServer.hostName + ":" + mockWebServer.port
        val observation = FluxNodeMonitor.getNodeHealth("flux-node-fail", endpoint)

        assertNull(observation)
        assertEquals(3, mockWebServer.requestCount)
    }

    @Test
    fun testRetryOnNetworkError() = runBlocking {
        // First attempt fails with network error (socket timeout)
        mockWebServer.enqueue(MockResponse().setSocketPolicy(
            okhttp3.mockwebserver.SocketPolicy.DISCONNECT_AFTER_REQUEST
        ))

        // Second attempt succeeds
        val successBody = """{
            "nodeId": "flux-node-003",
            "tier": "Nimbus",
            "status": "synced",
            "uptime": 5555555,
            "cpuUsage": 28.3,
            "memoryUsage": 55.0,
            "storageUsage": 60.0,
            "networkBandwidth": 150,
            "collateralStatus": "LOCKED",
            "benchmarkScore": 70000,
            "lastSeen": 1700000000000
        }"""
        mockWebServer.enqueue(MockResponse().setBody(successBody))

        val endpoint = mockWebServer.hostName + ":" + mockWebServer.port
        val observation = FluxNodeMonitor.getNodeHealth("flux-node-003", endpoint)

        assertNotNull(observation)
        observation?.let { obs ->
            assertEquals("flux-node-003", obs.nodeMetrics.nodeId)
            assertEquals("Nimbus", obs.nodeMetrics.tier)
        }
    }

    @Test
    fun testEmptyResponseHandling() = runBlocking {
        // First attempt returns empty response
        mockWebServer.enqueue(MockResponse().setBody(""))

        // Second attempt succeeds
        val successBody = """{
            "nodeId": "flux-node-004",
            "tier": "Cumulus",
            "status": "synced",
            "uptime": 7777777,
            "cpuUsage": 35.0,
            "memoryUsage": 52.0,
            "storageUsage": 68.0,
            "networkBandwidth": 120,
            "collateralStatus": "LOCKED",
            "benchmarkScore": 80000,
            "lastSeen": 1700000000000
        }"""
        mockWebServer.enqueue(MockResponse().setBody(successBody))

        val endpoint = mockWebServer.hostName + ":" + mockWebServer.port
        val observation = FluxNodeMonitor.getNodeHealth("flux-node-004", endpoint)

        assertNotNull(observation)
        observation?.let { obs ->
            assertEquals("flux-node-004", obs.nodeMetrics.nodeId)
            assertEquals(80000L, obs.nodeMetrics.benchmarkScore)
        }
    }

    @Test
    fun testPartialJsonHandling() = runBlocking {
        // Response with missing fields (should use defaults)
        val incompleteBody = """{
            "nodeId": "flux-node-005",
            "tier": "Stratus",
            "status": "synced"
        }"""

        mockWebServer.enqueue(MockResponse().setBody(incompleteBody))

        val endpoint = mockWebServer.hostName + ":" + mockWebServer.port
        val observation = FluxNodeMonitor.getNodeHealth("flux-node-005", endpoint)

        assertNotNull(observation)
        observation?.let { obs ->
            assertEquals("flux-node-005", obs.nodeMetrics.nodeId)
            assertEquals("Stratus", obs.nodeMetrics.tier)
            // Missing fields should use defaults
            assertEquals(0L, obs.nodeMetrics.uptime)
            assertEquals(0.0, obs.nodeMetrics.cpuUsage, 0.01)
            assertEquals("UNKNOWN", obs.nodeMetrics.collateralStatus)
        }
    }

    @Test
    fun testStaleDataRecovery() = runBlocking {
        // Create a stale observation
        val staleObs = FluxObservation(
            source = "operator-flux-node",
            sourceSha256 = "stale-digest",
            endpoint = "192.168.1.100:16110",
            nodeMetrics = NodeMetrics(
                nodeId = "flux-node-001",
                tier = "Cumulus",
                benchmarkScore = 80000,
                uptime = 100000,
                cpuUsage = 30.0,
                memoryUsage = 50.0,
                storageUsage = 70.0,
                networkBandwidth = 1000,
                collateralStatus = "LOCKED"
            ),
            staleFlag = false
        )

        // Mark as stale
        val staleFlagged = FluxNodeMonitor.handleStaleData(staleObs)

        assertTrue(staleFlagged.staleFlag)
        assertEquals(staleObs.nodeMetrics.nodeId, staleFlagged.nodeMetrics.nodeId)
    }

    @Test
    fun testExponentialBackoffTiming() = runBlocking {
        // All attempts fail
        mockWebServer.enqueue(MockResponse().setResponseCode(500))
        mockWebServer.enqueue(MockResponse().setResponseCode(500))
        mockWebServer.enqueue(MockResponse().setResponseCode(500))

        val startTime = System.currentTimeMillis()

        val endpoint = mockWebServer.hostName + ":" + mockWebServer.port
        FluxNodeMonitor.getNodeHealth("flux-node-backoff", endpoint)

        val elapsedTime = System.currentTimeMillis() - startTime

        // Should have delays: 1000ms + 2000ms = 3000ms minimum
        // Allow some tolerance for execution time
        assertTrue("Elapsed time should be at least 2500ms (allowing some variance), was $elapsedTime",
            elapsedTime >= 2500)
    }

    @Test
    fun testCachePreservationOnError() = runBlocking {
        val responseBody = """{
            "nodeId": "flux-node-cache",
            "tier": "Cumulus",
            "status": "synced",
            "uptime": 1000000,
            "cpuUsage": 40.0,
            "memoryUsage": 60.0,
            "storageUsage": 75.0,
            "networkBandwidth": 100,
            "collateralStatus": "LOCKED",
            "benchmarkScore": 85000,
            "lastSeen": 1700000000000
        }"""

        // First call succeeds
        mockWebServer.enqueue(MockResponse().setBody(responseBody))

        val endpoint = mockWebServer.hostName + ":" + mockWebServer.port
        val firstCall = FluxNodeMonitor.getNodeHealth("flux-node-cache", endpoint)
        assertNotNull(firstCall)

        // Second call fails
        mockWebServer.enqueue(MockResponse().setResponseCode(500))
        mockWebServer.enqueue(MockResponse().setResponseCode(500))
        mockWebServer.enqueue(MockResponse().setResponseCode(500))

        val secondCall = FluxNodeMonitor.getNodeHealth("flux-node-cache", endpoint)

        // Should return cached data on failure
        assertNotNull(secondCall)
        secondCall?.let { obs ->
            assertEquals("flux-node-cache", obs.nodeMetrics.nodeId)
        }
    }
}
