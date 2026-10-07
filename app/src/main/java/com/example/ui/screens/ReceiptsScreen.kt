package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.unit.dp
import com.example.data.AppDatabase
import com.example.data.Receipt
import com.google.gson.Gson
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun ReceiptsScreen(database: AppDatabase? = null) {
    val receipts = remember { mutableStateOf<List<Receipt>>(emptyList()) }
    val selectedReceipt = remember { mutableStateOf<Receipt?>(null) }
    val scope = rememberCoroutineScope()

    LaunchedEffect(database) {
        database?.let {
            scope.launch(Dispatchers.IO) {
                receipts.value = it.receiptDao().getAllReceipts()
            }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        Text(
            "Receipts",
            style = MaterialTheme.typography.headlineMedium,
            modifier = Modifier.padding(bottom = 16.dp)
        )

        if (selectedReceipt.value == null) {
            if (receipts.value.isEmpty()) {
                Text("No receipts yet", style = MaterialTheme.typography.bodyMedium)
            } else {
                LazyColumn {
                    items(receipts.value) { receipt ->
                        ReceiptListItem(receipt) {
                            selectedReceipt.value = receipt
                        }
                    }
                }
            }
        } else {
            ReceiptDetailView(selectedReceipt.value!!) {
                selectedReceipt.value = null
            }
        }
    }
}

@Composable
fun ReceiptListItem(receipt: Receipt, onClick: () -> Unit) {
    val dateFormat = SimpleDateFormat("MM/dd HH:mm", Locale.US)
    val timestamp = dateFormat.format(Date(receipt.timestamp))
    val icon = if (receipt.signature != null) Icons.Default.Check else Icons.Default.Close
    val tint = if (receipt.signature != null) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .padding(12.dp)
            .background(MaterialTheme.colorScheme.surfaceVariant, MaterialTheme.shapes.small)
            .padding(12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(receipt.summary, style = MaterialTheme.typography.bodyMedium)
            Text(timestamp, style = MaterialTheme.typography.labelSmall)
        }
        Icon(icon, null, tint = tint, modifier = Modifier.size(24.dp))
    }
}

@Composable
fun ReceiptDetailView(receipt: Receipt, onBack: () -> Unit) {
    val clipboardManager = LocalClipboardManager.current
    val dateFormat = SimpleDateFormat("MM/dd HH:mm:ss", Locale.US)
    val timestamp = dateFormat.format(Date(receipt.timestamp))
    val gson = Gson()

    Column(modifier = Modifier.fillMaxSize()) {
        Button(onClick = onBack, modifier = Modifier.padding(bottom = 16.dp)) {
            Text("Back")
        }

        Text("Outcome: ${receipt.outcome}", style = MaterialTheme.typography.bodyMedium)
        Text("Summary: ${receipt.summary}", style = MaterialTheme.typography.bodyMedium)
        Text("Hash: ${receipt.messageHash.take(12)}...", style = MaterialTheme.typography.bodySmall)
        if (receipt.signature != null) {
            Text("Signature: ${receipt.signature.take(12)}...", style = MaterialTheme.typography.bodySmall)
        }
        Text("Slot: ${receipt.slot ?: "N/A"}", style = MaterialTheme.typography.bodySmall)
        Text("Broadcast: ${receipt.broadcast}", style = MaterialTheme.typography.bodySmall)
        Text("Time: $timestamp", style = MaterialTheme.typography.bodySmall)

        Button(
            onClick = {
                val json = gson.toJson(receipt)
                clipboardManager.setText(AnnotatedString(json))
            },
            modifier = Modifier.padding(top = 16.dp)
        ) {
            Text("Copy JSON")
        }
    }
}
