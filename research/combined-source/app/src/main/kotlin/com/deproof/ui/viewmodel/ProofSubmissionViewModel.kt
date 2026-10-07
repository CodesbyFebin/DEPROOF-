package com.deproof.ui.viewmodel

import android.content.Context
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.deproof.data.observations.Observation
import com.deproof.data.solana.*
import com.deproof.ui.screen.ProofSubmissionScreenState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.util.UUID

class ProofSubmissionViewModel(
    private val context: Context,
    private val rpcClient: SolanaRpcClient = SolanaRpcClient("https://api.devnet.solana.com"),
    private val walletAdapter: MobileWalletAdapter = MobileWalletAdapter(context),
    private val submissionService: ProofSubmissionService = ProofSubmissionService(rpcClient)
) : ViewModel() {

    companion object {
        const val TAG = "ProofSubmissionVM"
        const val DEVNET_ENDPOINT = "https://api.devnet.solana.com"
    }

    private val _uiState = MutableStateFlow(ProofSubmissionScreenState())
    val uiState: StateFlow<ProofSubmissionScreenState> = _uiState.asStateFlow()

    private var observations: List<Observation> = emptyList()
    private var proofHash: String = ""
    private var selectedWallet: WalletInfo? = null

    init {
        Log.d(TAG, "ProofSubmissionViewModel initialized")
    }

    fun setObservations(obs: List<Observation>) {
        observations = obs
        val hash = generateProofHash(obs)
        proofHash = hash
        updateState {
            it.copy(
                observationCount = obs.size,
                proofHash = hash
            )
        }
        Log.d(TAG, "Observations set: ${obs.size} items, proof hash: $hash")
    }

    fun discoverWallets() {
        viewModelScope.launch {
            try {
                updateState { it.copy(isDiscoveringWallets = true) }
                Log.d(TAG, "Starting wallet discovery...")

                val wallets = walletAdapter.discoverWallets()
                Log.d(TAG, "Discovered ${wallets.size} wallets")

                updateState { state ->
                    state.copy(
                        wallets = wallets,
                        isDiscoveringWallets = false,
                        statusMessage = "Found ${wallets.size} wallet(s)"
                    )
                }
            } catch (e: Exception) {
                Log.e(TAG, "Wallet discovery failed: ${e.message}", e)
                updateState { state ->
                    state.copy(
                        isDiscoveringWallets = false,
                        errorMessage = "Wallet discovery failed: ${e.message}"
                    )
                }
            }
        }
    }

    fun selectWallet(wallet: WalletInfo) {
        selectedWallet = wallet
        updateState { state ->
            state.copy(
                selectedWallet = wallet,
                statusMessage = "Selected ${wallet.name}"
            )
        }
        Log.d(TAG, "Selected wallet: ${wallet.name}")
    }

    fun submitProof() {
        if (observations.isEmpty()) {
            updateState { it.copy(errorMessage = "No observations to submit") }
            return
        }

        if (selectedWallet == null) {
            updateState { it.copy(errorMessage = "No wallet selected") }
            return
        }

        viewModelScope.launch {
            try {
                updateState { state ->
                    state.copy(
                        isSubmitting = true,
                        statusMessage = "Getting latest blockhash...",
                        errorMessage = null
                    )
                }

                // Generate proof hash
                val proofHash = generateProofHash(observations)
                Log.d(TAG, "Proof generated: $proofHash")

                updateState { state ->
                    state.copy(statusMessage = "Proof generated, preparing transaction...")
                }

                // Create submission request
                val request = ProofSubmissionRequest(
                    deviceId = UUID.randomUUID().toString(),
                    observations = observations.map { it.toString() },
                    proofHash = proofHash,
                    timestamp = System.currentTimeMillis()
                )

                updateState { state ->
                    state.copy(statusMessage = "Sending to wallet for signing...")
                }

                // Submit proof (in real implementation, would handle wallet response)
                val walletPublicKey = "11111111111111111111111111111111" // Placeholder

                val result = submissionService.submitProof(request, walletPublicKey)

                Log.d(TAG, "Proof submission result: ${result.status}")

                updateState { state ->
                    state.copy(
                        isSubmitting = false,
                        lastResult = result,
                        statusMessage = buildStatusMessage(result)
                    )
                }

            } catch (e: Exception) {
                Log.e(TAG, "Proof submission failed: ${e.message}", e)
                updateState { state ->
                    state.copy(
                        isSubmitting = false,
                        errorMessage = "Submission failed: ${e.message}"
                    )
                }
            }
        }
    }

    fun retrySubmission() {
        updateState { it.copy(lastResult = null, errorMessage = null) }
        submitProof()
    }

    private fun generateProofHash(observations: List<Observation>): String {
        val message = observations.joinToString("|") { it.toString() }
        val digest = java.security.MessageDigest.getInstance("SHA-256")
        val hashBytes = digest.digest(message.toByteArray())
        return hashBytes.joinToString("") { "%02x".format(it) }
    }

    private fun buildStatusMessage(result: ProofSubmissionResult): String {
        return when (result.status) {
            SubmissionStatus.PENDING -> "Proof submission pending..."
            SubmissionStatus.CONFIRMING -> "Confirming transaction on blockchain..."
            SubmissionStatus.CONFIRMED -> "✅ Proof successfully submitted! Tx: ${result.transactionSignature?.take(8)}..."
            SubmissionStatus.FAILED -> "❌ Submission failed: ${result.error ?: "Unknown error"}"
            SubmissionStatus.TIMEOUT -> "⏱️ Transaction confirmation timeout"
        }
    }

    private fun updateState(update: (ProofSubmissionScreenState) -> ProofSubmissionScreenState) {
        _uiState.value = update(_uiState.value)
    }
}
