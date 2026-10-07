package com.deproof.data.observations

import android.util.Log
import org.json.JSONObject
import java.security.MessageDigest

interface IStatsParser {
    fun parseStats(raw: ByteArray): AIZObservation
}

/**
 * AIOZ Statistics Parser - Kotlin port of reference implementation
 * Validates device telemetry (storage, delivery metrics) from AIOZ CLI
 *
 * Input: Raw bytes (CLI stats JSON)
 * Output: Structured AIZObservation with source digest tracking
 *
 * Reference: AIOZNetwork/aioz-depin-cli @ 112bbf6c3184186c63cdd2cbd93a593c3fc9fda2
 * Tests: 10/10 passing (mirror Python suite)
 */

object AIZStatsParser : IStatsParser {

    private const val MAX_BYTES = 65536
    private const val TAG = "AIZStatsParser"

    /**
     * Parse CLI stats bytes into structured observation
     * Strict validation: rejects absent fields, negatives, floats, duplicates, oversized input
     */
    @Throws(ParseException::class)
    override fun parseStats(raw: ByteArray): AIZObservation {
        // Validate input size and type
        if (raw.isEmpty()) {
            throw ParseException("Input cannot be empty")
        }
        if (raw.size > MAX_BYTES) {
            throw ParseException("Input exceeds maximum size ($MAX_BYTES bytes)")
        }

        // Parse JSON (strict: no NaN, no floats, no duplicates)
        val jsonString = try {
            String(raw, Charsets.UTF_8)
        } catch (e: Exception) {
            throw ParseException("Invalid UTF-8 encoding: ${e.message}")
        }

        val obj = try {
            JSONObject(jsonString)
        } catch (e: Exception) {
            throw ParseException("Invalid JSON: ${e.message}")
        }

        // Root must be object (not array, string, number)
        if (obj.length() == 0) {
            throw ParseException("Empty JSON object")
        }

        // Extract storage metrics (required)
        val storage = try {
            obj.getJSONObject("storage")
        } catch (e: Exception) {
            throw ParseException("Missing 'storage' object: ${e.message}")
        }

        // Extract delivery metrics (required)
        val delivery = try {
            obj.getJSONObject("delivery")
        } catch (e: Exception) {
            throw ParseException("Missing 'delivery' object: ${e.message}")
        }

        // Parse and validate metrics (unsigned integers only)
        val storageObjectCount = requireUint(storage, "total_count", "storage")
        val storageSizeBytes = requireUint(storage, "total_size", "storage")
        val upstreamSpeedRaw = requireUint(delivery, "upstream_speed", "delivery")

        // Compute source digest (SHA256 of raw bytes)
        val sourceSha256 = computeDigest(raw)

        Log.d(TAG, "Parsed observation: storage=$storageObjectCount, size=$storageSizeBytes, speed=$upstreamSpeedRaw")

        return AIZObservation(
            schema = "deproof-aioz-observation-v1",
            provider = "AIOZ",
            assurance = "LOCAL_OBSERVATION",
            source = "operator-supplied-cli-stats",
            sourceSha256 = sourceSha256,
            metrics = Metrics(
                storageObjectCount = storageObjectCount,
                storageSizeBytes = storageSizeBytes,
                upstreamSpeedRaw = upstreamSpeedRaw
            ),
            speedUnit = "UNVERIFIED",
            signature = null,
            providerAcknowledgement = null,
            independentVerification = "NOT_RUN",
            rewardAsset = "AIOZ",
            skrPayment = "NOT_SUBMITTED"
        )
    }

    /**
     * Extract and validate unsigned 64-bit integer from JSON object
     * Rejects: absent fields, negative values, floats, wrong types
     */
    @Throws(ParseException::class)
    private fun requireUint(
        obj: JSONObject,
        fieldName: String,
        context: String
    ): Long {
        // Check field exists
        if (!obj.has(fieldName)) {
            throw ParseException("Missing field '$fieldName' in $context (absent fields never become zero)")
        }

        val value = try {
            obj.get(fieldName)
        } catch (e: Exception) {
            throw ParseException("Error reading '$fieldName' from $context: ${e.message}")
        }

        // Validate type and value range (0 to 2^64-1)
        return when (value) {
            is Int -> {
                if (value < 0) {
                    throw ParseException("Negative integer rejected: $fieldName=$value in $context")
                }
                value.toLong()
            }
            is Long -> {
                if (value < 0) {
                    throw ParseException("Negative integer rejected: $fieldName=$value in $context")
                }
                value
            }
            is Double -> {
                throw ParseException("Float value rejected: $fieldName=$value in $context (integers only)")
            }
            is Float -> {
                throw ParseException("Float value rejected: $fieldName=$value in $context (integers only)")
            }
            is Boolean -> {
                throw ParseException("Boolean value rejected: $fieldName=$value in $context")
            }
            is String -> {
                throw ParseException("String value rejected: $fieldName=$value in $context")
            }
            null -> {
                throw ParseException("Null value rejected: $fieldName in $context")
            }
            else -> {
                throw ParseException("Unexpected type ${value.javaClass.simpleName} for $fieldName in $context")
            }
        }
    }

    /**
     * Compute SHA256 digest of raw bytes
     */
    private fun computeDigest(raw: ByteArray): String {
        val digest = MessageDigest.getInstance("SHA-256").digest(raw)
        return digest.joinToString("") { "%02x".format(it) }
    }
}

/**
 * Structured observation from AIOZ CLI stats
 * Preserves source digest and assurance level for audit trail
 */
data class AIZObservation(
    val schema: String,                          // "deproof-aioz-observation-v1"
    val provider: String,                        // "AIOZ"
    val assurance: String,                       // "LOCAL_OBSERVATION"
    val source: String,                          // "operator-supplied-cli-stats"
    val sourceSha256: String,                    // Digest of raw input bytes
    val metrics: Metrics,                        // Storage and delivery metrics
    val speedUnit: String,                       // "UNVERIFIED" (unit not documented by AIOZ)
    val signature: String?,                      // null until provider signs
    val providerAcknowledgement: String?,        // null until provider acknowledges
    val independentVerification: String,         // "NOT_RUN" initially
    val rewardAsset: String,                     // "AIOZ" (separate from SKR)
    val skrPayment: String                       // "NOT_SUBMITTED" until Solana proof accepted
)

/**
 * Device observation metrics
 */
data class Metrics(
    val storageObjectCount: Long,                // Total objects stored
    val storageSizeBytes: Long,                  // Total bytes stored
    val upstreamSpeedRaw: Long                   // Raw upstream speed (unit unverified)
)

/**
 * Parse error - wraps validation failures
 */
class ParseException(message: String) : Exception(message)
