package com.deproof.ui

import android.content.Context
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.graphics.Color
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.deproof.data.observations.*
import com.deproof.data.solana.MobileWalletAdapter
import com.deproof.data.solana.ProofSubmissionService
import com.deproof.data.solana.SolanaRpcClient
import com.deproof.domain.proof.ProofGeneratorWithObservations
import com.deproof.ui.screen.DeviceStatsScreen
import com.deproof.ui.screen.ProofSubmissionScreen
import com.deproof.ui.viewmodel.ProofSubmissionViewModel

class MainActivity : ComponentActivity() {
    private val viewModel: ProofSubmissionViewModel by viewModels {
        ProofSubmissionViewModelFactory(
            context = this,
            rpcClient = SolanaRpcClient("https://api.devnet.solana.com"),
            walletAdapter = MobileWalletAdapter(this),
            submissionService = ProofSubmissionService(
                SolanaRpcClient("https://api.devnet.solana.com")
            )
        )
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MaterialTheme {
                Surface(color = Color(0xFF0A0E27)) {
                    DeproofNavigation(viewModel)
                }
            }
        }
    }
}

@Composable
fun DeproofNavigation(viewModel: ProofSubmissionViewModel) {
    val state = viewModel.uiState.collectAsState()
    val currentScreen = remember { mutableStateOf("observations") }

    when (currentScreen.value) {
        "observations" -> {
            val observation = AIZObservation(
                source = "operator-supplied-cli-stats",
                sourceSha256 = "abc123def456abc123def456abc123def456abc123def456abc123def456abc1",
                metrics = Metrics(
                    storageObjectCount = 42,
                    storageSizeBytes = 1048576,
                    upstreamSpeedRaw = 1024
                )
            )
            DeviceStatsScreen(
                observation,
                onProceedToSubmission = {
                    viewModel.setObservations(listOf(observation))
                    currentScreen.value = "submission"
                }
            )
        }
        "submission" -> {
            ProofSubmissionScreen(
                state = state.value,
                onDiscoverWallets = { viewModel.discoverWallets() },
                onSelectWallet = { viewModel.selectWallet(it) },
                onSubmitProof = { viewModel.submitProof() }
            )
        }
    }
}

class ProofSubmissionViewModelFactory(
    private val context: Context,
    private val rpcClient: SolanaRpcClient,
    private val walletAdapter: MobileWalletAdapter,
    private val submissionService: ProofSubmissionService
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        return ProofSubmissionViewModel(
            context = context,
            rpcClient = rpcClient,
            walletAdapter = walletAdapter,
            submissionService = submissionService
        ) as T
    }
}
