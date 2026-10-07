package com.aistudio.deproof.sdwk.ui.screens

import android.content.ClipboardManager
import android.content.Context
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.aistudio.deproof.sdwk.domain.ReceiptEntity
import com.aistudio.deproof.sdwk.domain.ReceiptExporter
import com.aistudio.deproof.sdwk.util.shortKey
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun ReceiptsScreen(receipts: List<ReceiptEntity>) {
    var copied by remember { mutableStateOf(false) }
    var selectedReceiptId by remember { mutableStateOf<String?>(null) }
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    fun copyReceipt(receipt: ReceiptEntity) {
        val json = ReceiptExporter.exportReceipt(receipt)
        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as? ClipboardManager
        if (clipboard != null) {
            val clip = android.content.ClipData.newPlainText("receipt", json)
            clipboard.setPrimaryClip(clip)
            copied = true
            selectedReceiptId = receipt.id
            scope.launch {
                kotlinx.coroutines.delay(2000)
                copied = false
            }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        Text("Receipts", style = MaterialTheme.typography.headlineMedium)
        Spacer(modifier = Modifier.height(16.dp))

        if (receipts.isEmpty()) {
            Text(
                "No receipts yet",
                style = MaterialTheme.typography.bodyMedium,
                color = Color.Gray,
                modifier = Modifier.align(Alignment.CenterHorizontally)
            )
        } else {
            LazyColumn {
                items(receipts) { receipt ->
                    ReceiptCard(
                        receipt = receipt,
                        onCopy = { copyReceipt(receipt) },
                        isCopied = copied && selectedReceiptId == receipt.id
                    )
                }
            }
        }
    }
}

@Composable
fun ReceiptCard(
    receipt: ReceiptEntity,
    onCopy: () -> Unit,
    isCopied: Boolean
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = 8.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            // Status Badge
            val statusColor = when (receipt.status) {
                "recorded" -> Color(0xFF2E7D32)
                "signed" -> Color(0xFF1976D2)
                "rejected" -> Color(0xFFC62828)
                else -> Color.Gray
            }

            Box(
                modifier = Modifier
                    .background(statusColor.copy(alpha = 0.2f), shape = androidx.compose.foundation.shape.RoundedCornerShape(4.dp))
                    .padding(horizontal = 8.dp, vertical = 4.dp)
            ) {
                Text(
                    receipt.status.uppercase(),
                    fontSize = 10.sp,
                    color = statusColor
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Summary
            Text(
                receipt.summary,
                style = MaterialTheme.typography.bodySmall,
                fontFamily = FontFamily.Monospace
            )

            Spacer(modifier = Modifier.height(8.dp))

            // Signature (if present)
            if (receipt.signature != null) {
                Text(
                    "Sig: ${shortKey(receipt.signature)}",
                    style = MaterialTheme.typography.bodySmall,
                    color = Color.Gray,
                    fontFamily = FontFamily.Monospace
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Timestamp
            val timeStr = SimpleDateFormat("MM/dd HH:mm:ss", Locale.US).format(Date(receipt.timestamp))
            Text(
                timeStr,
                style = MaterialTheme.typography.bodySmall,
                color = Color.Gray
            )

            Spacer(modifier = Modifier.height(12.dp))

            // Copy Button
            Button(
                onClick = onCopy },
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (isCopied) Color(0xFF2E7D32) else MaterialTheme.colorScheme.primary
                )
            ) {
                Text(if (isCopied) "Copied!" else "Copy JSON")
            }
        }
    }
}
