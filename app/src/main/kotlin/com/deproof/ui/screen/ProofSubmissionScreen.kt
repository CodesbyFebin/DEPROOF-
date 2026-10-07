package com.deproof.ui.screen

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.deproof.data.solana.ProofSubmissionResult
import com.deproof.data.solana.SubmissionStatus
import com.deproof.data.solana.WalletInfo
import kotlinx.coroutines.delay

data class ProofSubmissionScreenState(
    val observationCount: Int = 0,
    val proofHash: String = "",
    val wallets: List<WalletInfo> = emptyList(),
    val selectedWallet: WalletInfo? = null,
    val isDiscoveringWallets: Boolean = false,
    val isSubmitting: Boolean = false,
    val lastResult: ProofSubmissionResult? = null,
    val errorMessage: String? = null,
    val statusMessage: String = "Ready to submit proof"
)

@Composable
fun ProofSubmissionScreen(
    state: ProofSubmissionScreenState = ProofSubmissionScreenState(),
    onDiscoverWallets: () -> Unit = {},
    onSelectWallet: (WalletInfo) -> Unit = {},
    onSubmitProof: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val scrollState = rememberScrollState()

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(Color(0x0A0E27))
            .verticalScroll(scrollState)
            .padding(16.dp),
        horizontalAlignment = Alignment.Start
    ) {
        // Header
        Text(
            "Proof Submission",
            fontSize = 28.sp,
            fontWeight = FontWeight.Bold,
            color = Color(0x00FF9F),
            fontFamily = FontFamily.Monospace
        )

        Spacer(modifier = Modifier.height(24.dp))

        // Proof Summary
        ProofSummarySection(
            observationCount = state.observationCount,
            proofHash = state.proofHash
        )

        Spacer(modifier = Modifier.height(24.dp))

        // Wallet Selection
        WalletSelectionSection(
            selectedWallet = state.selectedWallet,
            wallets = state.wallets,
            isDiscovering = state.isDiscoveringWallets,
            onDiscoverWallets = onDiscoverWallets,
            onSelectWallet = onSelectWallet
        )

        Spacer(modifier = Modifier.height(24.dp))

        // Submission Status
        SubmissionStatusSection(
            isSubmitting = state.isSubmitting,
            statusMessage = state.statusMessage,
            result = state.lastResult,
            errorMessage = state.errorMessage
        )

        Spacer(modifier = Modifier.height(24.dp))

        // Action Buttons
        ActionButtonsSection(
            selectedWallet = state.selectedWallet,
            isSubmitting = state.isSubmitting,
            onSubmitProof = onSubmitProof
        )

        Spacer(modifier = Modifier.height(32.dp))

        // Network Info
        NetworkInfoSection()
    }
}

@Composable
private fun ProofSummarySection(
    observationCount: Int,
    proofHash: String
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color(0x1A1F3A))
            .padding(16.dp),
        horizontalAlignment = Alignment.Start
    ) {
        Text(
            "Proof Summary",
            fontSize = 16.sp,
            fontWeight = FontWeight.Bold,
            color = Color(0x00D9FF)
        )

        Spacer(modifier = Modifier.height(12.dp))

        StatRow("Observations", observationCount.toString())
        StatRow("Proof Hash", if (proofHash.isEmpty()) "Not generated" else proofHash.take(16) + "...")
        StatRow("Status", "Ready to submit")
    }
}

