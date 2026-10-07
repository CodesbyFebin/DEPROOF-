package com.aistudio.deproof.sdwk.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.aistudio.deproof.sdwk.chain.AccountSummary
import com.aistudio.deproof.sdwk.chain.RpcClient
import com.aistudio.deproof.sdwk.chain.SignatureInfo
import com.aistudio.deproof.sdwk.util.formatSkr
import com.aistudio.deproof.sdwk.util.formatSol
import com.aistudio.deproof.sdwk.util.isPubkey
import com.aistudio.deproof.sdwk.util.shortKey
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun NowScreen(
    onSignatureTap: (SignatureInfo) -> Unit,
    onWalletConnect: suspend () -> String
) {
    var address by remember { mutableStateOf("") }
    var accountData by remember { mutableStateOf<AccountSummary?>(null) }
    var signatures by remember { mutableStateOf<List<SignatureInfo>>(emptyList()) }
    var isLoading by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }
    var isRefreshing by remember { mutableStateOf(false) }

    val rpcClient = remember { RpcClient("https://api.mainnet-beta.solana.com") }
    val scope = rememberCoroutineScope()

    fun loadAccount() {
        if (!isPubkey(address)) {
            error = "Invalid address"
            return
        }

        scope.launch {
            isLoading = true
            error = null
            try {
                accountData = rpcClient.getAccountInfo(address)
                signatures = rpcClient.getSignatures(address, 10)
                if (accountData == null) {
                    error = "Failed to load account"
                }
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
        // Address Input
        TextField(
            value = address,
            onValueChange = { address = it },
            label = { Text("Mainnet Address") },
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 12.dp),
            singleLine = true,
            isError = error != null
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
                color = Color.Red,
                modifier = Modifier.padding(bottom = 12.dp)
            )
        }

        // Balance Display
        if (accountData != null) {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 16.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("Balances", style = MaterialTheme.typography.titleMedium)
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(formatSol(accountData!!.solBalance), fontFamily = FontFamily.Monospace)
                    Text(formatSkr(accountData!!.skrBalance), fontFamily = FontFamily.Monospace)
                    Spacer(modifier = Modifier.height(8.dp))
                    Text("Slot: ${accountData!!.slot}", style = MaterialTheme.typography.bodySmall)
                }
            }
        }

        // Signatures List
        if (signatures.isNotEmpty()) {
            Text("Last 10 Signatures", style = MaterialTheme.typography.titleMedium)
            LazyColumn {
                items(signatures) { sig ->
                    SignatureCard(sig = sig, onClick = { onSignatureTap(sig) })
                }
            }
        } else if (accountData != null && !isLoading) {
            Text(
                "No signatures found",
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
                fontSize = 11.sp
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
                        "Failed" -> Color.Red
                        "Confirmed" -> Color.Green
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
