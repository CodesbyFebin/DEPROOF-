package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.example.chain.Verdict
import com.example.data.AppDatabase
import com.example.data.Receipt
import com.example.domain.ReviewBinding
import com.example.domain.WalletSession
import com.example.domain.canSign
import com.example.util.HardwareSigning
import com.example.util.shortKey
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.util.*

@Composable
fun ReviewScreen(
    verdict: Verdict? = null,
    programId: String = "",
    accounts: List<String> = emptyList(),
    data: String = "",
    onReject: () -> Unit = {},
    onApprove: (Receipt) -> Unit = {},
    database: AppDatabase? = null,
    walletSession: WalletSession? = null
) {
    var messageHash by remember { mutableStateOf("") }
    var hashedMessage by remember { mutableStateOf("") }
    var isSigning by remember { mutableStateOf(false) }
    var signError by remember { mutableStateOf<String?>(null) }
    val scope = rememberCoroutineScope()

    LaunchedEffect(verdict, programId, data) {
        if (verdict != null && programId.isNotEmpty()) {
            val binding = ReviewBinding.prepareReview(programId, accounts, data)
            messageHash = binding.messageHash
            hashedMessage = binding.canonicalMessage
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.Top
    ) {
        Text(
            "Review",
            style = MaterialTheme.typography.headlineMedium,
            modifier = Modifier.padding(bottom = 16.dp)
        )

        if (verdict == null) {
            Text(
                "No transaction to review",
                style = MaterialTheme.typography.bodyMedium
            )
        } else {
            VerdictCard(verdict)

            Spacer(modifier = Modifier.height(16.dp))

            Text("Details", style = MaterialTheme.typography.titleSmall)
            Text("Program: ${shortKey(programId)}", style = MaterialTheme.typography.bodySmall)
            Text("Accounts: ${accounts.size}", style = MaterialTheme.typography.bodySmall)

            Spacer(modifier = Modifier.height(16.dp))

            Text("Hash (first 12): ${messageHash.take(12)}...", style = MaterialTheme.typography.labelSmall)

            Spacer(modifier = Modifier.height(24.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Button(
                    onClick = {
                        scope.launch {
                            val receipt = Receipt(
                                outcome = "rejected",
                                summary = "${verdict.title} - Rejected",
                                messageHash = messageHash,
                                signature = null,
                                slot = null,
                                broadcast = false,
                                timestamp = System.currentTimeMillis(),
                                submittedByClearance = true
                            )
                            database?.receiptDao()?.insert(receipt)
                            onReject()
                        }
                    },
                    modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) {
                    Text("Reject")
                }

                Button(
                    onClick = {
                        if (canSign(verdict) && walletSession?.isReadyForMainnet() == true) {
                            isSigning = true
                            signError = null
                            scope.launch(Dispatchers.IO) {
                                try {
                                    // Ensure hardware key exists
                                    HardwareSigning.createOrGetKey()

                                    // Sign the message hash with hardware-backed key
                                    val signature = HardwareSigning.signData(
                                        messageHash.chunked(2).map { it.toInt(16).toByte() }.toByteArray()
                                    )

                                    if (signature == null) {
                                        signError = "Failed to sign with hardware key"
                                    } else {
                                        val receipt = Receipt(
                                            outcome = "signed",
                                            summary = "${verdict.title} - Approved",
                                            messageHash = messageHash,
                                            signature = signature,
                                            slot = null,
                                            broadcast = false,
                                            timestamp = System.currentTimeMillis(),
                                            submittedByClearance = true
                                        )
                                        database?.receiptDao()?.insert(receipt)
                                        onApprove(receipt)
                                    }
                                } catch (e: Exception) {
                                    signError = e.message ?: "Unknown signing error"
                                } finally {
                                    isSigning = false
                                }
                            }
                        }
                    },
                    modifier = Modifier.weight(1f),
                    enabled = canSign(verdict) && walletSession?.isReadyForMainnet() == true && !isSigning
                ) {
                    Text(if (isSigning) "Signing..." else "Approve")
                }
            }

            if (!canSign(verdict)) {
                Text(
                    "Cannot sign: ${verdict.reason}",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.error,
                    modifier = Modifier.padding(top = 8.dp)
                )
            }

            if (walletSession == null) {
                Text(
                    "Wallet not connected",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.error,
                    modifier = Modifier.padding(top = 8.dp)
                )
            } else if (!walletSession.devnetMemoSigned) {
                Text(
                    "Devnet memo not signed. Sign a devnet memo first.",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.error,
                    modifier = Modifier.padding(top = 8.dp)
                )
            }

            if (signError != null) {
                Text(
                    "Signing failed: $signError",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.error,
                    modifier = Modifier.padding(top = 8.dp)
                )
            }
        }
    }
}

@Composable
fun VerdictCard(verdict: Verdict) {
    val bgColor = if (verdict.isPayable) {
        MaterialTheme.colorScheme.primaryContainer
    } else {
        MaterialTheme.colorScheme.errorContainer
    }
    val textColor = if (verdict.isPayable) {
        MaterialTheme.colorScheme.onPrimaryContainer
    } else {
        MaterialTheme.colorScheme.onErrorContainer
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .background(bgColor),
        colors = CardDefaults.cardColors(containerColor = bgColor)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                verdict.title,
                style = MaterialTheme.typography.headlineSmall,
                color = textColor,
                textAlign = TextAlign.Center
            )
            Text(
                verdict.reason,
                style = MaterialTheme.typography.bodyMedium,
                color = textColor,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(top = 8.dp)
            )
        }
    }
}
