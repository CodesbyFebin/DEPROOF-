package com.deproof.data.observations

import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

/**
 * Unit tests for Flux node discovery and hardware validation
 */
@RunWith(RobolectricTestRunner::class)
class FluxDiscoveryTest {

    private val discovery = FluxDiscovery

    // ===== Node Discovery =====

    @Test
    fun testDiscoverNodeReturnsValidInfo() {
        // Test discovery of a valid Flux node
        // Note: Uses mock endpoint in this test environment
        val nodeInfo = discovery.discoverNode("192.168.1.100:16110")

        assertNotNull(nodeInfo)
        nodeInfo?.let {
            assertEquals("Cumulus", it.tier)
            assertTrue(it.cpuCores >= 2)
            assertTrue(it.ramGB >= 8)
        }
    }

    // ===== Hardware Validation: Valid Cases =====

    @Test
    fun testValidCumulusHardware() {
        val nodeInfo = FluxNodeInfo(
            nodeId = "flux-test-001",
            tier = "Cumulus",
            version = "4.2.0",
            status = "synced",
            cpuCores = 4,
            cpuThreads = 8,
            ramGB = 16,
            storageSizeGB = 500,
            bandwidthMbps = 100,
            publicIp = "203.0.113.42",
            collateralFlux = 1000,
            benchmarkScore = 85000,
            nodeEndpoint = "192.168.1.100:16110",
            timestamp = System.currentTimeMillis()
        )

        val validation = discovery.validateHardware(nodeInfo)

        assertTrue(validation.isValid)
        assertTrue(validation.violations.isEmpty())
        assertEquals("Cumulus", validation.tier)
    }

    @Test
    fun testExcessiveHardwareAlsoValid() {
        val nodeInfo = FluxNodeInfo(
            nodeId = "flux-test-002",
            tier = "Stratus",
            version = "4.2.0",
            status = "synced",
            cpuCores = 16,
            cpuThreads = 32,
            ramGB = 64,
            storageSizeGB = 2000,
            bandwidthMbps = 1000,
            publicIp = "203.0.113.43",
            collateralFlux = 5000,
            benchmarkScore = 250000,
            nodeEndpoint = "192.168.1.101:16110",
            timestamp = System.currentTimeMillis()
        )

        val validation = discovery.validateHardware(nodeInfo)

        assertTrue(validation.isValid)
        assertTrue(validation.violations.isEmpty())
        assertEquals("Stratus", validation.tier)
    }

    // ===== Hardware Validation: Failure Cases =====

    @Test
    fun testInsufficientCpuCores() {
        val nodeInfo = FluxNodeInfo(
            nodeId = "flux-test-003",
            tier = "Cumulus",
            version = "4.2.0",
            status = "synced",
            cpuCores = 1,  // Less than required 2
            cpuThreads = 2,
            ramGB = 16,
            storageSizeGB = 500,
            bandwidthMbps = 100,
            publicIp = "203.0.113.44",
            collateralFlux = 1000,
            benchmarkScore = 85000,
            nodeEndpoint = "192.168.1.102:16110",
            timestamp = System.currentTimeMillis()
        )

        val validation = discovery.validateHardware(nodeInfo)

        assertFalse(validation.isValid)
        assertTrue(validation.violations.any { it.contains("CPU cores") })
    }

    @Test
    fun testInsufficientRAM() {
        val nodeInfo = FluxNodeInfo(
            nodeId = "flux-test-004",
            tier = "Cumulus",
            version = "4.2.0",
            status = "synced",
            cpuCores = 4,
            cpuThreads = 8,
            ramGB = 4,  // Less than required 8
            storageSizeGB = 500,
            bandwidthMbps = 100,
            publicIp = "203.0.113.45",
            collateralFlux = 1000,
            benchmarkScore = 85000,
            nodeEndpoint = "192.168.1.103:16110",
            timestamp = System.currentTimeMillis()
        )

        val validation = discovery.validateHardware(nodeInfo)

        assertFalse(validation.isValid)
        assertTrue(validation.violations.any { it.contains("RAM") })
    }

    @Test
    fun testInsufficientStorage() {
        val nodeInfo = FluxNodeInfo(
            nodeId = "flux-test-005",
            tier = "Cumulus",
            version = "4.2.0",
            status = "synced",
            cpuCores = 4,
            cpuThreads = 8,
            ramGB = 16,
            storageSizeGB = 100,  // Less than required 220
            bandwidthMbps = 100,
            publicIp = "203.0.113.46",
            collateralFlux = 1000,
            benchmarkScore = 85000,
            nodeEndpoint = "192.168.1.104:16110",
            timestamp = System.currentTimeMillis()
        )

        val validation = discovery.validateHardware(nodeInfo)

        assertFalse(validation.isValid)
        assertTrue(validation.violations.any { it.contains("Storage") })
    }

