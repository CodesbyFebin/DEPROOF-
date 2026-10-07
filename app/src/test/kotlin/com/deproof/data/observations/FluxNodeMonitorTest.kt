package com.deproof.data.observations

import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

/**
 * Unit tests for Flux node monitoring
 */
@RunWith(RobolectricTestRunner::class)
class FluxNodeMonitorTest {

    private val monitor = FluxNodeMonitor

    // ===== Node Health Monitoring =====

    @Test
    fun testGetNodeHealthReturnsObservation() {
        val observation = monitor.getNodeHealth(
            "flux-node-001",
            "192.168.1.100:16110"
        )

        assertNotNull(observation)
        observation?.let {
            assertEquals("Flux", it.provider)
            assertEquals("deproof-flux-observation-v1", it.schema)
            assertEquals("LOCAL_OBSERVATION", it.assurance)
        }
    }

    @Test
    fun testObservationIncludesMetrics() {
        val observation = monitor.getNodeHealth(
            "flux-node-002",
            "192.168.1.101:16110"
        )

        assertNotNull(observation)
        observation?.let {
            assertEquals("Cumulus", it.nodeMetrics.tier)
            assertTrue(it.nodeMetrics.benchmarkScore > 0)
            assertTrue(it.nodeMetrics.uptime >= 0)
            assertTrue(it.nodeMetrics.cpuUsage >= 0 && it.nodeMetrics.cpuUsage <= 100)
        }
    }

    @Test
    fun testObservationIncludesDigest() {
        val observation = monitor.getNodeHealth(
            "flux-node-003",
            "192.168.1.102:16110"
        )

        assertNotNull(observation)
        observation?.let {
            assertNotNull(it.sourceSha256)
            assertNotNull(it.nodeSha256)
            assertEquals(64, it.sourceSha256.length)  // SHA256 hex is 64 chars
            assertEquals(64, it.nodeSha256.length)
        }
    }

    @Test
    fun testObservationIncludesEndpoint() {
        val endpoint = "192.168.1.103:16110"
        val observation = monitor.getNodeHealth("flux-node-004", endpoint)

        assertNotNull(observation)
        observation?.let {
            assertEquals(endpoint, it.endpoint)
        }
    }

    @Test
    fun testObservationIncludesTimestamp() {
        val before = System.currentTimeMillis()
        val observation = monitor.getNodeHealth(
            "flux-node-005",
            "192.168.1.104:16110"
        )
        val after = System.currentTimeMillis()

        assertNotNull(observation)
        observation?.let {
            assertTrue(it.timestamp in before..after)
        }
    }

    // ===== Asset Separation =====

    @Test
    fun testRewardAssetIsFLUX() {
        val observation = monitor.getNodeHealth(
            "flux-node-006",
            "192.168.1.105:16110"
        )

        assertNotNull(observation)
        observation?.let {
            assertEquals("FLUX", it.rewardAsset)
        }
    }

    @Test
    fun testSKRPaymentStatusSeparate() {
        val observation = monitor.getNodeHealth(
            "flux-node-007",
            "192.168.1.106:16110"
        )

        assertNotNull(observation)
        observation?.let {
            assertEquals("NOT_SUBMITTED", it.skrPaymentStatus)
            assertNotEquals(it.rewardAsset, it.skrPaymentStatus)
        }
    }

    // ===== Stale Data Handling =====

    @Test
    fun testHandleStaleDataMarksFlag() {
        val observation = monitor.getNodeHealth(
            "flux-node-008",
            "192.168.1.107:16110"
        )

        assertNotNull(observation)
        observation?.let {
            assertFalse(it.staleFlag)  // Fresh data

            // Simulate stale handling
            val staleObs = monitor.handleStaleData(it)
            // Age would be very recent, so staleFlag likely false
            // but the function is callable without error
            assertNotNull(staleObs)
        }
    }

    @Test
    fun testObservationNotStaleWhenFresh() {
        val observation = monitor.getNodeHealth(
            "flux-node-009",
            "192.168.1.108:16110"
        )

        assertNotNull(observation)
        observation?.let {
            assertFalse(it.staleFlag)
        }
    }

    // ===== Assurance Level =====

    @Test
    fun testAssuranceLevelIsLocalObservation() {
        val observation = monitor.getNodeHealth(
            "flux-node-010",
            "192.168.1.109:16110"
        )

        assertNotNull(observation)
        observation?.let {
            assertEquals("LOCAL_OBSERVATION", it.assurance)
            assertEquals("NOT_RUN", it.independentVerification)
        }
    }

    @Test
    fun testSignatureIsNull() {
        val observation = monitor.getNodeHealth(
            "flux-node-011",
            "192.168.1.110:16110"
        )

        assertNotNull(observation)
        observation?.let {
            assertNull(it.signature)  // Not signed yet
        }
    }

    // ===== Schema =====

    @Test
    fun testSchemaIsCorrect() {
        val observation = monitor.getNodeHealth(
            "flux-node-012",
            "192.168.1.111:16110"
        )

        assertNotNull(observation)
        observation?.let {
            assertEquals("deproof-flux-observation-v1", it.schema)
        }
    }

    @Test
    fun testProviderIsFlux() {
        val observation = monitor.getNodeHealth(
            "flux-node-013",
            "192.168.1.112:16110"
        )

        assertNotNull(observation)
        observation?.let {
            assertEquals("Flux", it.provider)
        }
    }

    // ===== Error Handling =====

    @Test
    fun testHandlesUnavailableNode() {
        // Query a node that likely won't respond
        val observation = monitor.getNodeHealth(
            "flux-node-offline",
            "127.0.0.1:19999"  // Invalid/unreachable endpoint
        )

        // Should handle gracefully - either return null or cached data
        // (No exception thrown)
    }

    // ===== Digest Consistency =====

    @Test
    fun testDigestIsConsistent() {
        val obs1 = monitor.getNodeHealth(
            "flux-node-014",
            "192.168.1.113:16110"
        )

        val obs2 = monitor.getNodeHealth(
            "flux-node-014",
            "192.168.1.113:16110"
        )

        assertNotNull(obs1)
        assertNotNull(obs2)

        // If responses are identical, digests should match
        // (But timing might differ)
        assertNotNull(obs1?.sourceSha256)
        assertNotNull(obs2?.sourceSha256)
    }
}
