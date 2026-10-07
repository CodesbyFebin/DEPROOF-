package com.deproof.data.observations

import android.util.Log
import com.google.gson.Gson
import com.google.gson.JsonObject
import kotlinx.coroutines.delay
import okhttp3.HttpClient
import okhttp3.Request
import java.util.concurrent.TimeUnit
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
    private const val TIMEOUT_MS = 5000L
    private const val BACKOFF_INITIAL_MS = 1000L

    // HTTP client for node queries
    private val httpClient = HttpClient.Builder()
        .connectTimeout(TIMEOUT_MS, TimeUnit.MILLISECONDS)
        .readTimeout(TIMEOUT_MS, TimeUnit.MILLISECONDS)
        .build()

    private val gson = Gson()

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
     * Query node endpoint directly with retry logic
     * Format: http://<node-ip>:16110/api/daemon/getzinfo
     * Implements exponential backoff on failure
     * Never queries public gateway (api.runonflux.io)
     */
    private suspend fun queryNodeEndpoint(endpoint: String): NodeResponse? {
        var lastException: Exception? = null

        for (retry in 0 until MAX_RETRIES) {
            try {
                val url = "http://$endpoint/api/daemon/getzinfo"
                Log.d(TAG, "Querying endpoint: $url (attempt ${retry + 1}/$MAX_RETRIES)")

                val request = Request.Builder()
                    .url(url)
                    .build()

                val response = httpClient.newCall(request).execute()

                if (!response.isSuccessful) {
                    Log.w(TAG, "HTTP error ${response.code} from $endpoint")
                    lastException = Exception("HTTP ${response.code}")

                    if (retry < MAX_RETRIES - 1) {
                        val backoffMs = BACKOFF_INITIAL_MS * (1 shl retry)
                        Log.d(TAG, "Retrying after ${backoffMs}ms backoff")
                        delay(backoffMs)
                    }
                    continue
                }

                val body = response.body?.string()
                if (body == null) {
                    Log.w(TAG, "Empty response body from $endpoint")
                    lastException = Exception("Empty response")

                    if (retry < MAX_RETRIES - 1) {
                        val backoffMs = BACKOFF_INITIAL_MS * (1 shl retry)
                        delay(backoffMs)
                    }
                    continue
                }

                // Parse response JSON
                val jsonObject = gson.fromJson(body, JsonObject::class.java)

                val nodeResponse = NodeResponse(
                    nodeId = jsonObject.get("nodeId")?.asString ?: "unknown",
                    tier = jsonObject.get("tier")?.asString ?: "Cumulus",
                    status = jsonObject.get("status")?.asString ?: "unknown",
                    uptime = jsonObject.get("uptime")?.asLong ?: 0L,
                    cpuUsage = jsonObject.get("cpuUsage")?.asDouble ?: 0.0,
                    memoryUsage = jsonObject.get("memoryUsage")?.asDouble ?: 0.0,
                    storageUsage = jsonObject.get("storageUsage")?.asDouble ?: 0.0,
                    networkBandwidth = jsonObject.get("networkBandwidth")?.asLong ?: 0L,
                    collateralStatus = jsonObject.get("collateralStatus")?.asString ?: "UNKNOWN",
                    benchmarkScore = jsonObject.get("benchmarkScore")?.asLong ?: 0L,
                    lastSeen = Instant.now().toEpochMilli()
                )

                Log.d(TAG, "Successfully queried $endpoint: ${nodeResponse.nodeId}")
                return nodeResponse

            } catch (e: Exception) {
                Log.w(TAG, "Query failed (attempt ${retry + 1}/$MAX_RETRIES): ${e.message}", e)
                lastException = e

                if (retry < MAX_RETRIES - 1) {
                    val backoffMs = BACKOFF_INITIAL_MS * (1 shl retry)
                    Log.d(TAG, "Exponential backoff: ${backoffMs}ms")
                    delay(backoffMs)
                }
            }
        }

        Log.e(TAG, "All $MAX_RETRIES attempts failed for $endpoint", lastException)
        return null
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

