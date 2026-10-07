package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.example.domain.MWAAdapter
import com.example.domain.WalletSession
import com.example.util.shortKey
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun WalletSection(
    onWalletConnected: (WalletSession?) -> Unit,
    currentSession: WalletSession? = null
) {
    var isConnecting by remember { mutableStateOf(false) }
    var isSigningMemo by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }
    val scope = rememberCoroutineScope()

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = 16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text("Wallet", style = MaterialTheme.typography.titleMedium)
            Spacer(modifier = Modifier.height(8.dp))

            if (currentSession == null) {
                Button(
                    onClick = {
                        isConnecting = true
                        error = null
                        scope.launch {
                            val result = MWAAdapter.authorize()
                            when {
                                result.isSuccess -> {
                                    val session = result.getOrNull()
                                    if (session != null) {
                                        onWalletConnected(session)
                                    }
                                }
                                else -> {
                                    error = result.exceptionOrNull()?.message ?: "Connection failed"
                                }
                            }
                            isConnecting = false
                        }
                    },
                    modifier = Modifier.fillMaxWidth(),
                    enabled = !isConnecting
                ) {
                    Text(if (isConnecting) "Connecting..." else "Connect Wallet")
                }
            } else {
                Text(
                    "Connected: ${shortKey(currentSession.publicKey)}",
                    style = MaterialTheme.typography.bodySmall
                )
                Text(
                    "Wallet: ${currentSession.walletName}",
                    style = MaterialTheme.typography.labelSmall,
                    color = Color.Gray,
                    modifier = Modifier.padding(top = 4.dp)
                )

                Spacer(modifier = Modifier.height(12.dp))

                if (currentSession.devnetMemoSigned) {
                    Text(
                        "✓ Devnet memo signed",
                        style = MaterialTheme.typography.labelSmall,
                        color = Color(0xFF51CF66),
                        modifier = Modifier.padding(top = 4.dp)
                    )
                    Text(
                        SimpleDateFormat("MM/dd HH:mm", Locale.US).format(
                            Date(currentSession.devnetMemoTimestamp)
                        ),
                        style = MaterialTheme.typography.labelSmall,
                        color = Color.Gray
                    )
                } else {
                    Button(
                        onClick = {
                            isSigningMemo = true
                            error = null
                            scope.launch {
                                // Sign devnet memo
                                isSigningMemo = false
                                error = "Devnet memo signing pending"
                            }
                        },
                        modifier = Modifier.fillMaxWidth(),
                        enabled = !isSigningMemo
                    ) {
                        Text(if (isSigningMemo) "Signing..." else "Sign Devnet Memo")
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                Button(
                    onClick = {
                        onWalletConnected(null)
                        error = null
                    },
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) {
                    Text("Disconnect")
                }
            }

            if (error != null) {
                Text(
                    error ?: "",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.error,
                    modifier = Modifier.padding(top = 8.dp)
                )
            }
        }
    }
}
