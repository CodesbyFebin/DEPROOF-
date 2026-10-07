package com.deproof.data.observations

/**
 * Unified observation interface supporting multiple sources (AIOZ, Flux, etc.)
 * Enables consistent handling of device telemetry across different providers
 */
interface Observation {
    // Core metadata
    val schema: String                      // "deproof-{provider}-observation-v1"
    val provider: String                    // "AIOZ", "Flux", etc.
    val source: String                      // Where metrics came from
    val timestamp: Long                     // Observation time (UTC ms)
    val assurance: String                   // LOCAL_OBSERVATION, etc.

    // Audit trail
    val sourceSha256: String               // SHA256 of raw input/response
    val endpoint: String?                   // Optional: node endpoint queried

    // Verification status
    val signature: String?                  // Provider signature (if any)
    val independentVerification: String     // "NOT_RUN", "PENDING", "VERIFIED"

    // Rewards (keep separate per provider)
    val rewardAsset: String                // Provider-native reward asset
    val rewardStatus: String               // "PENDING", "CLAIMED", etc.
    val skrPaymentStatus: String           // Always separate from rewards
}

/**
 * AIOZ host observation - implements Observation
 * Represents metrics from AIOZ CLI stats (storage, delivery, speed)
 */
data class AIZObservation(
    override val schema: String = "deproof-aioz-observation-v1",
    override val provider: String = "AIOZ",
    override val source: String,                    // "operator-supplied-cli-stats"
    override val timestamp: Long = System.currentTimeMillis(),
    override val assurance: String = "LOCAL_OBSERVATION",
    override val sourceSha256: String,              // SHA256 of CLI output
    override val endpoint: String? = null,          // CLI on local device
    override val signature: String? = null,
    override val independentVerification: String = "NOT_RUN",
    override val rewardAsset: String = "AIOZ",
    override val rewardStatus: String = "NOT_SUBMITTED",
    override val skrPaymentStatus: String = "NOT_SUBMITTED",

    // AIOZ-specific metrics
    val metrics: Metrics,

    // Speed unit — UNVERIFIED until AIOZ documents the unit
    val speedUnit: String = "UNVERIFIED",
    // Provider signature/acknowledgement (null until provider supplies them)
    val providerAcknowledgement: String? = null,
    // Proof submission tracker (mirrors skrPaymentStatus; used by AIZStatsParser tests)
    val skrPayment: String = "NOT_SUBMITTED"
) : Observation

/**
 * AIOZ device metrics (storage + delivery)
 */
data class Metrics(
    val storageObjectCount: Long,          // Total objects stored
    val storageSizeBytes: Long,            // Total bytes stored
    val upstreamSpeedRaw: Long             // Raw upstream speed (unit unverified)
)

/**
 * Flux node observation - implements Observation
 * Represents metrics from operator's Flux node endpoint
 */
data class FluxObservation(
    override val schema: String = "deproof-flux-observation-v1",
    override val provider: String = "Flux",
    override val source: String,                    // "operator-flux-node"
    override val timestamp: Long = System.currentTimeMillis(),
    override val assurance: String = "LOCAL_OBSERVATION",
    override val sourceSha256: String,              // SHA256 of node response
    override val endpoint: String,                  // Node IP:port queried
    override val signature: String? = null,
    override val independentVerification: String = "NOT_RUN",
    override val rewardAsset: String = "FLUX",
    override val rewardStatus: String = "PENDING",
    override val skrPaymentStatus: String = "NOT_SUBMITTED",

    // Flux-specific metrics
    val nodeMetrics: NodeMetrics,
    val nodeSha256: String = "",                   // Response digest
    val staleFlag: Boolean = false                  // Marks stale cached data
) : Observation

/**
 * Flux node health metrics
 */
data class NodeMetrics(
    val nodeId: String,
    val tier: String,                      // Cumulus, Stratus, Nimbus
    val benchmarkScore: Long,
    val uptime: Long,                      // seconds
    val cpuUsage: Double,                  // 0-100%
    val memoryUsage: Double,               // 0-100%
    val storageUsage: Double,              // 0-100%
    val networkBandwidth: Long,            // Mbps
    val collateralStatus: String           // "LOCKED", "UNLOCKED"
)

/**
 * Observation aggregator for multiple sources
 * Enables proof generation over combined observations
 */
data class AggregatedObservations(
    val deviceId: String,
    val observations: List<Observation>,   // Mix of AIOZ, Flux, etc.
    val generationTime: Long = System.currentTimeMillis(),
    val observationCount: Int = observations.size,
    val providers: Set<String> = observations.map { it.provider }.toSet()
) {
    fun getByProvider(provider: String): List<Observation> =
        observations.filter { it.provider == provider }

    fun allVerified(): Boolean =
        observations.all { it.independentVerification != "NOT_RUN" }
}

/**
 * Type-safe observation factory for creating observations from different sources
 */
sealed class ObservationFactory {
    companion object {
        fun fromAIZ(
            source: String,
            sourceSha256: String,
            metrics: Metrics
        ): AIZObservation = AIZObservation(
            source = source,
            sourceSha256 = sourceSha256,
            metrics = metrics
        )

        fun fromFlux(
            source: String,
            endpoint: String,
            sourceSha256: String,
            nodeMetrics: NodeMetrics
        ): FluxObservation = FluxObservation(
            source = source,
            endpoint = endpoint,
            sourceSha256 = sourceSha256,
            nodeMetrics = nodeMetrics
        )
    }
}
