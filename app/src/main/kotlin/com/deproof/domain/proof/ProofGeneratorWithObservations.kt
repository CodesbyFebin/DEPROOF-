package com.deproof.domain.proof

import android.util.Log
import com.deproof.data.observations.Observation
import java.security.MessageDigest
import java.security.SecureRandom
import java.util.UUID

/**
 * Generates cryptographic proofs from aggregated observations.
 * Supports multi-source evidence: AIOZ, Flux, future providers.
 * Each proof binds: deviceId + observations + timestamp + random nonce.
 */
interface IProofGenerator {
    suspend fun generateProof(
        deviceId: String,
        observations: List<Observation>
    ): Result<GeneratedProof>
}

/**
 * Production proof generator implementing complete message binding
 */
object ProofGeneratorWithObservations : IProofGenerator {
    private const val TAG = "ProofGenerator"
    private const val PROOF_VERSION = "deproof-proof-v1"

    override suspend fun generateProof(
        deviceId: String,
        observations: List<Observation>
    ): Result<GeneratedProof> {
        return try {
            Log.d(TAG, "Generating proof for $deviceId with ${observations.size} observations")

            if (deviceId.isBlank()) {
                return Result.Error(IllegalArgumentException("Device ID cannot be blank"))
            }

            if (observations.isEmpty()) {
                return Result.Error(IllegalArgumentException("Must provide at least one observation"))
            }

            // Create proof input with complete binding
            val proofInput = ProofInput(
                deviceId = deviceId,
                timestamp = System.currentTimeMillis(),
                randomNonce = generateRandomNonce(),
                observations = observations
            )

            // Serialize complete message
            val completeMessage = serializeProofInput(proofInput)

            // Compute message hash
            val messageHash = computeMessageHash(completeMessage)

            // Create proof record (immutable after creation)
            val proof = GeneratedProof(
                proofId = UUID.randomUUID().toString(),
                version = PROOF_VERSION,
                deviceId = deviceId,
                timestamp = proofInput.timestamp,
                messageHash = messageHash,
                completeMessageBinding = completeMessage,
                observationCount = observations.size,
                providers = observations.map { it.provider }.distinct().sorted(),
                observationDigests = observations.map { it.sourceSha256 },
                status = ProofStatus.GENERATED,
                createdAt = System.currentTimeMillis()
            )

            Log.d(TAG, "Proof generated: ${proof.proofId} with hash ${proof.messageHash.take(16)}...")
            Result.Success(proof)

        } catch (e: Exception) {
            Log.e(TAG, "Proof generation failed: ${e.message}", e)
            Result.Error(e)
        }
    }

    private fun generateRandomNonce(): ByteArray {
        val nonce = ByteArray(32)
        SecureRandom().nextBytes(nonce)
        return nonce
    }

    private fun serializeProofInput(input: ProofInput): ByteArray {
        // Create deterministic serialization for complete message binding
        val parts = mutableListOf(
            "deviceId=${input.deviceId}",
            "timestamp=${input.timestamp}",
            "nonce=${input.randomNonce.joinToString("") { "%02x".format(it) }}"
        )

        input.observations.forEach { obs ->
            parts.add("provider=${obs.provider};digest=${obs.sourceSha256.take(32)}")
        }

        val serialized = parts.joinToString("\n")
        return serialized.toByteArray(Charsets.UTF_8)
    }

    private fun computeMessageHash(message: ByteArray): String {
        val digest = MessageDigest.getInstance("SHA-256").digest(message)
        return digest.joinToString("") { "%02x".format(it) }
    }
}

/**
 * Input to proof generation (immutable binding)
 */
data class ProofInput(
    val deviceId: String,
    val timestamp: Long,
    val randomNonce: ByteArray,
    val observations: List<Observation>
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is ProofInput) return false
        return deviceId == other.deviceId &&
                timestamp == other.timestamp &&
                randomNonce.contentEquals(other.randomNonce) &&
                observations == other.observations
    }

    override fun hashCode(): Int {
        var result = deviceId.hashCode()
        result = 31 * result + timestamp.hashCode()
        result = 31 * result + randomNonce.contentHashCode()
        result = 31 * result + observations.hashCode()
        return result
    }
}

/**
 * Generated proof with complete message binding
 * Immutable after creation - modifications create new proof with new hash
 */
data class GeneratedProof(
    val proofId: String,
    val version: String,
    val deviceId: String,
    val timestamp: Long,
    val messageHash: String,
    val completeMessageBinding: ByteArray,
    val observationCount: Int,
    val providers: List<String>,  // Sorted, unique provider names
    val observationDigests: List<String>,  // SHA256 of each observation
    val status: ProofStatus,
    val createdAt: Long,
    val signature: String? = null  // Populated after wallet signing
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is GeneratedProof) return false
        return proofId == other.proofId &&
                version == other.version &&
                deviceId == other.deviceId &&
                timestamp == other.timestamp &&
                messageHash == other.messageHash &&
                completeMessageBinding.contentEquals(other.completeMessageBinding) &&
                observationCount == other.observationCount &&
                providers == other.providers &&
                observationDigests == other.observationDigests &&
                status == other.status &&
                signature == other.signature
    }

    override fun hashCode(): Int {
        var result = proofId.hashCode()
        result = 31 * result + version.hashCode()
        result = 31 * result + deviceId.hashCode()
        result = 31 * result + timestamp.hashCode()
        result = 31 * result + messageHash.hashCode()
        result = 31 * result + completeMessageBinding.contentHashCode()
        result = 31 * result + observationCount
        result = 31 * result + providers.hashCode()
        result = 31 * result + observationDigests.hashCode()
        result = 31 * result + status.hashCode()
        result = 31 * result + (signature?.hashCode() ?: 0)
        return result
    }
}

enum class ProofStatus {
    GENERATED,          // Proof created, ready for review
    SIGNED,             // Wallet signature added
    SUBMITTED,          // Submitted to chain
    CONFIRMED,          // On-chain confirmed
    FAILED,             // Submission failed
    REJECTED            // User rejected signing
}

/**
 * Aggregated observations for multi-source proof generation
 */
data class AggregatedObservations(
    val deviceId: String,
    val observations: List<Observation>,
    val createdAt: Long = System.currentTimeMillis()
) {
    val observationCount: Int get() = observations.size
    val providers: Set<String> get() = observations.map { it.provider }.toSet()
    val hasMultipleSources: Boolean get() = providers.size > 1

    fun filterByProvider(provider: String): List<Observation> =
        observations.filter { it.provider == provider }
}

/**
 * Result type for proof operations
 */
sealed class Result<out T> {
    data class Success<T>(val data: T) : Result<T>()
    data class Error(val exception: Exception) : Result<Nothing>()

    fun getOrNull(): T? = when (this) {
        is Success -> data
        is Error -> null
    }

    fun isSuccess(): Boolean = this is Success
    fun isError(): Boolean = this is Error
}
