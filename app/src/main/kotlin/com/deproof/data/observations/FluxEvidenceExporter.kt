package com.deproof.data.observations

import android.util.Log
import org.json.JSONObject
import java.time.Instant
import java.time.format.DateTimeFormatter

interface IFluxEvidenceExporter {
    fun exportAsJson(observation: FluxObservation): String
    fun exportAsCSV(observations: List<FluxObservation>): String
    fun exportWithDisclaimer(observation: FluxObservation): String
}

/**
 * Flux observation evidence exporter
 * Exports timestamped observations with clear attribution and digest tracking
 * Emphasizes: node observation (not independent proof claim)
 */
object FluxEvidenceExporter : IFluxEvidenceExporter {
    private const val TAG = "FluxEvidenceExporter"

    /**
     * Export observation as JSON with clear labeling
     * Includes: endpoint, digest, timestamp, audit trail notice
     */
    override fun exportAsJson(observation: FluxObservation): String {
        return try {
            val json = JSONObject()

            // Observation metadata
            json.put("type", "flux-node-observation")
            json.put("provider", observation.provider)
            json.put("nodeId", observation.nodeMetrics.nodeId)
            json.put("endpoint", observation.endpoint)
            json.put("timestamp", observation.timestamp)
            json.put("timestampISO", Instant.ofEpochMilli(observation.timestamp)
                .format(DateTimeFormatter.ISO_INSTANT))

            // Audit trail
            json.put("audit", JSONObject().apply {
                put("sourceDigest", observation.sourceSha256)
                put("digestAlgorithm", "SHA-256")
                put("responseDigest", observation.nodeSha256)
                put("enablesVerification", "Endpoint + digest + timestamp enable audit trail verification")
            })

            // Node metrics
            val metrics = JSONObject()
            metrics.put("tier", observation.nodeMetrics.tier)
            metrics.put("benchmarkScore", observation.nodeMetrics.benchmarkScore)
            metrics.put("uptime_seconds", observation.nodeMetrics.uptime)
            metrics.put("cpuUsage_percent", observation.nodeMetrics.cpuUsage)
            metrics.put("memoryUsage_percent", observation.nodeMetrics.memoryUsage)
            metrics.put("storageUsage_percent", observation.nodeMetrics.storageUsage)
            metrics.put("networkBandwidth_mbps", observation.nodeMetrics.networkBandwidth)
            metrics.put("collateralStatus", observation.nodeMetrics.collateralStatus)
            json.put("nodeMetrics", metrics)

            // Asset tracking (separate ledgers)
            val rewards = JSONObject()
            rewards.put("fluxReward", JSONObject().apply {
                put("asset", "FLUX")
                put("blockchain", "Flux")
                put("status", observation.rewardStatus)
                put("note", "Native Flux blockchain rewards")
            })
            rewards.put("skrPayment", JSONObject().apply {
                put("asset", "SKR")
                put("blockchain", "Solana")
                put("status", observation.skrPaymentStatus)
                put("note", "Solana blockchain payment - separate ledger, not conflated with FLUX rewards")
            })
            json.put("rewardTracking", rewards)

            // Assurance and verification status
            json.put("assurance", JSONObject().apply {
                put("level", observation.assurance)
                put("definition", "Device-reported metrics from operator's node endpoint")
                put("verification", observation.independentVerification)
            })

            // Disclaimer
            json.put("disclaimer", getDisclaimer())

            Log.d(TAG, "JSON export created for node: ${observation.nodeMetrics.nodeId}")
            json.toString(2)

        } catch (e: Exception) {
            Log.e(TAG, "JSON export failed: ${e.message}", e)
            "{\"error\": \"${e.message}\"}"
        }
    }

    /**
     * Export multiple observations as CSV time-series
     * Header: timestamp, nodeId, tier, cpuUsage, memoryUsage, storageUsage, uptime, endpoint, digest
     */
    override fun exportAsCSV(observations: List<FluxObservation>): String {
        return try {
            val sb = StringBuilder()

            // Header
            sb.append("timestamp,nodeId,tier,cpuUsage(%),memory(%),storage(%),")
            sb.append("uptime(s),benchmarkScore,collateralStatus,endpoint,sourceDigest\n")

            // Data rows
            observations.forEach { obs ->
                val timestamp = Instant.ofEpochMilli(obs.timestamp)
                    .format(DateTimeFormatter.ISO_INSTANT)
                val metrics = obs.nodeMetrics

                sb.append("$timestamp,")
                sb.append("${metrics.nodeId},")
                sb.append("${metrics.tier},")
                sb.append("${metrics.cpuUsage},")
                sb.append("${metrics.memoryUsage},")
                sb.append("${metrics.storageUsage},")
                sb.append("${metrics.uptime},")
                sb.append("${metrics.benchmarkScore},")
                sb.append("${metrics.collateralStatus},")
                sb.append("${obs.endpoint},")
                sb.append("${obs.sourceSha256.take(16)}...\n")
            }

            Log.d(TAG, "CSV export created: ${observations.size} observations")
            sb.toString()

        } catch (e: Exception) {
            Log.e(TAG, "CSV export failed: ${e.message}", e)
            "error,${e.message}\n"
        }
    }

    /**
     * Export with full disclaimer explaining what this observation represents
     */
    override fun exportWithDisclaimer(observation: FluxObservation): String {
        return """
            ${getDisclaimer()}

            OBSERVATION DATA:
            ${exportAsJson(observation)}
        """.trimIndent()
    }

    /**
     * Get full disclaimer text
     */
    private fun getDisclaimer(): String {
        return """
            DISCLAIMER:
            This is a node health observation from Flux.
            It does NOT constitute independent proof of contribution.
            It provides operator-owned metrics from their node endpoint only.

            AUDIT TRAIL:
            - Endpoint: The specific Flux node queried
            - Response Digest: SHA256 of raw node response
            - Timestamp: UTC time of observation
            These enable verification of what was observed and when.

            ASSET SEPARATION:
            - FLUX rewards: Native Flux blockchain asset
            - SKR payments: Solana blockchain asset (separate ledger)
            These are tracked independently and never conflated.

            VERIFICATION STATUS: LOCAL_OBSERVATION (device-reported, not yet verified on-chain)
        """.trimIndent()
    }
}

/**
 * Example observation export structure
 */
object FluxEvidenceExample {
    val exampleObservation = FluxObservation(
        schema = "deproof-flux-observation-v1",
        provider = "Flux",
        source = "operator-flux-node",
        timestamp = System.currentTimeMillis(),
        assurance = "LOCAL_OBSERVATION",
        sourceSha256 = "e908a7da76643f008927ce7e28170d05bd25e6f94f33cad8fed853d560a6d6ea",
        endpoint = "192.168.1.100:16110",
        rewardAsset = "FLUX",
        rewardStatus = "PENDING",
        nodeMetrics = NodeMetrics(
            nodeId = "flux-node-001",
            tier = "Cumulus",
            benchmarkScore = 85000,
            uptime = 1234567,
            cpuUsage = 45.2,
            memoryUsage = 62.8,
            storageUsage = 78.5,
            networkBandwidth = 100,
            collateralStatus = "LOCKED"
        ),
        nodeSha256 = "a1b2c3d4e5f6g7h8i9j0k1l2m3n4o5p6"
    )

    fun printExample() {
        val json = FluxEvidenceExporter.exportAsJson(exampleObservation)
        Log.d("FluxEvidenceExample", json)
    }
}
