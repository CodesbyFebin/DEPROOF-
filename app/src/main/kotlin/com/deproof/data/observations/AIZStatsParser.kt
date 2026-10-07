package com.deproof.data.observations

import android.util.Log
import java.security.MessageDigest

interface IStatsParser {
    suspend fun parseStats(raw: ByteArray): Observation?
}

/**
 * Parses AIOZ CLI stats output into unified Observation interface.
 * Input format: newline-separated key=value pairs from AIOZ CLI tool
 * Example:
 *   storage_object_count=42
 *   storage_size_bytes=1048576
 *   upstream_speed_kbps=1024
 */
object AIZStatsParser : IStatsParser {
    private const val TAG = "AIZStatsParser"
    private const val MAX_INPUT_SIZE = 65 * 1024  // 65KB limit
    private const val STALE_THRESHOLD_MS = 300000   // 5 minutes

    override suspend fun parseStats(raw: ByteArray): Observation? {
        return try {
            Log.d(TAG, "Parsing AIOZ stats: ${raw.size} bytes")

            if (raw.isEmpty()) {
                Log.w(TAG, "Empty input")
                return null
            }

            if (raw.size > MAX_INPUT_SIZE) {
                Log.e(TAG, "Input oversized: ${raw.size} > $MAX_INPUT_SIZE")
                return null
            }

            val stats = parseKeyValuePairs(raw)
            validateStats(stats)

            val observation = AIZObservation(
                schema = "deproof-aioz-observation-v1",
                provider = "AIOZ",
                source = "operator-supplied-cli-stats",
                timestamp = System.currentTimeMillis(),
                assurance = "LOCAL_OBSERVATION",
                sourceSha256 = computeDigest(raw),
                endpoint = "localhost:aiz-cli",
                signature = null,
                independentVerification = "NOT_RUN",
                rewardAsset = "AIOZ",
                rewardStatus = "PENDING",
                skrPaymentStatus = "NOT_SUBMITTED",
                metrics = parseMetrics(stats)
            )

            Log.d(TAG, "Parsed AIOZ observation: ${observation.nodeMetrics}")
            return observation

        } catch (e: Exception) {
            Log.e(TAG, "Parse failed: ${e.message}", e)
            return null
        }
    }

    private fun parseKeyValuePairs(raw: ByteArray): Map<String, String> {
        val text = raw.decodeToString()
        val pairs = mutableMapOf<String, String>()

        text.lines().forEach { line ->
            if (line.isNotBlank() && "=" in line) {
                val (key, value) = line.split("=", limit = 2)
                pairs[key.trim()] = value.trim()
            }
        }

        return pairs
    }

    private fun validateStats(stats: Map<String, String>) {
        val requiredKeys = setOf(
            "storage_object_count",
            "storage_size_bytes",
            "upstream_speed_kbps"
        )

        val missing = requiredKeys - stats.keys
        if (missing.isNotEmpty()) {
            throw IllegalArgumentException("Missing required fields: $missing")
        }

        // Validate each field
        stats.forEach { (key, value) ->
            when (key) {
                "storage_object_count" -> {
                    val count = value.toLongOrNull()
                        ?: throw IllegalArgumentException("Invalid storage_object_count: $value")
                    if (count < 0) throw IllegalArgumentException("storage_object_count cannot be negative: $count")
                }
                "storage_size_bytes" -> {
                    val size = value.toLongOrNull()
                        ?: throw IllegalArgumentException("Invalid storage_size_bytes: $value")
                    if (size < 0) throw IllegalArgumentException("storage_size_bytes cannot be negative: $size")
                }
                "upstream_speed_kbps" -> {
                    val speed = value.toDoubleOrNull()
                        ?: throw IllegalArgumentException("Invalid upstream_speed_kbps: $value")
                    if (speed < 0) throw IllegalArgumentException("upstream_speed_kbps cannot be negative: $speed")
                    // Allow floats for speed (e.g., "1024.5")
                }
            }
        }
    }

    private fun parseMetrics(stats: Map<String, String>): Metrics {
        val storageObjectCount = stats["storage_object_count"]!!.toLong()
        val storageSizeBytes = stats["storage_size_bytes"]!!.toLong()
        val upstreamSpeedRaw = stats["upstream_speed_kbps"]!!.toLong()

        return Metrics(
            storageObjectCount = storageObjectCount,
            storageSizeBytes = storageSizeBytes,
            upstreamSpeedRaw = upstreamSpeedRaw
        )
    }

    private fun computeDigest(data: ByteArray): String {
        val digest = MessageDigest.getInstance("SHA-256").digest(data)
        return digest.joinToString("") { "%02x".format(it) }
    }
}
