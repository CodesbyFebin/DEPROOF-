package com.deproof.presentation.ui.screen

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import com.deproof.presentation.viewmodel.NowViewModel
import com.deproof.util.Formatters

@Composable
fun NowScreen(viewModel: NowViewModel) {
    val uiState by viewModel.uiState.collectAsState()
    var addressInput by remember { mutableStateOf("") }
    val clipboardManager = LocalClipboardManager.current

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Now") },
                actions = {
                    IconButton(onClick = { viewModel.refreshAll() }) {
                        Icon(Icons.Default.Refresh, contentDescription = "Refresh")
                    }
                }
            )
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
                WalletConnectionCard(
                    walletInfo = uiState.walletInfo,
                    onConnect = { address ->
                        if (address.isNotEmpty()) {
                            viewModel.connectWallet(address)
                        }
                    },
                    onDisconnect = { viewModel.disconnectWallet() },
                    addressInput = addressInput,
                    onAddressChange = { addressInput = it },
                    clipboardManager = clipboardManager
                )
            }

            if (uiState.walletInfo != null) {
                item {
                    BalanceCard(
                        label = "SOL Balance",
                        balance = uiState.solBalance,
                        isLoading = uiState.isLoading
                    )
                }

                item {
                    BalanceCard(
                        label = "SKR Balance",
                        balance = uiState.skrBalance,
                        isLoading = uiState.isLoading
                    )
                }

                item {
                    Text(
                        "Recent Signatures",
                        style = MaterialTheme.typography.headlineSmall
                    )
                }

                items(uiState.recentSignatures) { signature ->
                    SignatureCard(signature = signature)
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

            if (uiState.recentSignatures.isEmpty() && uiState.walletInfo != null && !uiState.isLoading) {
                item {
                    Text(
                        "No recent signatures",
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
            }
        }
    }
}

@Composable
private fun WalletConnectionCard(
    walletInfo: com.deproof.domain.model.WalletInfo?,
    onConnect: (String) -> Unit,
    onDisconnect: () -> Unit,
    addressInput: String,
    onAddressChange: (String) -> Unit,
    clipboardManager: androidx.compose.ui.platform.ClipboardManager
) {
    Card(
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            if (walletInfo == null) {
                Text(
                    "Connect Your Wallet",
                    style = MaterialTheme.typography.headlineSmall
                )

                TextField(
                    value = addressInput,
                    onValueChange = onAddressChange,
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text("Solana Address") },
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                    keyboardActions = KeyboardActions(
                        onDone = { onConnect(addressInput) }
                    ),
                    singleLine = true
                )

                Button(
                    onClick = { onConnect(addressInput) },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Connect")
                }
            } else {
                Text(
                    "Connected to ${walletInfo.walletType}",
                    style = MaterialTheme.typography.headlineSmall
                )

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(MaterialTheme.colorScheme.surfaceVariant, shape = RoundedCornerShape(4.dp))
                        .padding(8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        Formatters.formatAddress(walletInfo.publicKey),
                        style = MaterialTheme.typography.bodySmall
                    )
                    IconButton(onClick = {
                        val annotatedString = AnnotatedString(walletInfo.publicKey)
                        clipboardManager.setText(annotatedString)
                    }) {
                        Icon(Icons.Default.ContentCopy, contentDescription = "Copy")
                    }
                }

                Button(
                    onClick = onDisconnect,
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.error
                    )
                ) {
                    Text("Disconnect")
                }
            }
        }
    }
}

@Composable
private fun BalanceCard(
    label: String,
    balance: com.deproof.domain.model.Balance?,
    isLoading: Boolean
) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Text(label, style = MaterialTheme.typography.labelSmall)

            if (isLoading) {
                CircularProgressIndicator(modifier = Modifier.size(24.dp))
            } else if (balance != null) {
                Text(
                    balance.displayAmount,
                    style = MaterialTheme.typography.headlineSmall
                )
                Text(
                    "Amount: ${balance.amount}",
                    style = MaterialTheme.typography.bodySmall
                )
            } else {
                Text("No balance data", style = MaterialTheme.typography.bodyMedium)
            }
        }
    }
}

@Composable
private fun SignatureCard(signature: com.deproof.domain.model.SignatureInfo) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp)
        ) {
            Text(
                Formatters.formatHash(signature.signature),
                style = MaterialTheme.typography.bodySmall
            )
            Text(
                "Slot: ${Formatters.formatSlot(signature.slot)}",
                style = MaterialTheme.typography.labelSmall
            )
            Text(
                "Time: ${Formatters.formatTimestamp(signature.blockTime)}",
                style = MaterialTheme.typography.labelSmall
            )
            Text(
                signature.status.name,
                style = MaterialTheme.typography.labelSmall,
                color = if (signature.status == com.deproof.domain.model.TransactionStatus.SUCCESS)
                    MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error
            )
        }
    }
}

@Composable
private fun ErrorCard(message: String, onDismiss: () -> Unit) {
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
