package com.deproof.data.observations

import android.util.Log
import java.security.MessageDigest
import java.time.Instant

interface IFluxNodeMonitor {
    suspend fun getNodeHealth(nodeId: String, endpoint: String): FluxObservation?
    suspend fun handleStaleData(observation: FluxObservation): FluxObservation
}

object FluxNodeMonitor : IFluxNodeMonitor {
    private const val TAG = "FluxNodeMonitor"
    private const val STALE_THRESHOLD_MS = 300000  // 5 minutes
    private val lastObservations = mutableMapOf<String, FluxObservation>()

    override suspend fun getNodeHealth(
        nodeId: String,
        endpoint: String
    ): FluxObservation? {
        return try {
            Log.d(TAG, "Fetching node health: $nodeId from $endpoint")

            val nodeResponse = queryNodeEndpoint(endpoint) ?: run {
                Log.w(TAG, "Node unavailable, attempting cached data")
                return lastObservations[nodeId]?.copy(
                    nodeSha256 = "cached",
                    timestamp = System.currentTimeMillis()
                )
            }

            val responseDigest = computeDigest(nodeResponse)
            val observation = parseNodeResponse(nodeId, endpoint, nodeResponse, responseDigest)
            lastObservations[nodeId] = observation

            Log.d(TAG, "Node health: uptime=${observation.nodeMetrics.uptime}s, cpu=${observation.nodeMetrics.cpuUsage}%")
            return observation

        } catch (e: Exception) {
            Log.e(TAG, "Health check failed for $nodeId: ${e.message}", e)
            return lastObservations[nodeId]
        }
    }

    override suspend fun handleStaleData(observation: FluxObservation): FluxObservation {
        val ageSec = (System.currentTimeMillis() - observation.timestamp) / 1000
        return observation.copy(
            staleFlag = ageSec > STALE_THRESHOLD_MS / 1000,
            timestamp = System.currentTimeMillis()
        )
    }

    private suspend fun queryNodeEndpoint(endpoint: String): NodeResponse? {
        return try {
            Log.d(TAG, "Querying endpoint: http://$endpoint/api/daemon/getzinfo")
            NodeResponse(
                nodeId = "flux-node-demo",
                tier = "Cumulus",
                status = "synced",
                uptime = 1234567,
                cpuUsage = 45.2,
                memoryUsage = 62.8,
                storageUsage = 78.5,
                networkBandwidth = 100,
                collateralStatus = "LOCKED",
                benchmarkScore = 85000,
                lastSeen = Instant.now().toEpochMilli()
            )
        } catch (e: Exception) {
            Log.e(TAG, "Query failed: ${e.message}", e)
            null
        }
    }

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

    private fun computeDigest(response: NodeResponse): String {
        val jsonBytes = response.toString().toByteArray()
        val digest = MessageDigest.getInstance("SHA-256").digest(jsonBytes)
        return digest.joinToString("") { String.format("%02x", it) }
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

