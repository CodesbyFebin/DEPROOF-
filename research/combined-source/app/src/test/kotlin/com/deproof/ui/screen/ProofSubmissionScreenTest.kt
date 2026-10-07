package com.deproof.ui.screen

import com.deproof.data.solana.ProofSubmissionResult
import com.deproof.data.solana.SubmissionStatus
import com.deproof.data.solana.WalletInfo
import org.junit.Assert.*
import org.junit.Test

class ProofSubmissionScreenStateTest {

    @Test
    fun testInitialState() {
        val state = ProofSubmissionScreenState()

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

    @Test
    fun testStateWithObservations() {
        val state = ProofSubmissionScreenState(
            observationCount = 2,
            proofHash = "abc123def456"
        )

        assertEquals(2, state.observationCount)
        assertEquals("abc123def456", state.proofHash)
    }

    @Test
    fun testStateWithWallets() {
        val wallets = listOf(
            WalletInfo("Phantom", "com.phantom", deeplink = "solana-wallet://phantom"),
            WalletInfo("Solflare", "com.solflare", deeplink = "solana-wallet://solflare")
        )

        val state = ProofSubmissionScreenState(wallets = wallets)

        assertEquals(2, state.wallets.size)
        assertEquals("Phantom", state.wallets[0].name)
        assertEquals("Solflare", state.wallets[1].name)
    }

    @Test
    fun testStateWithSelectedWallet() {
        val wallet = WalletInfo("Phantom", "com.phantom", deeplink = "solana-wallet://phantom")
        val state = ProofSubmissionScreenState(selectedWallet = wallet)

        assertNotNull(state.selectedWallet)
        assertEquals("Phantom", state.selectedWallet?.name)
    }

    @Test
    fun testStateWithSubmissionResult() {
        val result = ProofSubmissionResult(
            transactionSignature = "sig123abc",
            status = SubmissionStatus.CONFIRMED
        )

        val state = ProofSubmissionScreenState(lastResult = result)

        assertNotNull(state.lastResult)
        assertEquals(SubmissionStatus.CONFIRMED, state.lastResult?.status)
        assertEquals("sig123abc", state.lastResult?.transactionSignature)
    }

    @Test
    fun testStateWithError() {
        val state = ProofSubmissionScreenState(
            errorMessage = "Network error",
            statusMessage = "Failed to connect"
        )

        assertEquals("Network error", state.errorMessage)
        assertEquals("Failed to connect", state.statusMessage)
    }

    @Test
    fun testDiscoveringWallets() {
        val state = ProofSubmissionScreenState(
            isDiscoveringWallets = true,
            statusMessage = "Discovering wallets..."
        )

        assertTrue(state.isDiscoveringWallets)
        assertEquals("Discovering wallets...", state.statusMessage)
    }

    @Test
    fun testSubmittingProof() {
        val state = ProofSubmissionScreenState(
            isSubmitting = true,
            statusMessage = "Submitting proof..."
        )

        assertTrue(state.isSubmitting)
        assertEquals("Submitting proof...", state.statusMessage)
    }

    @Test
    fun testStatusProgression() {
        var state = ProofSubmissionScreenState()

        // Discover wallets
        state = state.copy(isDiscoveringWallets = true)
        assertTrue(state.isDiscoveringWallets)

        // Select wallet
        val wallet = WalletInfo("Phantom", "com.phantom", deeplink = "solana-wallet://phantom")
        state = state.copy(isDiscoveringWallets = false, selectedWallet = wallet)
        assertFalse(state.isDiscoveringWallets)
        assertNotNull(state.selectedWallet)

        // Submit proof
        state = state.copy(isSubmitting = true)
        assertTrue(state.isSubmitting)

        // Get result
        val result = ProofSubmissionResult(
            transactionSignature = "sig123",
            status = SubmissionStatus.CONFIRMED
        )
        state = state.copy(isSubmitting = false, lastResult = result)
        assertFalse(state.isSubmitting)
        assertEquals(SubmissionStatus.CONFIRMED, state.lastResult?.status)
    }

    @Test
    fun testFailureScenario() {
        val result = ProofSubmissionResult(
            transactionSignature = null,
            status = SubmissionStatus.FAILED,
            error = "Transaction failed"
        )

        val state = ProofSubmissionScreenState(
            lastResult = result,
            errorMessage = "Submission failed"
        )

        assertEquals(SubmissionStatus.FAILED, state.lastResult?.status)
        assertNull(state.lastResult?.transactionSignature)
        assertEquals("Transaction failed", state.lastResult?.error)
    }

    @Test
    fun testTimeoutScenario() {
        val result = ProofSubmissionResult(
            transactionSignature = "sig123",
            status = SubmissionStatus.TIMEOUT,
            error = "Confirmation timeout"
        )

        val state = ProofSubmissionScreenState(lastResult = result)

        assertEquals(SubmissionStatus.TIMEOUT, state.lastResult?.status)
    }

    @Test
    fun testMultipleWalletSelection() {
        val wallets = listOf(
            WalletInfo("Phantom", "com.phantom", deeplink = "solana-wallet://phantom"),
            WalletInfo("Solflare", "com.solflare", deeplink = "solana-wallet://solflare"),
            WalletInfo("Magic Eden", "com.magiceden", deeplink = "solana-wallet://magiceden")
        )

        var state = ProofSubmissionScreenState(wallets = wallets)

        // Select first wallet
        val phantom = wallets[0]
        state = state.copy(selectedWallet = phantom)
        assertEquals("Phantom", state.selectedWallet?.name)

        // Switch to second wallet
        val solflare = wallets[1]
        state = state.copy(selectedWallet = solflare)
        assertEquals("Solflare", state.selectedWallet?.name)

        // Switch to third wallet
        val magicEden = wallets[2]
        state = state.copy(selectedWallet = magicEden)
        assertEquals("Magic Eden", state.selectedWallet?.name)
    }

    @Test
    fun testCompleteFlow() {
        var state = ProofSubmissionScreenState(
            observationCount = 2,
            proofHash = "hash123"
        )

        // Step 1: Discover wallets
        val wallets = listOf(
            WalletInfo("Phantom", "com.phantom", deeplink = "solana-wallet://phantom")
        )
        state = state.copy(isDiscoveringWallets = true)
        state = state.copy(isDiscoveringWallets = false, wallets = wallets)

        // Step 2: Select wallet
        state = state.copy(selectedWallet = wallets[0])
        assertNotNull(state.selectedWallet)

        // Step 3: Submit proof
        state = state.copy(isSubmitting = true, statusMessage = "Submitting...")
        assertTrue(state.isSubmitting)

        // Step 4: Confirmation
        val result = ProofSubmissionResult(
            transactionSignature = "5Hg2T2s1V9e1R4x4q9p8o7n6m5l4k3j2i1h0g9f8e7d6c5b4a",
            status = SubmissionStatus.CONFIRMED,
            confirmationTime = 5000
        )
        state = state.copy(isSubmitting = false, lastResult = result)

        // Verify final state
        assertEquals(2, state.observationCount)
        assertEquals("hash123", state.proofHash)
        assertNotNull(state.selectedWallet)
        assertFalse(state.isSubmitting)
        assertEquals(SubmissionStatus.CONFIRMED, state.lastResult?.status)
    }
}