@Composable
private fun WalletSelectionSection(
    selectedWallet: WalletInfo?,
    wallets: List<WalletInfo>,
    isDiscovering: Boolean,
    onDiscoverWallets: () -> Unit,
    onSelectWallet: (WalletInfo) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color(0x1A1F3A))
            .padding(16.dp),
        horizontalAlignment = Alignment.Start
    ) {
        Text(
            "Wallet Selection",
            fontSize = 16.sp,
            fontWeight = FontWeight.Bold,
            color = Color(0x00D9FF)
        )

        Spacer(modifier = Modifier.height(12.dp))

        if (selectedWallet != null) {
            StatRow("Selected Wallet", selectedWallet.name)
            StatRow("Package", selectedWallet.packageName)
        } else {
            Text(
                "No wallet selected",
                fontSize = 12.sp,
                color = Color(0xAAAAAA)
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        Button(
            onClick = onDiscoverWallets,
            enabled = !isDiscovering,
            modifier = Modifier
                .fillMaxWidth()
                .height(44.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = Color(0x00FF9F),
                contentColor = Color(0x0A0E27),
                disabledContainerColor = Color(0x00AA66),
                disabledContentColor = Color(0x0A0E27)
            )
        ) {
            if (isDiscovering) {
                CircularProgressIndicator(
                    modifier = Modifier.size(20.dp),
                    color = Color(0x0A0E27),
                    strokeWidth = 2.dp
                )
                Spacer(modifier = Modifier.width(8.dp))
            }
            Text(
                if (isDiscovering) "Discovering Wallets..." else "Discover Wallets",
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                fontFamily = FontFamily.Monospace
            )
        }

        if (wallets.isNotEmpty()) {
            Spacer(modifier = Modifier.height(12.dp))

            Text(
                "Available wallets:",
                fontSize = 12.sp,
                color = Color(0xAAAAAA)
            )

            Spacer(modifier = Modifier.height(8.dp))

            wallets.forEach { wallet ->
                Button(
                    onClick = { onSelectWallet(wallet) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(40.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (selectedWallet?.packageName == wallet.packageName)
                            Color(0x00D9FF) else Color(0x2A3F5F),
                        contentColor = if (selectedWallet?.packageName == wallet.packageName)
                            Color(0x0A0E27) else Color(0x00FF9F)
                    )
                ) {
                    Text(wallet.name, fontSize = 12.sp, fontFamily = FontFamily.Monospace)
                }
                Spacer(modifier = Modifier.height(8.dp))
            }
        }
    }
}

@Composable
private fun SubmissionStatusSection(
    isSubmitting: Boolean,
    statusMessage: String,
    result: ProofSubmissionResult?,
    errorMessage: String?
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color(0x1A1F3A))
            .padding(16.dp),
        horizontalAlignment = Alignment.Start
    ) {
        Text(
            "Submission Status",
            fontSize = 16.sp,
            fontWeight = FontWeight.Bold,
            color = Color(0x00D9FF)
        )

        Spacer(modifier = Modifier.height(12.dp))

        if (isSubmitting) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Start
            ) {
                CircularProgressIndicator(
                    modifier = Modifier.size(20.dp),
                    color = Color(0x00FF9F),
                    strokeWidth = 2.dp
                )
                Spacer(modifier = Modifier.width(12.dp))
                Text(statusMessage, fontSize = 12.sp, color = Color(0xCCCCCC))
            }
        } else if (result != null) {
            val statusColor = when (result.status) {
                SubmissionStatus.CONFIRMED -> Color(0x00FF9F)
                SubmissionStatus.FAILED -> Color(0xFF6B6B)
                SubmissionStatus.TIMEOUT -> Color(0xFFAA00)
                else -> Color(0x00D9FF)
            }

            StatRow("Status", result.status.name, statusColor)
            if (result.transactionSignature != null) {
                StatRow("TX Signature", result.transactionSignature.take(16) + "...")
            }
            if (result.error != null) {
                StatRow("Error", result.error)
            }
        } else {
            Text(statusMessage, fontSize = 12.sp, color = Color(0xAAAAAA))
        }

        if (errorMessage != null) {
            Spacer(modifier = Modifier.height(12.dp))
            Text(
                errorMessage,
                fontSize = 11.sp,
                color = Color(0xFF6B6B),
                fontFamily = FontFamily.Monospace
            )
        }
    }
}

@Composable
private fun ActionButtonsSection(
    selectedWallet: WalletInfo?,
    isSubmitting: Boolean,
    onSubmitProof: () -> Unit
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Button(
            onClick = onSubmitProof,
            enabled = selectedWallet != null && !isSubmitting,
            modifier = Modifier
                .fillMaxWidth()
                .height(48.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = Color(0x00FF9F),
                contentColor = Color(0x0A0E27),
                disabledContainerColor = Color(0x00AA66),
                disabledContentColor = Color(0x0A0E27)
            )
        ) {
            if (isSubmitting) {
                CircularProgressIndicator(
                    modifier = Modifier.size(20.dp),
                    color = Color(0x0A0E27),
                    strokeWidth = 2.dp
                )
                Spacer(modifier = Modifier.width(12.dp))
            }
            Text(
                if (isSubmitting) "Submitting..." else "Submit Proof",
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                fontFamily = FontFamily.Monospace
            )
        }

        Spacer(modifier = Modifier.height(12.dp))

        Text(
            if (selectedWallet == null) "Select a wallet to continue" else "Click to submit proof to ${selectedWallet.name}",
            fontSize = 11.sp,
            color = Color(0xAAAAAA),
            fontFamily = FontFamily.Monospace
        )
    }
}

@Composable
private fun NetworkInfoSection() {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color(0x1A1F3A))
            .padding(16.dp),
        horizontalAlignment = Alignment.Start
    ) {
        Text(
            "Network Configuration",
            fontSize = 14.sp,
            fontWeight = FontWeight.Bold,
            color = Color(0x00D9FF)
        )

        Spacer(modifier = Modifier.height(8.dp))

        StatRow("Network", "Solana Devnet", Color(0x00D9FF))
        StatRow("Endpoint", "api.devnet.solana.com", Color(0x00D9FF))
        StatRow("Confirmation", "Max 30 polls (60s)", Color(0x00D9FF))

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            "💡 Tip: Make sure your wallet app is installed and updated",
            fontSize = 10.sp,
            color = Color(0xAAAAAA),
            fontFamily = FontFamily.Monospace
        )
    }
}

@Composable
private fun StatRow(
    label: String,
    value: String,
    valueColor: Color = Color(0x00FF9F)
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            label,
            fontSize = 12.sp,
            color = Color(0xAAAAAA),
            fontFamily = FontFamily.Monospace
        )
        Text(
            value,
            fontSize = 12.sp,
            color = valueColor,
            fontFamily = FontFamily.Monospace
        )
    }
}
