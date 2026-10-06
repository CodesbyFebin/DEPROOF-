package com.deproof.presentation.ui.screen

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.deproof.presentation.viewmodel.ReceiptsViewModel
import com.deproof.util.Formatters

@Composable
fun ReceiptsScreen(viewModel: ReceiptsViewModel) {
    val uiState by viewModel.uiState.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Contribution Receipts (${uiState.receiptCount})") }
            )
        }
    ) { paddingValues ->
        if (uiState.receipts.isEmpty()) {
            EmptyReceiptsView(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
            )
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                item {
                    ReceiptSummaryCard(
                        totalCount = uiState.receiptCount,
                        receipts = uiState.receipts
                    )
                }

                items(uiState.receipts) { receipt ->
                    ReceiptItemCard(
                        receipt = receipt,
                        onSelect = { viewModel.selectReceipt(receipt) },
                        onExport = { viewModel.exportReceiptAsJson(receipt) },
                        onDelete = { viewModel.deleteReceipt(receipt) }
                    )
                }
            }
        }

        uiState.selectedReceipt?.let { receipt ->
            ReceiptDetailDialog(
                receipt = receipt,
                onDismiss = { viewModel.deselectReceipt() },
                onExport = { viewModel.exportReceiptAsJson(receipt) }
            )
        }

        if (uiState.error != null) {
            ErrorSnackbar(
                message = uiState.error ?: "Unknown error",
                onDismiss = { viewModel.clearError() }
            )
        }

        if (uiState.successMessage != null) {
            SuccessSnackbar(
                message = uiState.successMessage ?: "Success",
                onDismiss = { viewModel.clearSuccessMessage() }
            )
        }

        uiState.exportedJson?.let { json ->
            ExportedJsonDialog(
                json = json,
                onDismiss = { viewModel.clearExportedJson() }
            )
        }
    }
}

@Composable
private fun EmptyReceiptsView(modifier: Modifier = Modifier) {
    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            "No Receipts Yet",
            style = MaterialTheme.typography.headlineSmall
        )
        Text(
            "Sign transactions to create receipts",
            style = MaterialTheme.typography.bodyMedium
        )
    }
}

@Composable
private fun ReceiptSummaryCard(
    totalCount: Int,
    receipts: List<com.deproof.domain.model.Receipt>
) {
    val signedCount = receipts.count {
        it.signatureStatus == com.deproof.domain.model.SignatureStatus.SIGNED
    }
    val percentage = if (totalCount > 0) (signedCount * 100) / totalCount else 0

    Card(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                "Receipt Summary",
                style = MaterialTheme.typography.headlineSmall
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    Text("Total Receipts", style = MaterialTheme.typography.labelSmall)
                    Text(totalCount.toString(), style = MaterialTheme.typography.headlineSmall)
                }
                Column {
                    Text("Signed", style = MaterialTheme.typography.labelSmall)
                    Text("$signedCount ($percentage%)", style = MaterialTheme.typography.headlineSmall)
                }
            }

            LinearProgressIndicator(
                progress = percentage / 100f,
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}

@Composable
private fun ReceiptItemCard(
    receipt: com.deproof.domain.model.Receipt,
    onSelect: () -> Unit,
    onExport: () -> Unit,
    onDelete: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        onClick = onSelect
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        Formatters.formatHash(receipt.transactionHash),
                        style = MaterialTheme.typography.bodySmall
                    )
                    Text(
                        "Verdict: ${receipt.verdict}",
                        style = MaterialTheme.typography.labelSmall
                    )
                }

                Spacer(modifier = Modifier.width(8.dp))

                Text(
                    receipt.signatureStatus.name,
                    style = MaterialTheme.typography.labelSmall,
                    color = when (receipt.signatureStatus) {
                        com.deproof.domain.model.SignatureStatus.SIGNED -> MaterialTheme.colorScheme.primary
                        com.deproof.domain.model.SignatureStatus.REJECTED -> MaterialTheme.colorScheme.error
                        else -> MaterialTheme.colorScheme.tertiary
                    }
                )
            }

            Text(
                Formatters.formatTimestamp(receipt.timestamp),
                style = MaterialTheme.typography.labelSmall
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End,
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onExport) {
                    Text("Export")
                }
                IconButton(onClick = onDelete) {
                    Icon(Icons.Default.Delete, contentDescription = "Delete")
                }
            }
        }
    }
}

@Composable
private fun ReceiptDetailDialog(
    receipt: com.deproof.domain.model.Receipt,
    onDismiss: () -> Unit,
    onExport: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Receipt Details") },
        text = {
            Column(
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                DetailRow("ID", receipt.id)
                DetailRow("Tx Hash", Formatters.formatHash(receipt.transactionHash))
                DetailRow("Verdict", receipt.verdict)
                DetailRow("Message Hash", Formatters.formatHash(receipt.messageHash))
                DetailRow("Status", receipt.signatureStatus.name)
                DetailRow("Timestamp", Formatters.formatTimestamp(receipt.timestamp))
            }
        },
        confirmButton = {
            Button(onClick = onExport) {
                Text("Export JSON")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Close")
            }
        }
    )
}

@Composable
private fun DetailRow(label: String, value: String) {
    Column {
        Text(label, style = MaterialTheme.typography.labelSmall)
        Text(value, style = MaterialTheme.typography.bodySmall)
    }
}

@Composable
private fun ExportedJsonDialog(json: String, onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Exported Receipt") },
        text = {
            Text(json, style = MaterialTheme.typography.bodySmall)
        },
        confirmButton = {
            Button(onClick = onDismiss) {
                Text("Close")
            }
        }
    )
}

@Composable
private fun ErrorSnackbar(message: String, onDismiss: () -> Unit) {
    Snackbar(
        modifier = Modifier.padding(16.dp),
        action = {
            TextButton(onClick = onDismiss) {
                Text("Dismiss")
            }
        }
    ) {
        Text(message)
    }
}

@Composable
private fun SuccessSnackbar(message: String, onDismiss: () -> Unit) {
    Snackbar(
        modifier = Modifier.padding(16.dp),
        action = {
            TextButton(onClick = onDismiss) {
                Text("OK")
            }
        }
    ) {
        Text(message)
    }
}
