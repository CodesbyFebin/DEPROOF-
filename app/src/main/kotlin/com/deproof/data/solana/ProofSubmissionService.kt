package com.deproof.data.solana

import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import java.util.Base64

data class ProofSubmissionRequest(
    val deviceId: String,
    val observations: List<String>,
    val proofHash: String,
    val timestamp: Long,
    val signature: String? = null
)

data class ProofSubmissionResult(
    val transactionSignature: String?,
    val status: SubmissionStatus,
    val error: String? = null,
    val confirmationTime: Long? = null
)

enum class SubmissionStatus {
    PENDING,
    CONFIRMING,
    CONFIRMED,
    FAILED,
    TIMEOUT
}

class ProofSubmissionService(
    private val rpcClient: SolanaRpcClient,
    private val skrMint: String = "SKRbvo6Gf7GoNcKKqqyckfjxN2PEVEqJf3rUKdPbdYu",
    private val programId: String = "DeproofProofProgramIDHere"
) {
    companion object {
        const val TAG = "ProofSubmissionService"
        const val MAX_CONFIRMATION_POLLS = 30
        const val POLL_INTERVAL_MS = 2000L
    }

    suspend fun submitProof(
        request: ProofSubmissionRequest,
        walletPublicKey: String
    ): ProofSubmissionResult = withContext(Dispatchers.IO) {
        return@withContext try {
            Log.d(TAG, "Submitting proof for device: ${request.deviceId}")

            val blockhash = rpcClient.getLatestBlockhash()
                ?: return@withContext ProofSubmissionResult(
                    null,
                    SubmissionStatus.FAILED,
                    "Failed to get latest blockhash"
                )

            Log.d(TAG, "Latest blockhash: ${blockhash.blockhash}")

            val transaction = buildProofTransaction(
                request,
                walletPublicKey,
                blockhash
            )

            Log.d(TAG, "Built transaction, attempting to send...")

            val transactionSignature = rpcClient.sendTransaction(transaction)
                ?: return@withContext ProofSubmissionResult(
                    null,
                    SubmissionStatus.FAILED,
                    "Failed to send transaction"
                )

            Log.d(TAG, "Transaction sent: $transactionSignature")

            val status = confirmTransaction(transactionSignature)

            ProofSubmissionResult(
                transactionSignature = transactionSignature,
                status = status,
                confirmationTime = System.currentTimeMillis() - request.timestamp
            )

        } catch (e: Exception) {
            Log.e(TAG, "Error submitting proof: ${e.message}", e)
            ProofSubmissionResult(
                null,
                SubmissionStatus.FAILED,
                e.message
            )
        }
    }

    private suspend fun confirmTransaction(
        signature: String,
        maxPolls: Int = MAX_CONFIRMATION_POLLS
    ): SubmissionStatus = withContext(Dispatchers.IO) {
        for (attempt in 0 until maxPolls) {
            try {
                val status = rpcClient.getSignatureStatus(signature)

                if (status != null) {
                    val confirmationStatus = status["confirmationStatus"] as? String
                    val err = status["err"]

                    when {
                        err != null -> {
                            Log.w(TAG, "Transaction failed: $err")
                            return@withContext SubmissionStatus.FAILED
                        }
                        confirmationStatus == "confirmed" || confirmationStatus == "finalized" -> {
                            Log.d(TAG, "Transaction confirmed: $signature")
                            return@withContext SubmissionStatus.CONFIRMED
                        }
                        else -> {
                            Log.d(TAG, "Transaction pending (attempt $attempt/$maxPolls)")
                            delay(POLL_INTERVAL_MS)
                        }
                    }
                } else {
                    Log.d(TAG, "Status not yet available (attempt $attempt/$maxPolls)")
                    delay(POLL_INTERVAL_MS)
                }

            } catch (e: Exception) {
                Log.w(TAG, "Error checking transaction status: ${e.message}")
                delay(POLL_INTERVAL_MS)
            }
        }

        Log.w(TAG, "Transaction confirmation timeout")
        return@withContext SubmissionStatus.TIMEOUT
    }

    private fun buildProofTransaction(
        request: ProofSubmissionRequest,
        walletPublicKey: String,
        blockhash: LatestBlockhash
    ): String {
        Log.d(TAG, "Building proof transaction for $walletPublicKey")

        val instruction = buildProofInstruction(request, walletPublicKey)

        val message = buildTransactionMessage(
            instruction,
            listOf(walletPublicKey),
            blockhash.blockhash
        )

        return Base64.getEncoder().encodeToString(message.toByteArray(Charsets.UTF_8))
    }

    private fun buildProofInstruction(
        request: ProofSubmissionRequest,
        walletPublicKey: String
    ): String {
        val instructionData = mutableListOf<String>()
        instructionData.add("DeProofProof")
        instructionData.add(request.deviceId)
        instructionData.add(request.proofHash)
        instructionData.add(request.timestamp.toString())

        Log.d(TAG, "Proof instruction data: $instructionData")
        return instructionData.joinToString("|")
    }

    private fun buildTransactionMessage(
        instruction: String,
        signers: List<String>,
        recentBlockhash: String
    ): String {
        val message = mutableListOf<String>()
        message.add("solana-tx-v1")
        message.add(recentBlockhash)
        message.add("${signers.size}")
        message.addAll(signers)
        message.add(instruction)

        val txString = message.joinToString(":")
        Log.d(TAG, "Transaction message: $txString")
        return txString
    }

    suspend fun getProofStatus(signature: String): ProofSubmissionResult? =
        withContext(Dispatchers.IO) {
            return@withContext try {
                val status = rpcClient.getSignatureStatus(signature) ?: return@withContext null

                val confirmationStatus = status["confirmationStatus"] as? String
                val err = status["err"]

                val submissionStatus = when {
                    err != null -> SubmissionStatus.FAILED
                    confirmationStatus == "confirmed" || confirmationStatus == "finalized" ->
                        SubmissionStatus.CONFIRMED
                    else -> SubmissionStatus.CONFIRMING
                }

                ProofSubmissionResult(
                    transactionSignature = signature,
                    status = submissionStatus,
                    error = err?.toString()
                )

            } catch (e: Exception) {
                Log.e(TAG, "Error getting proof status: ${e.message}")
                null
            }
        }
}
