package com.deproof.presentation.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.deproof.domain.model.Instruction
import com.deproof.domain.model.Receipt
import com.deproof.domain.model.ReviewBinding
import com.deproof.domain.model.Verdict
import com.deproof.crypto.InstructionDecoder
import com.deproof.crypto.MessageBinding
import com.deproof.data.repository.RpcRepository
import com.deproof.data.repository.ReceiptRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class ReviewUiState(
    val transactionHash: String = "",
    val transactionData: String = "",
    val instructions: List<Instruction> = emptyList(),
    val verdict: Verdict? = null,
    val messageBinding: ReviewBinding? = null,
    val tamperDetectionAlert: String? = null,
    val isLoadingTransaction: Boolean = false,
    val isProcessingVerdictFailed: Boolean = false,
    val error: String? = null,
    val isSigning: Boolean = false,
    val signatureResult: String? = null
)

class ReviewViewModel(
    private val rpcRepository: RpcRepository,
    private val receiptRepository: ReceiptRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(ReviewUiState())
    val uiState: StateFlow<ReviewUiState> = _uiState.asStateFlow()

    fun loadTransaction(signature: String) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoadingTransaction = true, error = null)

            val result = rpcRepository.getTransaction(signature)
            when (result) {
                is com.deproof.domain.model.Result.Success -> {
                    _uiState.value = _uiState.value.copy(
                        transactionHash = signature,
                        transactionData = result.data,
                        isLoadingTransaction = false
                    )
                    decodeInstructions(signature, result.data)
                }
                is com.deproof.domain.model.Result.Error -> {
                    _uiState.value = _uiState.value.copy(
                        error = result.exception.message ?: "Failed to load transaction",
                        isLoadingTransaction = false
                    )
                }
                else -> {}
            }
        }
    }

    private fun decodeInstructions(txHash: String, transactionData: String) {
        viewModelScope.launch {
            try {
                val instructions = mutableListOf<Instruction>()
                val verdicts = mutableListOf<Verdict>()

                for (instruction in instructions) {
                    val verdict = InstructionDecoder.decodeInstruction(instruction)
                    verdicts.add(verdict)
                }

                val finalVerdict = when {
                    verdicts.any { it is Verdict.DoNotSign } -> Verdict.DoNotSign
                    verdicts.all { it is Verdict.Payable } -> Verdict.Payable
                    else -> Verdict.Unknown("Mixed or unknown verdicts")
                }

                createMessageBinding(txHash, finalVerdict)
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    error = "Instruction decoding failed: ${e.message}"
                )
            }
        }
    }

    private fun createMessageBinding(txHash: String, verdict: Verdict) {
        val verdictText = when (verdict) {
            is Verdict.Payable -> "PAYABLE"
            is Verdict.DoNotSign -> "DO_NOT_SIGN"
            is Verdict.Unknown -> verdict.reason
        }

        val canonicalMessage = MessageBinding.createCanonicalMessage(
            transactionHash = txHash,
            verdict = verdictText
        )
        val messageHash = MessageBinding.calculateMessageHash(canonicalMessage)

        val messageBinding = ReviewBinding(
            txHash = txHash,
            messageText = canonicalMessage,
            messageHash = messageHash,
            verdict = verdictText,
            timestamp = System.currentTimeMillis()
        )

        _uiState.value = _uiState.value.copy(
            verdict = verdict,
            messageBinding = messageBinding,
            isProcessingVerdictFailed = false
        )
    }

    fun verifyMessageIntegrity() {
        val messageBinding = _uiState.value.messageBinding ?: return

        try {
            val isValid = MessageBinding.verifyMessageIntegrity(
                messageBinding.messageText,
                messageBinding.messageHash
            )

            if (!isValid) {
                val alert = MessageBinding.createTamperDetectionAlert(
                    messageBinding.messageHash,
                    messageBinding.messageText
                )
                _uiState.value = _uiState.value.copy(tamperDetectionAlert = alert)
            }
        } catch (e: Exception) {
            _uiState.value = _uiState.value.copy(
                error = "Message verification failed: ${e.message}"
            )
        }
    }

    fun approveAndSign() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isSigning = true)

            val messageBinding = _uiState.value.messageBinding
            val verdict = _uiState.value.verdict

            if (messageBinding != null && verdict != null) {
                val receipt = Receipt(
                    id = java.util.UUID.randomUUID().toString(),
                    txHash = messageBinding.txHash,
                    verdict = messageBinding.verdict,
                    messageHash = messageBinding.messageHash,
                    timestamp = System.currentTimeMillis(),
                    signatureStatus = com.deproof.domain.model.SignatureStatus.SIGNED,
                    chainSubmitted = false,
                    jsonData = ""
                )

                val result = receiptRepository.insertReceipt(receipt)
                when (result) {
                    is com.deproof.domain.model.Result.Success -> {
                        _uiState.value = _uiState.value.copy(
                            isSigning = false,
                            signatureResult = "Transaction signed and recorded"
                        )
                    }
                    is com.deproof.domain.model.Result.Error -> {
                        _uiState.value = _uiState.value.copy(
                            isSigning = false,
                            error = result.exception.message ?: "Failed to record signature"
                        )
                    }
                    else -> {}
                }
            }
        }
    }

    fun reject() {
        _uiState.value = _uiState.value.copy(verdict = Verdict.DoNotSign)
    }

    fun clearError() {
        _uiState.value = _uiState.value.copy(error = null)
    }

    fun clearSignatureResult() {
        _uiState.value = _uiState.value.copy(signatureResult = null)
    }
}
