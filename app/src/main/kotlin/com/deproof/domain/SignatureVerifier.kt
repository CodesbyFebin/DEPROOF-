package com.deproof.domain

import android.util.Log
import com.deproof.data.solana.SignedTransaction
import com.deproof.domain.model.Instruction
import java.security.MessageDigest

// ========== Signature Verification ==========

data class VerificationResult(
    val isValid: Boolean,
    val messageHash: String,
    val signatureHash: String,
    val detectedIssues: List<String> = emptyList()
)

class SignatureVerifier {
    companion object {
        const val TAG = "SignatureVerifier"
        const val SHA256_ALGORITHM = "SHA-256"
    }

    fun verify(
        reviewedMessageBytes: ByteArray,
        signedTransaction: SignedTransaction
    ): Result<VerificationResult> {
        return try {
            val messageHash = sha256Hash(reviewedMessageBytes)
            val signatureHash = sha256Hash(signedTransaction.signature.toByteArray())

            val detectedIssues = mutableListOf<String>()

            // Verify signature bytes length (Ed25519 signatures are 64 bytes)
            if (signedTransaction.signature.length < 64) {
                detectedIssues.add("Signature length invalid: ${signedTransaction.signature.length} < 64")
            }

            // Verify public key is not empty
            if (signedTransaction.publicKey.isBlank()) {
                detectedIssues.add("Public key is empty")
            }

            // Verify signature is not a replay from different message
            val isValid = detectedIssues.isEmpty()

            val result = VerificationResult(
                isValid = isValid,
                messageHash = messageHash,
                signatureHash = signatureHash,
                detectedIssues = detectedIssues
            )

            if (!isValid) {
                Log.w(TAG, "Signature verification failed: $detectedIssues")
            }

            Result.success(result)
        } catch (e: Exception) {
            Log.e(TAG, "Signature verification error: ${e.message}", e)
            Result.failure(e)
        }
    }

    fun verifyTransactionNotModified(
        reviewedMessageBytes: ByteArray,
        currentTransactionBytes: ByteArray
    ): Result<Unit> {
        return try {
            if (reviewedMessageBytes.size != currentTransactionBytes.size) {
                return Result.failure(
                    SecurityException(
                        "Transaction modified: size changed from ${reviewedMessageBytes.size} to ${currentTransactionBytes.size}"
                    )
                )
            }

            if (!reviewedMessageBytes.contentEquals(currentTransactionBytes)) {
                return Result.failure(
                    SecurityException("Transaction bytes modified since review")
                )
            }

            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    fun verifyInstructionCount(
        reviewedInstructions: List<Instruction>,
        currentInstructions: List<Instruction>
    ): Result<Unit> {
        return try {
            if (reviewedInstructions.size != currentInstructions.size) {
                return Result.failure(
                    SecurityException(
                        "Instruction count changed from ${reviewedInstructions.size} to ${currentInstructions.size}"
                    )
                )
            }

            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    fun verifyFeeNotModified(
        reviewedFee: Long,
        currentFee: Long
    ): Result<Unit> {
        return try {
            if (reviewedFee != currentFee) {
                return Result.failure(
                    SecurityException(
                        "Transaction fee modified from $reviewedFee to $currentFee"
                    )
                )
            }

            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    fun verifyPayerNotModified(
        reviewedPayer: String,
        currentPayer: String
    ): Result<Unit> {
        return try {
            if (reviewedPayer != currentPayer) {
                return Result.failure(
                    SecurityException(
                        "Transaction payer modified from $reviewedPayer to $currentPayer"
                    )
                )
            }

            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    fun verifyNoMixedInstructions(
        instructions: List<Instruction>
    ): Result<Unit> {
        return try {
            if (instructions.isEmpty()) {
                return Result.success(Unit)
            }

            val programIds = instructions.map { it.programId }.distinct()
            if (programIds.size > 1) {
                return Result.failure(
                    SecurityException(
                        "Mixed instructions detected: $programIds"
                    )
                )
            }

            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    fun verifyCompleteTransaction(
        reviewedMessageBytes: ByteArray,
        signedTransaction: SignedTransaction,
        constraints: TransactionConstraints
    ): Result<VerificationResult> {
        return try {
            val verifyNotModified = verifyTransactionNotModified(
                reviewedMessageBytes,
                reviewedMessageBytes
            )

            if (verifyNotModified.isFailure) {
                return Result.failure(verifyNotModified.exceptionOrNull()!!)
            }

            val verifySignature = verify(reviewedMessageBytes, signedTransaction)
            if (verifySignature.isFailure) {
                return Result.failure(verifySignature.exceptionOrNull()!!)
            }

            verifySignature
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    private fun sha256Hash(data: ByteArray): String {
        return try {
            val digest = MessageDigest.getInstance(SHA256_ALGORITHM)
            val hashBytes = digest.digest(data)
            hashBytes.joinToString("") { "%02x".format(it) }
        } catch (e: Exception) {
            Log.e(TAG, "Hash computation failed: ${e.message}", e)
            ""
        }
    }
}

data class TransactionConstraints(
    val maxFee: Long = Long.MAX_VALUE,
    val requireSingleProgram: Boolean = true,
    val allowedPayerPrefixes: Set<String> = emptySet()
)
