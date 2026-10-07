package com.deproof.ui.screen

import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import org.junit.Rule
import org.junit.Test
import com.deproof.data.observations.AIZObservation
import com.deproof.data.observations.FluxObservation
import com.deproof.data.observations.Metrics
import com.deproof.data.observations.NodeMetrics

class DeviceStatsScreenTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    // ===== AIOZ Observation Tests =====

    @Test
    fun testAIZObservationHeaderDisplaysProvider() {
        val observation = AIZObservation(
            source = "operator-supplied-cli-stats",
            sourceSha256 = "abc123def456abc123def456abc123def456abc123def456abc123def456abc1",
            metrics = Metrics(
                storageObjectCount = 42,
                storageSizeBytes = 1048576,
                upstreamSpeedRaw = 1024
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
            source = "operator-supplied-cli-stats",
            sourceSha256 = "abc123def456abc123def456abc123def456abc123def456abc123def456abc1",
            metrics = Metrics(
                storageObjectCount = 100,
                storageSizeBytes = 5242880,
                upstreamSpeedRaw = 2048
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
            source = "operator-supplied-cli-stats",
            sourceSha256 = "abc123def456abc123def456abc123def456abc123def456abc123def456abc1",
            metrics = Metrics(
                storageObjectCount = 1,
                storageSizeBytes = 1024,
                upstreamSpeedRaw = 100
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
            source = "operator-supplied-cli-stats",
            sourceSha256 = "abc123def456abc123def456abc123def456abc123def456abc123def456abc1",
            metrics = Metrics(
                storageObjectCount = 1,
                storageSizeBytes = 1024,
                upstreamSpeedRaw = 100
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
            source = "operator-supplied-cli-stats",
            sourceSha256 = fullDigest,
            metrics = Metrics(
                storageObjectCount = 1,
                storageSizeBytes = 1024,
                upstreamSpeedRaw = 100
            )
        )

        composeTestRule.setContent {
            DeviceStatsScreen(observation)
        }

        composeTestRule.onNodeWithText("Source Digest").assertExists()
        composeTestRule.onNodeWithText("abc123def456abc1...").assertExists()
    }

    // ===== Flux Observation Tests =====

    @Test
    fun testFluxObservationHeaderDisplaysProvider() {
        val observation = FluxObservation(
            source = "operator-flux-node",
            sourceSha256 = "xyz789abc123xyz789abc123xyz789abc123xyz789abc123xyz789abc123xyz78",
            endpoint = "192.168.1.100:16127",
            nodeMetrics = NodeMetrics(
                nodeId = "flux-node-001",
                tier = "T1",
                benchmarkScore = 85,
                uptime = 86400,
                cpuUsage = 25.5,
                memoryUsage = 45.2,
                storageUsage = 60.8,
                networkBandwidth = 100,
                collateralStatus = "LOCKED"
            )
        )

        composeTestRule.setContent {
            DeviceStatsScreen(observation)
        }

        composeTestRule.onNodeWithText("Flux Observation").assertExists()
        composeTestRule.onNodeWithText("Schema: deproof-flux-observation-v1").assertExists()
    }

    @Test
    fun testFluxMetricsDisplayCorrectly() {
        val observation = FluxObservation(
            source = "operator-flux-node",
            sourceSha256 = "xyz789abc123xyz789abc123xyz789abc123xyz789abc123xyz789abc123xyz78",
            endpoint = "192.168.1.100:16127",
            nodeMetrics = NodeMetrics(
                nodeId = "flux-node-001",
                tier = "T1",
                benchmarkScore = 95,
                uptime = 604800,
                cpuUsage = 30.0,
                memoryUsage = 50.0,
                storageUsage = 70.0,
                networkBandwidth = 1000,
                collateralStatus = "LOCKED"
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

    // ===== Metadata Display Tests =====

    @Test
    fun testTimestampDisplayed() {
        val timestamp = System.currentTimeMillis()
        val observation = AIZObservation(
            source = "operator-supplied-cli-stats",
            sourceSha256 = "abc123def456abc123def456abc123def456abc123def456abc123def456abc1",
            metrics = Metrics(
                storageObjectCount = 1,
                storageSizeBytes = 1024,
                upstreamSpeedRaw = 100
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
            source = "operator-supplied-cli-stats",
            sourceSha256 = "abc123def456abc123def456abc123def456abc123def456abc123def456abc1",
            metrics = Metrics(
                storageObjectCount = 1,
                storageSizeBytes = 1024,
                upstreamSpeedRaw = 100
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
            source = "operator-supplied-cli-stats",
            sourceSha256 = "abc123def456abc123def456abc123def456abc123def456abc123def456abc1",
            metrics = Metrics(
                storageObjectCount = 1,
                storageSizeBytes = 1024,
                upstreamSpeedRaw = 100
            )
        )

        composeTestRule.setContent {
            DeviceStatsScreen(observation)
        }

        composeTestRule.onNodeWithText("does NOT constitute independent proof").assertExists()
    }

    // ===== Byte Formatting Tests =====

    @Test
    fun testByteFormattingGB() {
        val observation = AIZObservation(
            source = "operator-supplied-cli-stats",
            sourceSha256 = "abc123def456abc123def456abc123def456abc123def456abc123def456abc1",
            metrics = Metrics(
                storageObjectCount = 1,
                storageSizeBytes = 1099511627776,  // 1 TB
                upstreamSpeedRaw = 100
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
            source = "operator-supplied-cli-stats",
            sourceSha256 = "abc123def456abc123def456abc123def456abc123def456abc123def456abc1",
            metrics = Metrics(
                storageObjectCount = 1,
                storageSizeBytes = 5242880,  // 5 MB
                upstreamSpeedRaw = 100
            )
        )

        composeTestRule.setContent {
            DeviceStatsScreen(observation)
        }

        composeTestRule.onNodeWithText("5.00 MB").assertExists()
    }

    @Test
    fun testSKRPaymentStatusDisplayed() {
        val observation = AIZObservation(
            source = "operator-supplied-cli-stats",
            sourceSha256 = "abc123def456abc123def456abc123def456abc123def456abc123def456abc1",
            metrics = Metrics(
                storageObjectCount = 1,
                storageSizeBytes = 1024,
                upstreamSpeedRaw = 100
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
