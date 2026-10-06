package com.deproof.presentation.ui.screen

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Error
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import com.deproof.domain.model.Verdict
import com.deproof.presentation.viewmodel.ReviewViewModel
import com.deproof.util.Formatters

@Composable
fun ReviewScreen(viewModel: ReviewViewModel) {
    val uiState by viewModel.uiState.collectAsState()
    var signatureInput by remember { mutableStateOf("") }

    Scaffold(
        topBar = {
            TopAppBar(title = { Text("Review Transaction") })
        }
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item {
                TransactionInputCard(
                    signatureInput = signatureInput,
                    onSignatureChange = { signatureInput = it },
                    onLoad = { viewModel.loadTransaction(signatureInput) },
                    isLoading = uiState.isLoadingTransaction
                )
            }

            if (uiState.transactionHash.isNotEmpty()) {
                item {
                    TransactionDetailsCard(
                        txHash = uiState.transactionHash,
                        transactionData = uiState.transactionData
                    )
                }

                uiState.verdict?.let {
                    item {
                        VerdictCard(verdict = it)
                    }
                }

                uiState.messageBinding?.let { binding ->
                    item {
                        MessageBindingCard(binding = binding)
                    }
                }

                uiState.tamperDetectionAlert?.let { alert ->
                    item {
                        TamperDetectionCard(alert = alert)
                    }
                }

                item {
                    ReviewActionButtons(
                        isProcessing = uiState.isSigning,
                        onApprove = { viewModel.approveAndSign() },
                        onReject = { viewModel.reject() },
                        onVerify = { viewModel.verifyMessageIntegrity() }
                    )
                }
            }

            if (uiState.error != null) {
                item {
                    ErrorCard(
                        message = uiState.error ?: "Unknown error",
                        onDismiss = { viewModel.clearError() }
                    )
                }
            }

            if (uiState.signatureResult != null) {
                item {
                    SuccessCard(
                        message = uiState.signatureResult ?: "Success",
                        onDismiss = { viewModel.clearSignatureResult() }
                    )
                }
            }
        }
    }
}

@Composable
private fun TransactionInputCard(
    signatureInput: String,
    onSignatureChange: (String) -> Unit,
    onLoad: () -> Unit,
    isLoading: Boolean
) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                "Enter Transaction Signature",
                style = MaterialTheme.typography.headlineSmall
            )

            TextField(
                value = signatureInput,
                onValueChange = onSignatureChange,
                modifier = Modifier.fillMaxWidth(),
                label = { Text("Transaction Signature") },
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                keyboardActions = KeyboardActions(
                    onDone = { onLoad() }
                ),
                singleLine = true
            )

            Button(
                onClick = onLoad,
                modifier = Modifier.fillMaxWidth(),
                enabled = !isLoading && signatureInput.isNotEmpty()
            ) {
                if (isLoading) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(20.dp),
                        color = MaterialTheme.colorScheme.onPrimary
                    )
                } else {
                    Text("Load Transaction")
                }
            }
        }
    }
}

@Composable
private fun TransactionDetailsCard(txHash: String, transactionData: String) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                "Transaction Details",
                style = MaterialTheme.typography.headlineSmall
            )

            Text(
                "Hash:",
                style = MaterialTheme.typography.labelSmall
            )
            Text(
                Formatters.formatHash(txHash),
                style = MaterialTheme.typography.bodySmall
            )

            if (transactionData.isNotEmpty()) {
                Text(
                    "Status: Loaded",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.primary
                )
            }
        }
    }
}

@Composable
private fun VerdictCard(verdict: Verdict) {
    val (icon, color, text) = when (verdict) {
        is Verdict.Payable -> Triple(Icons.Default.CheckCircle, MaterialTheme.colorScheme.primary, "PAYABLE")
        is Verdict.DoNotSign -> Triple(Icons.Default.Error, MaterialTheme.colorScheme.error, "DO NOT SIGN")
        is Verdict.Unknown -> Triple(Icons.Default.Error, MaterialTheme.colorScheme.warning, verdict.reason)
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = color.copy(alpha = 0.1f)
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = icon,
                contentDescription = "Verdict",
                tint = color,
                modifier = Modifier.size(32.dp)
            )

            Column {
                Text(
                    "Verdict",
                    style = MaterialTheme.typography.labelSmall
                )
                Text(
                    text,
                    style = MaterialTheme.typography.headlineSmall,
                    color = color
                )
            }
        }
    }
}

@Composable
private fun MessageBindingCard(binding: com.deproof.domain.model.ReviewBinding) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                "Message Binding",
                style = MaterialTheme.typography.headlineSmall
            )

            Text(
                "Hash:",
                style = MaterialTheme.typography.labelSmall
            )
            Text(
                Formatters.formatHash(binding.messageHash),
                style = MaterialTheme.typography.bodySmall
            )

            Text(
                "Verdict: ${binding.verdict}",
                style = MaterialTheme.typography.bodySmall
            )
        }
    }
}

@Composable
private fun TamperDetectionCard(alert: String) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.errorContainer
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Text(
                "⚠️ Tamper Detection Alert",
                style = MaterialTheme.typography.headlineSmall,
                color = MaterialTheme.colorScheme.error
            )
            Text(
                alert,
                style = MaterialTheme.typography.bodySmall
            )
        }
    }
}

@Composable
private fun ReviewActionButtons(
    isProcessing: Boolean,
    onApprove: () -> Unit,
    onReject: () -> Unit,
    onVerify: () -> Unit
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Button(
            onClick = onApprove,
            modifier = Modifier.fillMaxWidth(),
            enabled = !isProcessing,
            colors = ButtonDefaults.buttonColors(
                containerColor = MaterialTheme.colorScheme.primary
            )
        ) {
            if (isProcessing) {
                CircularProgressIndicator(modifier = Modifier.size(20.dp))
            } else {
                Text("Approve & Sign")
            }
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Button(
                onClick = onReject,
                modifier = Modifier.weight(1f),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.error
                )
            ) {
                Text("Reject")
            }

            OutlinedButton(
                onClick = onVerify,
                modifier = Modifier.weight(1f)
            ) {
                Text("Verify")
            }
        }
    }
}

@Composable
private fun SuccessCard(message: String, onDismiss: () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Text(message, style = MaterialTheme.typography.bodySmall)
            Button(
                onClick = onDismiss,
                modifier = Modifier.align(Alignment.End)
            ) {
                Text("Dismiss")
            }
        }
    }
}
