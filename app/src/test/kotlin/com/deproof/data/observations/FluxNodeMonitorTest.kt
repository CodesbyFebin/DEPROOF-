package com.deproof.data.observations

import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Test

class FluxNodeMonitorTest {

    private val monitor = FluxNodeMonitor

    @Test
    fun testGetNodeHealthReturnsObservation() = runBlocking {
        val observation = monitor.getNodeHealth("flux-node-001", "192.168.1.100:16110")
        assertNotNull(observation)
        observation?.let {
            assertEquals("Flux", it.provider)
            assertEquals("deproof-flux-observation-v1", it.schema)
            assertEquals("LOCAL_OBSERVATION", it.assurance)
        }
    }

    @Test
    fun testObservationIncludesMetrics() = runBlocking {
        val observation = monitor.getNodeHealth("flux-node-002", "192.168.1.101:16110")
        assertNotNull(observation)
        observation?.let {
            assertEquals("Cumulus", it.nodeMetrics.tier)
            assertTrue(it.nodeMetrics.benchmarkScore > 0)
            assertTrue(it.nodeMetrics.uptime >= 0)
            assertTrue(it.nodeMetrics.cpuUsage >= 0 && it.nodeMetrics.cpuUsage <= 100)
        }
    }

    @Test
    fun testObservationIncludesDigest() = runBlocking {
        val observation = monitor.getNodeHealth("flux-node-003", "192.168.1.102:16110")
        assertNotNull(observation)
        observation?.let {
            assertNotNull(it.sourceSha256)
            assertTrue(it.sourceSha256.isNotEmpty())
            assertTrue(it.sourceSha256.length == 64)
        }
    }

    @Test
    fun testObservationTimestamp() = runBlocking {
        val before = System.currentTimeMillis()
        val observation = monitor.getNodeHealth("flux-node-004", "192.168.1.103:16110")
        val after = System.currentTimeMillis()

        assertNotNull(observation)
        observation?.let {
            assertTrue(it.timestamp >= before)
            assertTrue(it.timestamp <= after)
        }
    }

    @Test
    fun testHandleStaleData() = runBlocking {
        val observation = monitor.getNodeHealth("flux-node-005", "192.168.1.104:16110")
        assertNotNull(observation)

        observation?.let {
            val staleObs = monitor.handleStaleData(it)
            assertNotNull(staleObs)
        }
    }

    @Test
    fun testMultipleCallsWithSameNode() = runBlocking {
        val obs1 = monitor.getNodeHealth("flux-node-006", "192.168.1.105:16110")
        val obs2 = monitor.getNodeHealth("flux-node-006", "192.168.1.105:16110")

        assertNotNull(obs1)
        assertNotNull(obs2)
    }

    @Test
    fun testDifferentEndpoints() = runBlocking {
        val obs1 = monitor.getNodeHealth("flux-node-007", "192.168.1.106:16110")
        val obs2 = monitor.getNodeHealth("flux-node-008", "192.168.1.107:16110")

        assertNotNull(obs1)
        assertNotNull(obs2)
    }

    @Test
    fun testNodeMetricsValues() = runBlocking {
        val observation = monitor.getNodeHealth("flux-node-009", "192.168.1.108:16110")
        assertNotNull(observation)

        observation?.let { obs ->
            assertEquals("flux-node-demo", obs.nodeMetrics.nodeId)
            assertEquals("Cumulus", obs.nodeMetrics.tier)
            assertTrue(obs.nodeMetrics.cpuUsage > 0)
            assertTrue(obs.nodeMetrics.memoryUsage > 0)
            assertTrue(obs.nodeMetrics.uptime > 0)
        }
    }

    @Test
    fun testRewardStatus() = runBlocking {
        val observation = monitor.getNodeHealth("flux-node-010", "192.168.1.109:16110")
        assertNotNull(observation)

        observation?.let {
            assertEquals("LOCKED", it.rewardStatus)
            assertEquals("FLUX", it.rewardAsset)
        }
    }
}
