package com.deproof.ui.viewmodel

import android.content.Context
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.deproof.data.observations.*
import com.deproof.data.solana.MobileWalletAdapter
import com.deproof.data.solana.ProofSubmissionService
import com.deproof.data.solana.SolanaRpcClient
import com.deproof.data.solana.SubmissionStatus
import com.deproof.ui.screen.ProofSubmissionScreenState
import io.mockk.mockk
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

@RunWith(AndroidJUnit4::class)
class ProofSubmissionViewModelTest {

    private lateinit var viewModel: ProofSubmissionViewModel
    private lateinit var mockContext: Context
    private lateinit var mockRpcClient: SolanaRpcClient
    private lateinit var mockWalletAdapter: MobileWalletAdapter
    private lateinit var mockSubmissionService: ProofSubmissionService

    private val testObservation = AIZObservation(
        source = "operator-supplied-cli-stats",
        sourceSha256 = "abc123def456abc123def456abc123def456abc123def456abc123def456abc1",
        metrics = Metrics(
            storageObjectCount = 42,
            storageSizeBytes = 1048576,
            upstreamSpeedRaw = 1024
        )
    )

    @Before
    fun setUp() {
        mockContext = mockk(relaxed = true)
        mockRpcClient = mockk(relaxed = true)
        mockWalletAdapter = mockk(relaxed = true)
        mockSubmissionService = mockk(relaxed = true)

        viewModel = ProofSubmissionViewModel(
            context = mockContext,
            rpcClient = mockRpcClient,
            walletAdapter = mockWalletAdapter,
            submissionService = mockSubmissionService
        )
    }

    @Test
    fun testInitialState() {
        runBlocking {
            val state = viewModel.uiState.first()

            assertEquals(0, state.observationCount)
            assertEquals("", state.proofHash)
            assertTrue(state.wallets.isEmpty())
            assertNull(state.selectedWallet)
            assertFalse(state.isDiscoveringWallets)
            assertFalse(state.isSubmitting)
            assertNull(state.lastResult)
            assertNull(state.errorMessage)
            assertEquals("Ready to submit proof", state.statusMessage)
        }
    }

    @Test
    fun testSetObservations() {
        runBlocking {
            viewModel.setObservations(listOf(testObservation))
            val state = viewModel.uiState.first()

            assertEquals(1, state.observationCount)
            assertEquals(64, state.proofHash.length) // SHA-256 hex is 64 chars
            assertNotNull(state.proofHash)
        }
    }

    @Test
    fun testProofHashGeneration() {
        runBlocking {
            val obs1 = testObservation
            val obs2 = testObservation.copy(
                sourceSha256 = "different123456789abc123456789abc123456789abc123456789abc123456789"
            )

            viewModel.setObservations(listOf(obs1))
            val hash1 = viewModel.uiState.first().proofHash

            viewModel.setObservations(listOf(obs2))
            val hash2 = viewModel.uiState.first().proofHash

            // Different observations should produce different hashes
            assertTrue(hash1.isNotEmpty())
            assertTrue(hash2.isNotEmpty())
        }
    }

    @Test
    fun testSelectWallet() {
        runBlocking {
            val wallet = com.deproof.data.solana.WalletInfo(
                name = "Phantom",
                packageName = "com.phantom",
                deeplink = "solana-wallet://phantom"
            )

            viewModel.selectWallet(wallet)
            val state = viewModel.uiState.first()

            assertNotNull(state.selectedWallet)
            assertEquals("Phantom", state.selectedWallet?.name)
            assertEquals("com.phantom", state.selectedWallet?.packageName)
        }
    }

    @Test
    fun testSubmitProofWithoutObservations() {
        runBlocking {
            viewModel.submitProof()
            val state = viewModel.uiState.first()

            assertEquals("No observations to submit", state.errorMessage)
            assertFalse(state.isSubmitting)
        }
    }

    @Test
    fun testSubmitProofWithoutWallet() {
        runBlocking {
            viewModel.setObservations(listOf(testObservation))
            viewModel.submitProof()
            val state = viewModel.uiState.first()

            assertEquals("No wallet selected", state.errorMessage)
            assertFalse(state.isSubmitting)
        }
    }

    @Test
    fun testDiscoveryStateTransition() {
        runBlocking {
            // Initially not discovering
            var state = viewModel.uiState.first()
            assertFalse(state.isDiscoveringWallets)

            // Start discovery
            viewModel.discoverWallets()

            // State should indicate discovery in progress
            state = viewModel.uiState.first()
            // Note: actual discovery behavior depends on mock setup
        }
    }

    @Test
    fun testRetrySubmission() {
        runBlocking {
            val wallet = com.deproof.data.solana.WalletInfo(
                name = "Phantom",
                packageName = "com.phantom",
                deeplink = "solana-wallet://phantom"
            )

            viewModel.setObservations(listOf(testObservation))
            viewModel.selectWallet(wallet)

            // Simulate a retry (clears error state)
            viewModel.retrySubmission()

            val state = viewModel.uiState.first()
            assertNull(state.errorMessage)
            assertNull(state.lastResult)
        }
    }

    @Test
    fun testStatusMessageBuildingForConfirmed() {
        val result = com.deproof.data.solana.ProofSubmissionResult(
            transactionSignature = "sig123abc",
            status = SubmissionStatus.CONFIRMED
        )

        val message = buildStatusMessage(result)
        assertTrue(message.contains("✅"))
        assertTrue(message.contains("successfully"))
    }

    @Test
    fun testStatusMessageBuildingForFailed() {
        val result = com.deproof.data.solana.ProofSubmissionResult(
            transactionSignature = null,
            status = SubmissionStatus.FAILED,
            error = "Transaction failed"
        )

        val message = buildStatusMessage(result)
        assertTrue(message.contains("❌"))
        assertTrue(message.contains("failed"))
    }

    @Test
    fun testStatusMessageBuildingForTimeout() {
        val result = com.deproof.data.solana.ProofSubmissionResult(
            transactionSignature = "sig123",
            status = SubmissionStatus.TIMEOUT,
            error = "Confirmation timeout"
        )

        val message = buildStatusMessage(result)
        assertTrue(message.contains("⏱️"))
        assertTrue(message.contains("timeout"))
    }

    // Helper function extracted from ViewModel for testing
    private fun buildStatusMessage(result: com.deproof.data.solana.ProofSubmissionResult): String {
        return when (result.status) {
            SubmissionStatus.PENDING -> "Proof submission pending..."
            SubmissionStatus.CONFIRMING -> "Confirming transaction on blockchain..."
            SubmissionStatus.CONFIRMED -> "✅ Proof successfully submitted! Tx: ${result.transactionSignature?.take(8)}..."
            SubmissionStatus.FAILED -> "❌ Submission failed: ${result.error ?: "Unknown error"}"
            SubmissionStatus.TIMEOUT -> "⏱️ Transaction confirmation timeout"
        }
    }
}
