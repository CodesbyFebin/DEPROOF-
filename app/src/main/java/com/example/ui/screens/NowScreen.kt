package com.example.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.chain.SignatureInfo
import com.example.data.AppDatabase
import com.example.domain.WalletSession
import com.example.util.formatSkr
import com.example.util.formatSol
import com.example.util.isPubkey
import com.example.util.shortKey
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun NowScreen(
    onSignatureTap: (SignatureInfo) -> Unit = {},
    database: AppDatabase? = null,
    onWalletChanged: (WalletSession?) -> Unit = {},
    walletSession: WalletSession? = null
) {
    var address by remember { mutableStateOf("") }
    var solBalance by remember { mutableStateOf(0L) }
    var skrBalance by remember { mutableStateOf(0L) }
    var slot by remember { mutableStateOf(0L) }
    var signatures by remember { mutableStateOf<List<SignatureInfo>>(emptyList()) }
    var isLoading by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }
    var showPlaceholder by remember { mutableStateOf(false) }

    val scope = rememberCoroutineScope()

    fun loadAccount() {
        if (!isPubkey(address)) {
            error = "Invalid address"
            return
        }

        scope.launch {
            isLoading = true
            error = null
            showPlaceholder = true
            try {
                isLoading = false
            } catch (e: Exception) {
                error = "Error: ${e.message}"
            } finally {
                isLoading = false
            }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        Text("Now", style = MaterialTheme.typography.headlineMedium)
        Spacer(modifier = Modifier.height(16.dp))

        WalletSection(
            onWalletConnected = onWalletChanged,
            currentSession = walletSession
        )
        Spacer(modifier = Modifier.height(8.dp))

        // Address Input
        TextField(
            value = address,
            onValueChange = { address = it },
            label = { Text("Paste mainnet address") },
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 12.dp),
            singleLine = true,
            isError = error != null && address.isNotEmpty(),
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
            keyboardActions = KeyboardActions(onDone = { loadAccount() })
        )

        Button(
            onClick = { loadAccount() },
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 16.dp),
            enabled = !isLoading && isPubkey(address)
        ) {
            Text(if (isLoading) "Loading..." else "Load Account")
        }

        if (error != null) {
            Text(
                text = error ?: "",
                color = Color(0xFFFF6B6B),
                style = MaterialTheme.typography.bodySmall,
                modifier = Modifier.padding(bottom = 12.dp)
            )
        }

        // Account Summary
        if (showPlaceholder) {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 16.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("Address: ${shortKey(address)}", style = MaterialTheme.typography.bodySmall, fontFamily = FontFamily.Monospace)
                    Spacer(modifier = Modifier.height(12.dp))
                    Text("RPC integration coming soon", style = MaterialTheme.typography.bodyMedium, color = Color.Gray)
                    Text("Balances and signatures will appear here once RPC is configured", style = MaterialTheme.typography.bodySmall, color = Color.Gray)
                }
            }
        }

        // Signatures
        if (signatures.isNotEmpty()) {
            Text("Last 10 Signatures", style = MaterialTheme.typography.titleSmall)
            LazyColumn {
                items(signatures) { sig ->
                    SignatureCard(sig = sig, onClick = { onSignatureTap(sig) })
                }
            }
        } else if (!isLoading && address.isNotEmpty()) {
            Text(
                "No signatures",
                style = MaterialTheme.typography.bodySmall,
                color = Color.Gray
            )
        }
    }
}

@Composable
fun SignatureCard(sig: SignatureInfo, onClick: () -> Unit) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(8.dp)
            .clickable { onClick() }
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Text(
                shortKey(sig.signature),
                fontFamily = FontFamily.Monospace,
                fontSize = 10.sp
            )
            Spacer(modifier = Modifier.height(4.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    "Slot: ${sig.slot}",
                    style = MaterialTheme.typography.bodySmall
                )
                val status = when {
                    sig.err != null -> "Failed"
                    sig.blockTime == null -> "Processed"
                    else -> "Confirmed"
                }
                Text(
                    status,
                    style = MaterialTheme.typography.bodySmall,
                    color = when (status) {
                        "Failed" -> Color(0xFFFF6B6B)
                        "Confirmed" -> Color(0xFF51CF66)
                        else -> Color.Gray
                    }
                )
            }
            if (sig.blockTime != null) {
                Text(
                    SimpleDateFormat("MM/dd HH:mm", Locale.US).format(Date(sig.blockTime * 1000)),
                    style = MaterialTheme.typography.bodySmall,
                    color = Color.Gray
                )
            }
        }
    }
}
