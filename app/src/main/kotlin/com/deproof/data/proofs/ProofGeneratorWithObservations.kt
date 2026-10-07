package com.deproof.data.proofs

import android.util.Log
import com.deproof.data.observations.AIZObservation
import com.deproof.data.observations.AIZStatsParser
import com.deproof.data.observations.IStatsParser
import com.deproof.data.observations.ParseException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * ProofGenerator with AIOZ observations integration (Phase 2A)
 *
 * Generates zero-knowledge proofs that include host telemetry:
 * - Storage metrics (object count, total bytes)
 * - Delivery metrics (upstream speed)
 * - Source digest (audit trail)
 * - Assurance level (LOCAL_OBSERVATION)
 */
class ProofGenerator(
    private val parser: IStatsParser = AIZStatsParser,
    private val gnarkService: GnarkService,
    private val solanaClient: SolanaClient
) {

    companion object {
        private const val TAG = "ProofGenerator"
    }

    /**
     * Generate proof with device observations
     *
     * @param deviceId Unique device identifier
     * @param statsJson Raw bytes from AIOZ CLI (storage + delivery metrics)
     * @return GeneratedProof with gnark proof hash, observations, Solana transaction
     */
    suspend fun generateProof(
        deviceId: String,
        statsJson: ByteArray
    ): GeneratedProof {
        return withContext(Dispatchers.Default) {
            try {
                Log.d(TAG, "Starting proof generation for device: $deviceId")

                // Parse observations from CLI stats (strict validation)
                val observation = try {
                    parser.parseStats(statsJson)
                } catch (e: ParseException) {
                    Log.e(TAG, "Failed to parse observations: ${e.message}")
                    throw ProofException("Observation parsing failed: ${e.message}", e)
                }

                Log.d(TAG, "Parsed observation: storage=${observation.metrics.storageObjectCount}, " +
                        "size=${observation.metrics.storageSizeBytes}, " +
                        "speed=${observation.metrics.upstreamSpeedRaw}")

                // Build proof input with observations
                val proofInput = ProofInput(
                    deviceId = deviceId,
                    timestamp = System.currentTimeMillis(),
                    randomNonce = generateRandomNonce(),
                    observation = observation
                )

                // Generate zero-knowledge proof using gnark
                Log.d(TAG, "Calling gnark service for proof generation...")
                val proofHash = try {
                    gnarkService.proveDeviceIntegrity(proofInput)
                } catch (e: Exception) {
                    Log.e(TAG, "Gnark proof generation failed: ${e.message}")
                    throw ProofException("Proof generation failed: ${e.message}", e)
                }

                Log.d(TAG, "Proof hash: $proofHash")

                // Submit proof to Solana with observation metadata
                Log.d(TAG, "Submitting proof to Solana devnet...")
                val tx = try {
                    solanaClient.submitProof(
                        proof = proofHash,
                        observation = observation,
                        deviceId = deviceId
                    )
                } catch (e: Exception) {
                    Log.e(TAG, "Solana submission failed: ${e.message}")
                    throw ProofException("Solana submission failed: ${e.message}", e)
                }

                Log.d(TAG, "Proof submitted to Solana: ${tx.signature}")

                // Return complete proof result
                GeneratedProof(
                    proofHash = proofHash,
                    observation = observation,
                    deviceId = deviceId,
                    generationTime = System.currentTimeMillis(),
                    solanaTransaction = tx.signature,
                    transactionStatus = "SUBMITTED"
                )

            } catch (e: Exception) {
                Log.e(TAG, "Proof generation failed: ${e.message}", e)
                throw if (e is ProofException) e else ProofException("Unexpected error: ${e.message}", e)
            }
        }
    }

    /**
     * Generate random nonce for proof circuit
     */
    private fun generateRandomNonce(): ByteArray {
        return ByteArray(32).apply {
            (0 until size).forEach { i ->
                this[i] = (Math.random() * 256).toInt().toByte()
            }
        }
    }
}

/**
 * Input to proof generation circuit
 * Includes device observations for verifiable proof
 */
data class ProofInput(
    val deviceId: String,
    val timestamp: Long,
    val randomNonce: ByteArray,
    val observation: AIZObservation
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is ProofInput) return false

        if (deviceId != other.deviceId) return false
        if (timestamp != other.timestamp) return false
        if (!randomNonce.contentEquals(other.randomNonce)) return false
        if (observation != other.observation) return false

        return true
    }

    override fun hashCode(): Int {
        var result = deviceId.hashCode()
        result = 31 * result + timestamp.hashCode()
        result = 31 * result + randomNonce.contentHashCode()
        result = 31 * result + observation.hashCode()
        return result
    }
}

/**
 * Generated proof result
 * Ready for submission or local storage
 */
data class GeneratedProof(
    val proofHash: String,                   // Groth16 proof commitment
    val observation: AIZObservation,         // Device telemetry
    val deviceId: String,
    val generationTime: Long,
    val solanaTransaction: String,           // Solana TX signature
    val transactionStatus: String            // SUBMITTED, CONFIRMED, FAILED
)

/**
 * Proof generation error
 */
class ProofException(message: String, cause: Throwable? = null) : Exception(message, cause)

/**
 * Gnark service interface (implemented by gnark wrapper)
 */
interface GnarkService {
    suspend fun proveDeviceIntegrity(input: ProofInput): String
}

/**
 * Solana RPC client interface
 */
interface SolanaClient {
    suspend fun submitProof(
        proof: String,
        observation: AIZObservation,
        deviceId: String
    ): SolanaTransaction
}

/**
 * Solana transaction result
 */
data class SolanaTransaction(
    val signature: String,                   // Solana TX signature
    val slot: Long?,                         // Slot number (if confirmed)
    val blockTime: Long?,                    // Block timestamp
    val status: String = "SUBMITTED"         // SUBMITTED, CONFIRMED, FAILED
)
