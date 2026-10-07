package com.deproof.ui.integration
import org.junit.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue
import kotlin.test.assertFalse

import android.content.Context
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.deproof.data.observations.AIZObservation
import com.deproof.data.observations.Metrics
import com.deproof.data.solana.*
import com.deproof.ui.TestFixtures
import com.deproof.ui.TestScreenStateBuilder
import com.deproof.ui.viewmodel.ProofSubmissionViewModel
import io.mockk.mockk
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Before
import org.junit.runner.RunWith

/**
 * Integration tests for the complete proof submission flow
 */
@RunWith(AndroidJUnit4::class)
class ProofSubmissionFlowTest {

    private lateinit var viewModel: ProofSubmissionViewModel
    private lateinit var mockContext: Context
    private lateinit var mockRpcClient: SolanaRpcClient
    private lateinit var mockWalletAdapter: MobileWalletAdapter
    private lateinit var mockSubmissionService: ProofSubmissionService

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
    fun testCompleteSubmissionFlow() {
        runBlocking {
            // Step 1: Load observations
            viewModel.setObservations(listOf(TestFixtures.aizObservation))
            var state = viewModel.uiState.first()

            assertEquals(1, state.observationCount)
            assertNotNull(state.proofHash)
            assertTrue(state.proofHash.isNotEmpty())

            // Step 2: Select wallet
            viewModel.selectWallet(TestFixtures.phantomWallet)
            state = viewModel.uiState.first()

            assertEquals("Phantom", state.selectedWallet?.name)
            assertNull(state.errorMessage)

            // Step 3: Attempt submission (will fail due to mocks, but flow is validated)
            viewModel.submitProof()
            state = viewModel.uiState.first()

            // After submission attempt, either submitting or result should be set
            assertTrue(state.isSubmitting || state.lastResult != null)
        }
    }

    @Test
    fun testWalletSwitchingFlow() {
        runBlocking {
            // Select first wallet
            viewModel.selectWallet(TestFixtures.phantomWallet)
            var state = viewModel.uiState.first()
            assertEquals("Phantom", state.selectedWallet?.name)

            // Switch to second wallet
            viewModel.selectWallet(TestFixtures.solflareWallet)
            state = viewModel.uiState.first()
            assertEquals("Solflare", state.selectedWallet?.name)

            // Switch to third wallet
            viewModel.selectWallet(TestFixtures.magicEdenWallet)
            state = viewModel.uiState.first()
            assertEquals("Magic Eden", state.selectedWallet?.name)
        }
    }

    @Test
    fun testMultipleObservationsHandling() {
        runBlocking {
            val observations = listOf(
                TestFixtures.aizObservation,
                TestFixtures.fluxObservation
            )

            viewModel.setObservations(observations)
            val state = viewModel.uiState.first()

            assertEquals(2, state.observationCount)
            assertNotNull(state.proofHash)
        }
    }

    @Test
    fun testErrorRecoveryFlow() {
        runBlocking {
            // Start with error state
            viewModel.submitProof() // Will fail - no observations
            var state = viewModel.uiState.first()
            assertEquals("No observations to submit", state.errorMessage)

            // Recover by adding observations
            viewModel.setObservations(listOf(TestFixtures.aizObservation))
            state = viewModel.uiState.first()
            assertEquals(1, state.observationCount)

            // Try again - should fail due to no wallet
            viewModel.submitProof()
            state = viewModel.uiState.first()
            assertEquals("No wallet selected", state.errorMessage)

            // Select wallet
            viewModel.selectWallet(TestFixtures.phantomWallet)
            state = viewModel.uiState.first()
            assertNull(state.errorMessage)
        }
    }

    @Test
    fun testStateBuilderCreatesCorrectState() {
        val state = TestScreenStateBuilder()
            .withObservationCount(3)
            .withProofHash(TestFixtures.VALID_PROOF_HASH)
            .withWallets(TestFixtures.allWallets)
            .withSelectedWallet(TestFixtures.phantomWallet)
            .withResult(TestFixtures.confirmedResult)
            .build()

        assertEquals(3, state.observationCount)
        assertEquals(TestFixtures.VALID_PROOF_HASH, state.proofHash)
        assertEquals(3, state.wallets.size)
        assertEquals("Phantom", state.selectedWallet?.name)
        assertEquals(SubmissionStatus.CONFIRMED, state.lastResult?.status)
    }

    @Test
    fun testProofHashConsistency() {
        runBlocking {
            val obs = TestFixtures.aizObservation

            // Set observations twice with same data
            viewModel.setObservations(listOf(obs))
            val hash1 = viewModel.uiState.first().proofHash

            viewModel.setObservations(listOf(obs))
            val hash2 = viewModel.uiState.first().proofHash

            // Same observations should produce same hash (deterministic)
            assertEquals(hash1, hash2)
        }
    }

    @Test
    fun testRetryAfterFailure() {
        runBlocking {
            // Setup initial state with wallet
            viewModel.setObservations(listOf(TestFixtures.aizObservation))
            viewModel.selectWallet(TestFixtures.phantomWallet)

            // First submission attempt (mocked to not actually submit)
            viewModel.submitProof()
            var state = viewModel.uiState.first()

            // Retry submission
            viewModel.retrySubmission()
            state = viewModel.uiState.first()

            // Error state should be cleared
            assertNull(state.errorMessage)
            assertNull(state.lastResult)
            // Observations and wallet should still be set
            assertEquals(1, state.observationCount)
            assertEquals("Phantom", state.selectedWallet?.name)
        }
    }

    @Test
    fun testWalletDiscoveryInitiation() {
        runBlocking {
            viewModel.discoverWallets()
            val state = viewModel.uiState.first()

            // Discovery should be initiated (actual results depend on mock setup)
            // but state should reflect the action
            assertTrue(state.isDiscoveringWallets || state.wallets.isNotEmpty() || state.errorMessage != null)
        }
    }

    @Test
    fun testScreenStateTransitionsForSubmission() {
        runBlocking {
            viewModel.setObservations(listOf(TestFixtures.aizObservation))
            viewModel.selectWallet(TestFixtures.phantomWallet)

            // Before submission
            var state = viewModel.uiState.first()
            assertEquals("Ready to submit proof", state.statusMessage)
            assertFalse(state.isSubmitting)

            // During submission
            viewModel.submitProof()
            state = viewModel.uiState.first()
            // Status should change (actual message depends on implementation)
            assertTrue(
                state.isSubmitting ||
                state.statusMessage.contains("Getting") ||
                state.statusMessage.contains("Proof") ||
                state.lastResult != null
            )
        }
    }

    @Test
    fun testProofHashFormat() {
        runBlocking {
            viewModel.setObservations(listOf(TestFixtures.aizObservation))
            val state = viewModel.uiState.first()

            // SHA-256 hash should be 64 hex characters
            assertEquals(64, state.proofHash.length)
            assertTrue(state.proofHash.all { it in '0'..'9' || it in 'a'..'f' })
        }
    }
}
