package com.deproof.ui

import com.deproof.data.observations.AIZObservation
import com.deproof.data.observations.FluxObservation
import com.deproof.data.observations.Metrics
import com.deproof.data.observations.NodeMetrics
import com.deproof.data.solana.ProofSubmissionResult
import com.deproof.data.solana.SubmissionStatus
import com.deproof.data.solana.WalletInfo
import com.deproof.ui.screen.ProofSubmissionScreenState

/**
 * Test fixtures for consistent test data across all test suites
 */
object TestFixtures {

    // Observations
    val aizObservation = AIZObservation(
        source = "operator-supplied-cli-stats",
        sourceSha256 = "abc123def456abc123def456abc123def456abc123def456abc123def456abc1",
        metrics = Metrics(
            storageObjectCount = 42,
            storageSizeBytes = 1048576,
            upstreamSpeedRaw = 1024
        )
    )

    val fluxObservation = FluxObservation(
        source = "operator-flux-node",
        sourceSha256 = "def456abc123def456abc123def456abc123def456abc123def456abc123def456",
        endpoint = "192.168.1.100:5555",
        nodeMetrics = NodeMetrics(
            nodeId = "node-123",
            tier = "Cumulus",
            benchmarkScore = 850,
            uptime = 86400,
            cpuUsage = 45.2,
            memoryUsage = 62.8,
            storageUsage = 78.1,
            networkBandwidth = 100,
            collateralStatus = "LOCKED"
        )
    )

    // Wallets
    val phantomWallet = WalletInfo(
        name = "Phantom",
        packageName = "com.phantom",
        deeplink = "solana-wallet://phantom"
    )

    val solflareWallet = WalletInfo(
        name = "Solflare",
        packageName = "com.solflare",
        deeplink = "solana-wallet://solflare"
    )

    val magicEdenWallet = WalletInfo(
        name = "Magic Eden",
        packageName = "com.magiceden",
        deeplink = "solana-wallet://magiceden"
    )

    val allWallets = listOf(phantomWallet, solflareWallet, magicEdenWallet)

    // Submission Results
    val confirmedResult = ProofSubmissionResult(
        transactionSignature = "5Hg2T2s1V9e1R4x4q9p8o7n6m5l4k3j2i1h0g9f8e7d6c5b4a",
        status = SubmissionStatus.CONFIRMED,
        confirmationTime = 5000
    )

    val failedResult = ProofSubmissionResult(
        transactionSignature = null,
        status = SubmissionStatus.FAILED,
        error = "Insufficient balance for transaction"
    )

    val timeoutResult = ProofSubmissionResult(
        transactionSignature = "sig123abc",
        status = SubmissionStatus.TIMEOUT,
        error = "Confirmation timeout after 30 attempts"
    )

    val pendingResult = ProofSubmissionResult(
        transactionSignature = "pending123abc",
        status = SubmissionStatus.PENDING
    )

    // Screen States
    val initialState = ProofSubmissionScreenState()

    val stateWithObservations = ProofSubmissionScreenState(
        observationCount = 1,
        proofHash = "abc123def456abc123def456abc123def456abc123def456abc123def456abc1"
    )

    val stateWithWallets = ProofSubmissionScreenState(
        wallets = allWallets
    )

    val stateWithSelectedWallet = ProofSubmissionScreenState(
        selectedWallet = phantomWallet
    )

    val stateDiscovering = ProofSubmissionScreenState(
        isDiscoveringWallets = true,
        statusMessage = "Discovering wallets..."
    )

    val stateSubmitting = ProofSubmissionScreenState(
        isSubmitting = true,
        statusMessage = "Submitting proof..."
    )

    val stateWithConfirmedResult = ProofSubmissionScreenState(
        selectedWallet = phantomWallet,
        lastResult = confirmedResult,
        statusMessage = "✅ Proof successfully submitted!"
    )

    val stateWithFailedResult = ProofSubmissionScreenState(
        selectedWallet = phantomWallet,
        lastResult = failedResult,
        errorMessage = "Submission failed",
        statusMessage = "❌ Submission failed: Insufficient balance for transaction"
    )

    val stateWithError = ProofSubmissionScreenState(
        errorMessage = "Network connection failed",
        statusMessage = "Failed to connect to blockchain"
    )

    // Proof hashes
    const val VALID_PROOF_HASH = "abc123def456abc123def456abc123def456abc123def456abc123def456abc1"
    const val TRANSACTION_SIGNATURE = "5Hg2T2s1V9e1R4x4q9p8o7n6m5l4k3j2i1h0g9f8e7d6c5b4a"
    const val WALLET_PUBLIC_KEY = "11111111111111111111111111111111"
}

/**
 * Test builders for creating custom fixtures
 */
class TestScreenStateBuilder {
    private var observationCount = 0
    private var proofHash = ""
    private var wallets = listOf<WalletInfo>()
    private var selectedWallet: WalletInfo? = null
    private var isDiscoveringWallets = false
    private var isSubmitting = false
    private var lastResult: ProofSubmissionResult? = null
    private var errorMessage: String? = null
    private var statusMessage = "Ready to submit proof"

    fun withObservationCount(count: Int) = apply { observationCount = count }
    fun withProofHash(hash: String) = apply { proofHash = hash }
    fun withWallets(wallets: List<WalletInfo>) = apply { this.wallets = wallets }
    fun withSelectedWallet(wallet: WalletInfo?) = apply { selectedWallet = wallet }
    fun withDiscovering(discovering: Boolean) = apply { isDiscoveringWallets = discovering }
    fun withSubmitting(submitting: Boolean) = apply { isSubmitting = submitting }
    fun withResult(result: ProofSubmissionResult?) = apply { lastResult = result }
    fun withError(error: String?) = apply { errorMessage = error }
    fun withStatusMessage(message: String) = apply { statusMessage = message }

    fun build(): ProofSubmissionScreenState {
        return ProofSubmissionScreenState(
            observationCount = observationCount,
            proofHash = proofHash,
            wallets = wallets,
            selectedWallet = selectedWallet,
            isDiscoveringWallets = isDiscoveringWallets,
            isSubmitting = isSubmitting,
            lastResult = lastResult,
            errorMessage = errorMessage,
            statusMessage = statusMessage
        )
    }
}
