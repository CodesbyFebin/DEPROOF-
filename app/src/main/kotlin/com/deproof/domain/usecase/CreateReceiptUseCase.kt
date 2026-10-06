package com.deproof.domain.usecase

import com.deproof.domain.model.Receipt
import com.deproof.domain.model.Result
import com.deproof.domain.model.SignatureStatus
import com.deproof.crypto.MessageBinding
import com.deproof.data.repository.ReceiptRepository
import java.util.UUID

class CreateReceiptUseCase(private val receiptRepository: ReceiptRepository) {

    suspend fun createReceipt(
        txHash: String,
        verdict: String,
        messageText: String
    ): Result<String> {
        return try {
            if (!isValidTransactionHash(txHash)) {
                return Result.Error(IllegalArgumentException("Invalid transaction hash"))
            }

            val messageHash = MessageBinding.calculateMessageHash(messageText)

            val receipt = Receipt(
                id = UUID.randomUUID().toString(),
                transactionHash = txHash,
                verdict = verdict,
                messageHash = messageHash,
                timestamp = System.currentTimeMillis(),
                signatureStatus = SignatureStatus.PENDING,
                chainSubmitted = false,
                jsonData = ""
            )

            receiptRepository.insertReceipt(receipt)
        } catch (e: Exception) {
            Result.Error(e)
        }
    }

    suspend fun markReceiptSigned(receiptId: String): Result<Unit> {
        return try {
            val receipt = when (val result = receiptRepository.getReceiptById(receiptId)) {
                is Result.Success -> result.data
                is Result.Error -> return result
                else -> return Result.Error(Exception("Unknown error"))
            }

            val signedReceipt = receipt.copy(
                signatureStatus = SignatureStatus.SIGNED,
                timestamp = System.currentTimeMillis()
            )

            receiptRepository.updateReceipt(signedReceipt)
        } catch (e: Exception) {
            Result.Error(e)
        }
    }

    suspend fun markReceiptChainSubmitted(receiptId: String): Result<Unit> {
        return try {
            val receipt = when (val result = receiptRepository.getReceiptById(receiptId)) {
                is Result.Success -> result.data
                is Result.Error -> return result
                else -> return Result.Error(Exception("Unknown error"))
            }

            val submittedReceipt = receipt.copy(
                chainSubmitted = true,
                timestamp = System.currentTimeMillis()
            )

            receiptRepository.updateReceipt(submittedReceipt)
        } catch (e: Exception) {
            Result.Error(e)
        }
    }

    suspend fun rejectReceipt(receiptId: String): Result<Unit> {
        return try {
            val receipt = when (val result = receiptRepository.getReceiptById(receiptId)) {
                is Result.Success -> result.data
                is Result.Error -> return result
                else -> return Result.Error(Exception("Unknown error"))
            }

            val rejectedReceipt = receipt.copy(
                signatureStatus = SignatureStatus.REJECTED,
                timestamp = System.currentTimeMillis()
            )

            receiptRepository.updateReceipt(rejectedReceipt)
        } catch (e: Exception) {
            Result.Error(e)
        }
    }

    private fun isValidTransactionHash(txHash: String): Boolean {
        return txHash.length in 86..88 && txHash.isNotEmpty()
    }
}
