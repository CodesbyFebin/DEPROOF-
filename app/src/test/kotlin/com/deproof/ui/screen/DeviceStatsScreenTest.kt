package com.deproof.ui.screen

import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import org.junit.Rule
import org.junit.Test
import com.deproof.data.observations.AIZObservation
import com.deproof.data.observations.AIZMetrics
import com.deproof.data.observations.FluxObservation
import com.deproof.data.observations.FluxMetrics

class DeviceStatsScreenTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    // ===== AIOZ Observation Tests =====

    @Test
    fun testAIZObservationHeaderDisplaysProvider() {
        val observation = AIZObservation(
            sourceSha256 = "abc123def456abc123def456abc123def456abc123def456abc123def456abc1",
            nodeMetrics = AIZMetrics(
                storageObjectCount = 42,
                storageSizeBytes = 1048576,
                upstreamSpeedKbps = 1024.0
            )
        )

        composeTestRule.setContent {
            DeviceStatsScreen(observation)
        }

        composeTestRule.onNodeWithText("AIOZ Observation").assertExists()
        composeTestRule.onNodeWithText("Schema: deproof-aioz-observation-v1").assertExists()
    }

    @Test
    fun testAIZMetricsDisplayCorrectly() {
        val observation = AIZObservation(
            sourceSha256 = "abc123def456abc123def456abc123def456abc123def456abc123def456abc1",
            nodeMetrics = AIZMetrics(
                storageObjectCount = 100,
                storageSizeBytes = 5242880,  // 5 MB
                upstreamSpeedKbps = 2048.5
            )
        )

        composeTestRule.setContent {
            DeviceStatsScreen(observation)
        }

        composeTestRule.onNodeWithText("Storage Objects").assertExists()
        composeTestRule.onNodeWithText("100").assertExists()
        composeTestRule.onNodeWithText("Storage Size").assertExists()
        composeTestRule.onNodeWithText("Upstream Speed").assertExists()
    }

    @Test
    fun testAssuranceLevelDisplayed() {
        val observation = AIZObservation(
            sourceSha256 = "abc123def456abc123def456abc123def456abc123def456abc123def456abc1",
            nodeMetrics = AIZMetrics(
                storageObjectCount = 1,
                storageSizeBytes = 1024,
                upstreamSpeedKbps = 100.0
            ),
            assurance = "LOCAL_OBSERVATION"
        )

        composeTestRule.setContent {
            DeviceStatsScreen(observation)
        }

        composeTestRule.onNodeWithText("Assurance").assertExists()
        composeTestRule.onNodeWithText("LOCAL_OBSERVATION").assertExists()
    }

    @Test
    fun testRewardAssetDisplayed() {
        val observation = AIZObservation(
            sourceSha256 = "abc123def456abc123def456abc123def456abc123def456abc123def456abc1",
            nodeMetrics = AIZMetrics(
                storageObjectCount = 1,
                storageSizeBytes = 1024,
                upstreamSpeedKbps = 100.0
            ),
            rewardAsset = "AIOZ"
        )

        composeTestRule.setContent {
            DeviceStatsScreen(observation)
        }

        composeTestRule.onNodeWithText("Reward Asset").assertExists()
        composeTestRule.onNodeWithText("AIOZ").assertExists()
    }

    @Test
    fun testSourceDigestPreviewDisplayed() {
        val fullDigest = "abc123def456abc123def456abc123def456abc123def456abc123def456abc1"
        val observation = AIZObservation(
            sourceSha256 = fullDigest,
            nodeMetrics = AIZMetrics(
                storageObjectCount = 1,
                storageSizeBytes = 1024,
                upstreamSpeedKbps = 100.0
            )
        )

        composeTestRule.setContent {
            DeviceStatsScreen(observation)
        }

        composeTestRule.onNodeWithText("Source Digest").assertExists()
        // Preview should show first 16 chars + "..."
        composeTestRule.onNodeWithText("abc123def456abc1...").assertExists()
    }

    @Test
    fun testStaleFlagDisplayedWhenSet() {
        val observation = AIZObservation(
            sourceSha256 = "abc123def456abc123def456abc123def456abc123def456abc123def456abc1",
            nodeMetrics = AIZMetrics(
                storageObjectCount = 1,
                storageSizeBytes = 1024,
                upstreamSpeedKbps = 100.0
            ),
            staleFlag = true
        )

        composeTestRule.setContent {
            DeviceStatsScreen(observation)
        }

        composeTestRule.onNodeWithText("Status").assertExists()
        composeTestRule.onNodeWithText("STALE (Cached)").assertExists()
    }

    // ===== Flux Observation Tests =====

    @Test
    fun testFluxObservationHeaderDisplaysProvider() {
        val observation = FluxObservation(
            sourceSha256 = "xyz789abc123xyz789abc123xyz789abc123xyz789abc123xyz789abc123xyz78",
            nodeMetrics = FluxMetrics(
                tier = "T1",
                benchmarkScore = 85,
                uptime = 86400,
                cpuUsage = 25.5,
                memoryUsage = 45.2,
                storageUsage = 60.8,
                networkBandwidth = 100,
                collateralStatus = "ACTIVE"
            )
        )

        composeTestRule.setContent {
            DeviceStatsScreen(observation)
        }

        composeTestRule.onNodeWithText("FLUX Observation").assertExists()
        composeTestRule.onNodeWithText("Schema: deproof-flux-observation-v1").assertExists()
    }

    @Test
    fun testFluxMetricsDisplayCorrectly() {
        val observation = FluxObservation(
            sourceSha256 = "xyz789abc123xyz789abc123xyz789abc123xyz789abc123xyz789abc123xyz78",
            nodeMetrics = FluxMetrics(
                tier = "T1",
                benchmarkScore = 95,
                uptime = 604800,  // 7 days
                cpuUsage = 30.0,
                memoryUsage = 50.0,
                storageUsage = 70.0,
                networkBandwidth = 1000,
                collateralStatus = "ACTIVE"
            )
        )

        composeTestRule.setContent {
            DeviceStatsScreen(observation)
        }

        composeTestRule.onNodeWithText("Node Metrics").assertExists()
        composeTestRule.onNodeWithText("Tier").assertExists()
        composeTestRule.onNodeWithText("T1").assertExists()
        composeTestRule.onNodeWithText("Benchmark Score").assertExists()
        composeTestRule.onNodeWithText("Performance").assertExists()
        composeTestRule.onNodeWithText("CPU Usage").assertExists()
        composeTestRule.onNodeWithText("Collateral Status").assertExists()
    }

    @Test
    fun testFluxStaleFlagDisplayedWhenSet() {
        val observation = FluxObservation(
            sourceSha256 = "xyz789abc123xyz789abc123xyz789abc123xyz789abc123xyz789abc123xyz78",
            nodeMetrics = FluxMetrics(
                tier = "T2",
                benchmarkScore = 70,
                uptime = 172800,
                cpuUsage = 35.5,
                memoryUsage = 55.2,
                storageUsage = 65.8,
                networkBandwidth = 500,
                collateralStatus = "ACTIVE"
            ),
            staleFlag = true
        )

        composeTestRule.setContent {
            DeviceStatsScreen(observation)
        }

        composeTestRule.onNodeWithText("STALE (Cached)").assertExists()
    }

    // ===== Metadata Display Tests =====

    @Test
    fun testTimestampDisplayed() {
        val timestamp = System.currentTimeMillis()
        val observation = AIZObservation(
            sourceSha256 = "abc123def456abc123def456abc123def456abc123def456abc123def456abc1",
            nodeMetrics = AIZMetrics(
                storageObjectCount = 1,
                storageSizeBytes = 1024,
                upstreamSpeedKbps = 100.0
            ),
            timestamp = timestamp
        )

        composeTestRule.setContent {
            DeviceStatsScreen(observation)
        }

        composeTestRule.onNodeWithText("Timestamp").assertExists()
        composeTestRule.onNodeWithText("Epoch").assertExists()
    }

    @Test
    fun testEndpointDisplayedWhenPresent() {
        val observation = AIZObservation(
            sourceSha256 = "abc123def456abc123def456abc123def456abc123def456abc123def456abc1",
            nodeMetrics = AIZMetrics(
                storageObjectCount = 1,
                storageSizeBytes = 1024,
                upstreamSpeedKbps = 100.0
            ),
            endpoint = "localhost:aiz-cli"
        )

        composeTestRule.setContent {
            DeviceStatsScreen(observation)
        }

        composeTestRule.onNodeWithText("Endpoint").assertExists()
        composeTestRule.onNodeWithText("localhost:aiz-cli").assertExists()
    }

    @Test
    fun testDisclaimerDisplayed() {
        val observation = AIZObservation(
            sourceSha256 = "abc123def456abc123def456abc123def456abc123def456abc123def456abc1",
            nodeMetrics = AIZMetrics(
                storageObjectCount = 1,
                storageSizeBytes = 1024,
                upstreamSpeedKbps = 100.0
            )
        )

        composeTestRule.setContent {
            DeviceStatsScreen(observation)
        }

        composeTestRule.onNodeWithText("does NOT constitute independent proof").assertExists()
        composeTestRule.onNodeWithText("never conflated").assertExists()
    }

    // ===== Byte Formatting Tests =====

    @Test
    fun testByteFormattingGB() {
        val observation = AIZObservation(
            sourceSha256 = "abc123def456abc123def456abc123def456abc123def456abc123def456abc1",
            nodeMetrics = AIZMetrics(
                storageObjectCount = 1,
                storageSizeBytes = 1099511627776,  // 1 TB
                upstreamSpeedKbps = 100.0
            )
        )

        composeTestRule.setContent {
            DeviceStatsScreen(observation)
        }

        composeTestRule.onNodeWithText("Storage Size").assertExists()
        composeTestRule.onNodeWithText("1024.00 GB").assertExists()
    }

    @Test
    fun testByteFormattingMB() {
        val observation = AIZObservation(
            sourceSha256 = "abc123def456abc123def456abc123def456abc123def456abc123def456abc1",
            nodeMetrics = AIZMetrics(
                storageObjectCount = 1,
                storageSizeBytes = 5242880,  // 5 MB
                upstreamSpeedKbps = 100.0
            )
        )

        composeTestRule.setContent {
            DeviceStatsScreen(observation)
        }

        composeTestRule.onNodeWithText("5.00 MB").assertExists()
    }

    @Test
    fun testByteFormattingKB() {
        val observation = AIZObservation(
            sourceSha256 = "abc123def456abc123def456abc123def456abc123def456abc123def456abc1",
            nodeMetrics = AIZMetrics(
                storageObjectCount = 1,
                storageSizeBytes = 2048,  // 2 KB
                upstreamSpeedKbps = 100.0
            )
        )

        composeTestRule.setContent {
            DeviceStatsScreen(observation)
        }

        composeTestRule.onNodeWithText("2.00 KB").assertExists()
    }

    @Test
    fun testByteFormattingBytes() {
        val observation = AIZObservation(
            sourceSha256 = "abc123def456abc123def456abc123def456abc123def456abc123def456abc1",
            nodeMetrics = AIZMetrics(
                storageObjectCount = 1,
                storageSizeBytes = 512,  // 512 B
                upstreamSpeedKbps = 100.0
            )
        )

        composeTestRule.setContent {
            DeviceStatsScreen(observation)
        }

        composeTestRule.onNodeWithText("512 B").assertExists()
    }

    // ===== SKR Payment Status Display Tests =====

    @Test
    fun testSKRPaymentStatusDisplayed() {
        val observation = AIZObservation(
            sourceSha256 = "abc123def456abc123def456abc123def456abc123def456abc123def456abc1",
            nodeMetrics = AIZMetrics(
                storageObjectCount = 1,
                storageSizeBytes = 1024,
                upstreamSpeedKbps = 100.0
            ),
            skrPaymentStatus = "NOT_SUBMITTED"
        )

        composeTestRule.setContent {
            DeviceStatsScreen(observation)
        }

        composeTestRule.onNodeWithText("SKR Payment").assertExists()
        composeTestRule.onNodeWithText("NOT_SUBMITTED").assertExists()
    }
}
