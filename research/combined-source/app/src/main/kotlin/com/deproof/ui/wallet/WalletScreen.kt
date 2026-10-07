package com.deproof.ui.wallet

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.deproof.domain.repository.WalletAccount

@Composable
fun WalletScreen(
    viewModel: WalletViewModel,
    modifier: Modifier = Modifier
) {
    val walletState by viewModel.walletState.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val availableWallets by viewModel.availableWallets.collectAsState()

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text(
            "Wallet Connection",
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold
        )

        when (val state = walletState) {
            is WalletState.Disconnected -> {
                WalletConnectSection(
                    availableWallets = availableWallets,
                    isLoading = isLoading,
                    onConnectClick = { wallet ->
                        viewModel.connect(wallet)
                    }
                )
            }
            is WalletState.Connecting -> {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(100.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(MaterialTheme.colorScheme.surfaceVariant),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        CircularProgressIndicator(modifier = Modifier.size(32.dp))
                        Spacer(modifier = Modifier.height(8.dp))
                        Text("Connecting wallet...")
                    }
                }
            }
            is WalletState.Connected -> {
                WalletConnectedSection(
                    account = state.account,
                    onDisconnectClick = {
                        viewModel.disconnect()
                    }
                )
            }
            is WalletState.Error -> {
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(8.dp)),
                    color = MaterialTheme.colorScheme.errorContainer,
                    contentColor = MaterialTheme.colorScheme.onErrorContainer
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Text(
                            "Connection Error",
                            fontWeight = FontWeight.Bold
                        )
                        Text(state.message)
                        Button(
                            onClick = { viewModel.connect() },
                            modifier = Modifier.align(Alignment.End)
                        ) {
                            Text("Retry")
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun WalletConnectSection(
    availableWallets: List<String>,
    isLoading: Boolean,
    onConnectClick: (String) -> Unit
) {
    Column(
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text(
            "Select a wallet to connect",
            style = MaterialTheme.typography.bodyMedium
        )

        if (availableWallets.isEmpty()) {
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(8.dp)),
                color = MaterialTheme.colorScheme.surfaceVariant
            ) {
                Text(
                    "No wallets found. Please install Phantom, Solflare, or another Solana wallet.",
                    modifier = Modifier.padding(16.dp),
                    style = MaterialTheme.typography.bodySmall
                )
            }
        } else {
            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(availableWallets) { wallet ->
                    WalletButton(
                        walletName = wallet,
                        isLoading = isLoading,
                        onClick = { onConnectClick(wallet) }
                    )
                }
            }
        }
    }
}

@Composable
private fun WalletConnectedSection(
    account: WalletAccount,
    onDisconnectClick: () -> Unit
) {
    Column(
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(8.dp)),
            color = MaterialTheme.colorScheme.primaryContainer,
            contentColor = MaterialTheme.colorScheme.onPrimaryContainer
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text("Connected to ${account.name}", fontWeight = FontWeight.Bold)
                Text(
                    "Public Key: ${account.publicKey.take(8)}...${account.publicKey.takeLast(8)}",
                    style = MaterialTheme.typography.bodySmall
                )
            }
        }

        Button(
            onClick = onDisconnectClick,
            modifier = Modifier.fillMaxWidth(),
            colors = ButtonDefaults.buttonColors(
                containerColor = MaterialTheme.colorScheme.error
            )
        ) {
            Text("Disconnect Wallet")
        }
    }
}

@Composable
private fun WalletButton(
    walletName: String,
    isLoading: Boolean,
    onClick: () -> Unit
) {
    Button(
        onClick = onClick,
        modifier = Modifier
            .fillMaxWidth()
            .height(48.dp),
        enabled = !isLoading,
        shape = RoundedCornerShape(8.dp)
    ) {
        Text(walletName.replace("com.", "").replaceFirstChar { it.uppercase() })
    }
}
