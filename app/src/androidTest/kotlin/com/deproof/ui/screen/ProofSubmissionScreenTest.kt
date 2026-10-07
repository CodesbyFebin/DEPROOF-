package com.deproof.ui.screen
import org.junit.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue
import kotlin.test.assertFalse

import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import com.deproof.data.observations.AIZObservation
import com.deproof.data.observations.Metrics
import com.deproof.data.solana.ProofSubmissionResult
import com.deproof.data.solana.SubmissionStatus
import com.deproof.data.solana.WalletInfo
import org.junit.Rule

class ProofSubmissionScreenComposableTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    private val testWallet = WalletInfo(
        name = "Phantom",
        packageName = "com.phantom",
        deeplink = "solana-wallet://phantom"
    )

    private val testState = ProofSubmissionScreenState(
        observationCount = 2,
        proofHash = "abc123def456abc123def456abc123def456abc123def456abc123def456abc1",
        wallets = listOf(testWallet),
        selectedWallet = null,
        isDiscoveringWallets = false,
        isSubmitting = false,
        lastResult = null,
        errorMessage = null,
        statusMessage = "Ready to submit proof"
    )

    @Test
    fun testScreenRendersWithInitialState() {
        composeTestRule.setContent {
            ProofSubmissionScreen(state = testState)
        }

        // Check header is displayed
        composeTestRule.onNodeWithText("Proof Submission").assertIsDisplayed()

        // Check proof summary section
        composeTestRule.onNodeWithText("Proof Summary").assertIsDisplayed()
        composeTestRule.onNodeWithText("2").assertIsDisplayed() // observation count

        // Check wallet selection section
        composeTestRule.onNodeWithText("Wallet Selection").assertIsDisplayed()
    }

    @Test
    fun testDiscoverWalletsButtonIsEnabled() {
        composeTestRule.setContent {
            ProofSubmissionScreen(state = testState)
        }

        composeTestRule.onNodeWithText("Discover Wallets")
            .assertIsEnabled()
    }

    @Test
    fun testDiscoverWalletsButtonDisabledDuringDiscovery() {
        val discoveringState = testState.copy(isDiscoveringWallets = true)

        composeTestRule.setContent {
            ProofSubmissionScreen(state = discoveringState)
        }

        composeTestRule.onNodeWithText("Discovering Wallets...")
            .assertIsNotEnabled()
    }

    @Test
    fun testWalletSelectionButtonsDisplayed() {
        composeTestRule.setContent {
            ProofSubmissionScreen(state = testState)
        }

        composeTestRule.onNodeWithText("Phantom").assertIsDisplayed()
    }

    @Test
    fun testSelectedWalletHighlighted() {
        val stateWithSelection = testState.copy(selectedWallet = testWallet)

        composeTestRule.setContent {
            ProofSubmissionScreen(state = stateWithSelection)
        }

        composeTestRule.onNodeWithText("Selected Wallet").assertIsDisplayed()
        composeTestRule.onNodeWithText("Phantom").assertIsDisplayed()
    }

    @Test
    fun testSubmitButtonDisabledWithoutWallet() {
        composeTestRule.setContent {
            ProofSubmissionScreen(state = testState)
        }

        composeTestRule.onNodeWithText("Submit Proof")
            .assertIsNotEnabled()
    }

    @Test
    fun testSubmitButtonEnabledWithWallet() {
        val stateWithWallet = testState.copy(selectedWallet = testWallet)

        composeTestRule.setContent {
            ProofSubmissionScreen(state = stateWithWallet)
        }

        composeTestRule.onNodeWithText("Submit Proof")
            .assertIsEnabled()
    }

    @Test
    fun testSubmitButtonDisabledDuringSubmission() {
        val submittingState = testState.copy(
            selectedWallet = testWallet,
            isSubmitting = true
        )

        composeTestRule.setContent {
            ProofSubmissionScreen(state = submittingState)
        }

        composeTestRule.onNodeWithText("Submitting...")
            .assertIsNotEnabled()
    }

    @Test
    fun testStatusMessageDisplayed() {
        val customStatusState = testState.copy(
            statusMessage = "Custom status message"
        )

        composeTestRule.setContent {
            ProofSubmissionScreen(state = customStatusState)
        }

        composeTestRule.onNodeWithText("Custom status message").assertIsDisplayed()
    }

    @Test
    fun testConfirmedStatusDisplayed() {
        val confirmedResult = ProofSubmissionResult(
            transactionSignature = "5Hg2T2s1V9e1R4x4q9p8o7n6m5l4k3j2i1h0g9f8e7d6c5b4a",
            status = SubmissionStatus.CONFIRMED
        )

        val resultState = testState.copy(
            selectedWallet = testWallet,
            isSubmitting = false,
            lastResult = confirmedResult,
            statusMessage = "✅ Proof successfully submitted!"
        )

        composeTestRule.setContent {
            ProofSubmissionScreen(state = resultState)
        }

        composeTestRule.onNodeWithText("✅ Proof successfully submitted!").assertIsDisplayed()
        composeTestRule.onNodeWithText("CONFIRMED").assertIsDisplayed()
    }

    @Test
    fun testFailedStatusDisplayed() {
        val failedResult = ProofSubmissionResult(
            transactionSignature = null,
            status = SubmissionStatus.FAILED,
            error = "Transaction failed"
        )

        val resultState = testState.copy(
            selectedWallet = testWallet,
            isSubmitting = false,
            lastResult = failedResult,
            statusMessage = "❌ Submission failed: Transaction failed"
        )

        composeTestRule.setContent {
            ProofSubmissionScreen(state = resultState)
        }

        composeTestRule.onNodeWithText("❌ Submission failed: Transaction failed").assertIsDisplayed()
        composeTestRule.onNodeWithText("FAILED").assertIsDisplayed()
    }

    @Test
    fun testErrorMessageDisplayed() {
        val errorState = testState.copy(
            errorMessage = "Network connection failed"
        )

        composeTestRule.setContent {
            ProofSubmissionScreen(state = errorState)
        }

        composeTestRule.onNodeWithText("Network connection failed").assertIsDisplayed()
    }

    @Test
    fun testCallbackOnDiscoverWallets() {
        var callbackInvoked = false

        composeTestRule.setContent {
            ProofSubmissionScreen(
                state = testState,
                onDiscoverWallets = { callbackInvoked = true }
            )
        }

        composeTestRule.onNodeWithText("Discover Wallets").performClick()
        assert(callbackInvoked)
    }

    @Test
    fun testCallbackOnSelectWallet() {
        var selectedWallet: WalletInfo? = null

        composeTestRule.setContent {
            ProofSubmissionScreen(
                state = testState,
                onSelectWallet = { selectedWallet = it }
            )
        }

        composeTestRule.onNodeWithText("Phantom").performClick()
        assert(selectedWallet?.name == "Phantom")
    }

    @Test
    fun testCallbackOnSubmitProof() {
        var submitInvoked = false
        val stateWithWallet = testState.copy(selectedWallet = testWallet)

        composeTestRule.setContent {
            ProofSubmissionScreen(
                state = stateWithWallet,
                onSubmitProof = { submitInvoked = true }
            )
        }

        composeTestRule.onNodeWithText("Submit Proof").performClick()
        assert(submitInvoked)
    }

    @Test
    fun testNetworkInfoSectionDisplayed() {
        composeTestRule.setContent {
            ProofSubmissionScreen(state = testState)
        }

        composeTestRule.onNodeWithText("Network Configuration").assertIsDisplayed()
        composeTestRule.onNodeWithText("Solana Devnet").assertIsDisplayed()
    }

    @Test
    fun testProofHashTruncatedDisplay() {
        composeTestRule.setContent {
            ProofSubmissionScreen(state = testState)
        }

        // Hash should be truncated with "..." suffix
        val hashDisplay = testState.proofHash.take(16) + "..."
        composeTestRule.onNodeWithText(hashDisplay).assertIsDisplayed()
    }

    @Test
    fun testMultipleWalletsDisplayed() {
        val multiWalletState = testState.copy(
            wallets = listOf(
                WalletInfo("Phantom", "com.phantom", "solana-wallet://phantom"),
                WalletInfo("Solflare", "com.solflare", "solana-wallet://solflare"),
                WalletInfo("Magic Eden", "com.magiceden", "solana-wallet://magiceden")
            )
        )

        composeTestRule.setContent {
            ProofSubmissionScreen(state = multiWalletState)
        }

        composeTestRule.onNodeWithText("Phantom").assertIsDisplayed()
        composeTestRule.onNodeWithText("Solflare").assertIsDisplayed()
        composeTestRule.onNodeWithText("Magic Eden").assertIsDisplayed()
    }

    @Test
    fun testTimeoutStatusDisplayed() {
        val timeoutResult = ProofSubmissionResult(
            transactionSignature = "sig123",
            status = SubmissionStatus.TIMEOUT,
            error = "Confirmation timeout"
        )

        val timeoutState = testState.copy(
            selectedWallet = testWallet,
            lastResult = timeoutResult,
            statusMessage = "⏱️ Transaction confirmation timeout"
        )

        composeTestRule.setContent {
            ProofSubmissionScreen(state = timeoutState)
        }

        composeTestRule.onNodeWithText("⏱️ Transaction confirmation timeout").assertIsDisplayed()
        composeTestRule.onNodeWithText("TIMEOUT").assertIsDisplayed()
    }
}