    @Test
    fun testInsufficientBandwidth() {
        val nodeInfo = FluxNodeInfo(
            nodeId = "flux-test-006",
            tier = "Cumulus",
            version = "4.2.0",
            status = "synced",
            cpuCores = 4,
            cpuThreads = 8,
            ramGB = 16,
            storageSizeGB = 500,
            bandwidthMbps = 10,  // Less than required 25
            publicIp = "203.0.113.47",
            collateralFlux = 1000,
            benchmarkScore = 85000,
            nodeEndpoint = "192.168.1.105:16110",
            timestamp = System.currentTimeMillis()
        )

        val validation = discovery.validateHardware(nodeInfo)

        assertFalse(validation.isValid)
        assertTrue(validation.violations.any { it.contains("Bandwidth") })
    }

    @Test
    fun testMissingPublicIP() {
        val nodeInfo = FluxNodeInfo(
            nodeId = "flux-test-007",
            tier = "Cumulus",
            version = "4.2.0",
            status = "synced",
            cpuCores = 4,
            cpuThreads = 8,
            ramGB = 16,
            storageSizeGB = 500,
            bandwidthMbps = 100,
            publicIp = null,  // Required but missing
            collateralFlux = 1000,
            benchmarkScore = 85000,
            nodeEndpoint = "192.168.1.106:16110",
            timestamp = System.currentTimeMillis()
        )

        val validation = discovery.validateHardware(nodeInfo)

        assertFalse(validation.isValid)
        assertTrue(validation.violations.any { it.contains("Public IP") })
    }

    @Test
    fun testInsufficientCollateral() {
        val nodeInfo = FluxNodeInfo(
            nodeId = "flux-test-008",
            tier = "Cumulus",
            version = "4.2.0",
            status = "synced",
            cpuCores = 4,
            cpuThreads = 8,
            ramGB = 16,
            storageSizeGB = 500,
            bandwidthMbps = 100,
            publicIp = "203.0.113.48",
            collateralFlux = 500,  // Less than required 1000
            benchmarkScore = 85000,
            nodeEndpoint = "192.168.1.107:16110",
            timestamp = System.currentTimeMillis()
        )

        val validation = discovery.validateHardware(nodeInfo)

        assertFalse(validation.isValid)
        assertTrue(validation.violations.any { it.contains("Collateral") })
    }

    @Test
    fun testMissingBenchmarkScore() {
        val nodeInfo = FluxNodeInfo(
            nodeId = "flux-test-009",
            tier = "Cumulus",
            version = "4.2.0",
            status = "synced",
            cpuCores = 4,
            cpuThreads = 8,
            ramGB = 16,
            storageSizeGB = 500,
            bandwidthMbps = 100,
            publicIp = "203.0.113.49",
            collateralFlux = 1000,
            benchmarkScore = null,  // Required but missing
            nodeEndpoint = "192.168.1.108:16110",
            timestamp = System.currentTimeMillis()
        )

        val validation = discovery.validateHardware(nodeInfo)

        assertFalse(validation.isValid)
        assertTrue(validation.violations.any { it.contains("Benchmark") })
    }

    @Test
    fun testMultipleViolations() {
        val nodeInfo = FluxNodeInfo(
            nodeId = "flux-test-010",
            tier = "Cumulus",
            version = "4.2.0",
            status = "synced",
            cpuCores = 1,    // Violation 1
            cpuThreads = 2,
            ramGB = 4,       // Violation 2
            storageSizeGB = 100,  // Violation 3
            bandwidthMbps = 10,   // Violation 4
            publicIp = null,      // Violation 5
            collateralFlux = 500, // Violation 6
            benchmarkScore = null,  // Violation 7
            nodeEndpoint = "192.168.1.109:16110",
            timestamp = System.currentTimeMillis()
        )

        val validation = discovery.validateHardware(nodeInfo)

        assertFalse(validation.isValid)
        assertTrue(validation.violations.size >= 6)
    }

    // ===== Tier-Specific Validation =====

    @Test
    fun testValidationPreservesTierInfo() {
        val nodeInfo = FluxNodeInfo(
            nodeId = "flux-test-011",
            tier = "Nimbus",
            version = "4.2.0",
            status = "synced",
            cpuCores = 4,
            cpuThreads = 8,
            ramGB = 16,
            storageSizeGB = 500,
            bandwidthMbps = 100,
            publicIp = "203.0.113.50",
            collateralFlux = 1000,
            benchmarkScore = 85000,
            nodeEndpoint = "192.168.1.110:16110",
            timestamp = System.currentTimeMillis()
        )

        val validation = discovery.validateHardware(nodeInfo)

        assertEquals("Nimbus", validation.tier)
    }

    @Test
    fun testValidationIncludesTimestamp() {
        val before = System.currentTimeMillis()

        val nodeInfo = FluxNodeInfo(
            nodeId = "flux-test-012",
            tier = "Cumulus",
            version = "4.2.0",
            status = "synced",
            cpuCores = 4,
            cpuThreads = 8,
            ramGB = 16,
            storageSizeGB = 500,
            bandwidthMbps = 100,
            publicIp = "203.0.113.51",
            collateralFlux = 1000,
            benchmarkScore = 85000,
            nodeEndpoint = "192.168.1.111:16110",
            timestamp = System.currentTimeMillis()
        )

        val validation = discovery.validateHardware(nodeInfo)

        val after = System.currentTimeMillis()

        assertTrue(validation.qualifiedTime in before..after)
    }
}
