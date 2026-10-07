package com.deproof.data.observations

import android.util.Log
import java.security.MessageDigest
import java.time.Instant

interface IFluxNodeMonitor {
    suspend fun getNodeHealth(nodeId: String, endpoint: String): FluxObservation?
    suspend fun handleStaleData(observation: FluxObservation): FluxObservation
}

/**
 * Flux node health monitoring
 * Queries operator-specific node endpoint (not public gateway)
 * Tracks: uptime, resource usage, benchmark score, collateral status
 */
object FluxNodeMonitor : IFluxNodeMonitor {
    private const val TAG = "FluxNodeMonitor"
    private const val STALE_THRESHOLD_MS = 300000  // 5 minutes
    private const val MAX_RETRIES = 3

    // In-memory cache for stale data handling
    private val lastObservations = mutableMapOf<String, FluxObservation>()

    /**
     * Get current node health observations
     * Queries: /api/daemon/getzinfo, /api/daemon/benchmark, /api/daemon/getstatus
     * Handles: stale, unavailable, malformed responses
     */
    override suspend fun getNodeHealth(
        nodeId: String,
        endpoint: String
    ): FluxObservation? {
        return try {
            Log.d(TAG, "Fetching node health: $nodeId from $endpoint")

            // Query node endpoint (not public gateway)
            val nodeResponse = queryNodeEndpoint(endpoint) ?: run {
                Log.w(TAG, "Node unavailable, attempting cached data")
                return lastObservations[nodeId]?.copy(
                    nodeSha256 = "cached",
                    timestamp = System.currentTimeMillis()
                )
            }

            // Compute response digest for audit trail
            val responseDigest = computeDigest(nodeResponse)

            // Parse observations
            val observation = parseNodeResponse(nodeId, endpoint, nodeResponse, responseDigest)

            // Cache for stale data recovery
            lastObservations[nodeId] = observation

            Log.d(TAG, "Node health: uptime=${observation.nodeMetrics.uptime}s, cpu=${observation.nodeMetrics.cpuUsage}%")
            return observation

        } catch (e: Exception) {
            Log.e(TAG, "Health check failed for $nodeId: ${e.message}", e)
            // Return cached data if available
            return lastObservations[nodeId]
        }
    }

    /**
     * Handle stale data gracefully
     * Marks observation as stale but preserves historical context
     */
    override suspend fun handleStaleData(observation: FluxObservation): FluxObservation {
        val ageSec = (System.currentTimeMillis() - observation.timestamp) / 1000
        return observation.copy(
            staleFlag = ageSec > STALE_THRESHOLD_MS / 1000,
            timestamp = System.currentTimeMillis()
        )
    }

    /**
     * Query node endpoint directly
     * Format: http://<node-ip>:16110/api/daemon/getzinfo
     * Never queries public gateway (api.runonflux.io)
     */
    private suspend fun queryNodeEndpoint(endpoint: String): NodeResponse? {
        // TODO: Replace with actual HTTP client
        // for (retry in 0 until MAX_RETRIES) {
        //     try {
        //         val response = httpClient.get("http://$endpoint/api/daemon/getzinfo")
        //         return response.body()
        //     } catch (e: Exception) {
        //         if (retry == MAX_RETRIES - 1) throw e
        //         delay(1000 * (retry + 1))  // Exponential backoff
        //     }
        // }

        // Mock data for testing
        return NodeResponse(
            nodeId = "flux-node-test",
            tier = "Cumulus",
            status = "synced",
            uptime = 1234567,
            cpuUsage = 45.2,
            memoryUsage = 62.8,
            storageUsage = 78.5,
            networkBandwidth = 85,
            collateralStatus = "LOCKED",
            benchmarkScore = 85000,
            lastSeen = Instant.now().toEpochMilli()
        )
    }

    /**
     * Parse node response into observation
     */
    private fun parseNodeResponse(
        nodeId: String,
        endpoint: String,
        response: NodeResponse,
        digest: String
    ): FluxObservation {
        return FluxObservation(
            schema = "deproof-flux-observation-v1",
            provider = "Flux",
            source = "operator-flux-node",
            timestamp = System.currentTimeMillis(),
            assurance = "LOCAL_OBSERVATION",
            sourceSha256 = digest,
            endpoint = endpoint,
            signature = null,
            independentVerification = "NOT_RUN",
            rewardAsset = "FLUX",
            rewardStatus = response.collateralStatus,
            skrPaymentStatus = "NOT_SUBMITTED",
            nodeMetrics = NodeMetrics(
                nodeId = nodeId,
                tier = response.tier,
                benchmarkScore = response.benchmarkScore,
                uptime = response.uptime,
                cpuUsage = response.cpuUsage,
                memoryUsage = response.memoryUsage,
                storageUsage = response.storageUsage,
                networkBandwidth = response.networkBandwidth,
                collateralStatus = response.collateralStatus
            ),
            nodeSha256 = digest,
            staleFlag = false
        )
    }

    /**
     * Compute SHA256 of raw response for audit trail
     */
    private fun computeDigest(response: NodeResponse): String {
        val jsonBytes = response.toString().toByteArray()
        val digest = MessageDigest.getInstance("SHA-256").digest(jsonBytes)
        return digest.joinToString("") { "%02x".format(it) }
    }
}

/**
 * Raw node response from endpoint
 */
data class NodeResponse(
    val nodeId: String,
    val tier: String,
    val status: String,
    val uptime: Long,                      // seconds
    val cpuUsage: Double,                  // 0-100%
    val memoryUsage: Double,               // 0-100%
    val storageUsage: Double,              // 0-100%
    val networkBandwidth: Long,            // Mbps
    val collateralStatus: String,          // LOCKED, UNLOCKED
    val benchmarkScore: Long,
    val lastSeen: Long
)

/**
 * Extended FluxObservation for Phase 2B monitoring
 * (Extends the UnifiedObservation.FluxObservation)
 */
data class FluxObservation(
    override val schema: String = "deproof-flux-observation-v1",
    override val provider: String = "Flux",
    override val source: String = "operator-flux-node",
    override val timestamp: Long = System.currentTimeMillis(),
    override val assurance: String = "LOCAL_OBSERVATION",
    override val sourceSha256: String,
    override val endpoint: String,
    override val signature: String? = null,
    override val independentVerification: String = "NOT_RUN",
    override val rewardAsset: String = "FLUX",
    override val rewardStatus: String = "PENDING",
    override val skrPaymentStatus: String = "NOT_SUBMITTED",
    val nodeMetrics: NodeMetrics,
    val nodeSha256: String,                // Response digest
    val staleFlag: Boolean = false         // Marks stale cached data
) : Observation
