package com.aistudio.deproof.sdwk.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.aistudio.deproof.sdwk.chain.SignatureInfo
import com.aistudio.deproof.sdwk.chain.TransactionResponse
import com.aistudio.deproof.sdwk.chain.Verdict
import com.aistudio.deproof.sdwk.domain.InstructionDecoder
import com.aistudio.deproof.sdwk.domain.ReviewBinding
import com.aistudio.deproof.sdwk.domain.ReviewHash
import com.aistudio.deproof.sdwk.util.sha256Text
import com.aistudio.deproof.sdwk.util.shortKey

@Composable
fun ReviewScreen(
    tx: TransactionResponse,
    onApprove: suspend () -> Unit,
    onReject: suspend () -> Unit,
    walletConnected: Boolean = false
) {
    var verdict by remember { mutableStateOf<Verdict?>(null) }
    var reviewHash by remember { mutableStateOf<ReviewHash?>(null) }
    var hashMismatch by remember { mutableStateOf(false) }
    var isApproving by remember { mutableStateOf(false) }
    var isRejecting by remember { mutableStateOf(false) }

    LaunchedEffect(tx) {
        // Prepare review
        reviewHash = ReviewBinding.prepareReview(tx, "mainnet-beta")

        // Decode first instruction
        if (tx.transaction.message.instructions.isNotEmpty()) {
            val instr = tx.transaction.message.instructions[0]
            val programId =
                tx.transaction.message.accountKeys.getOrNull(instr.programIdIndex) ?: "unknown"
            val accounts = instr.accounts.mapNotNull { idx ->
                tx.transaction.message.accountKeys.getOrNull(idx)
            }

            verdict = InstructionDecoder.decode(programId, accounts, instr.data, instr)
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp)
    ) {
        // Verdict Card
        if (verdict != null) {
            val v = verdict!!
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 16.dp),
                colors = CardDefaults.cardColors(
                    containerColor = if (v.isPayable) Color(0xFFE8F5E9) else Color(0xFFFFEBEE)
                )
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        v.title,
                        style = MaterialTheme.typography.titleLarge,
                        color = if (v.isPayable) Color(0xFF2E7D32) else Color(0xFFC62828)
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(v.reason, style = MaterialTheme.typography.bodyMedium)
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        v.summary,
                        style = MaterialTheme.typography.bodySmall,
                        fontFamily = FontFamily.Monospace
                    )
                }
            }
        }

        // Message Hash
        if (reviewHash != null) {
            Card(modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 16.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("Message Hash", style = MaterialTheme.typography.titleSmall)
                    Text(
                        reviewHash!!.messageHash,
                        fontFamily = FontFamily.Monospace,
                        fontSize = 10.sp,
                        modifier = Modifier.padding(top = 8.dp)
                    )
                }
            }
        }

        // Tampering Detection
        if (hashMismatch) {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 16.dp),
                colors = CardDefaults.cardColors(containerColor = Color(0xFFFFEBEE))
            ) {
                Text(
                    "MESSAGE_CHANGED: Hash mismatch detected",
                    modifier = Modifier.padding(16.dp),
                    color = Color.Red,
                    style = MaterialTheme.typography.bodySmall
                )
            }
        }

        // Program Info
        if (tx.transaction.message.instructions.isNotEmpty()) {
            val instr = tx.transaction.message.instructions[0]
            val programId = tx.transaction.message.accountKeys.getOrNull(instr.programIdIndex)
            if (programId != null) {
                Card(modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 16.dp)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text("Program", style = MaterialTheme.typography.titleSmall)
                        Text(
                            shortKey(programId),
                            fontFamily = FontFamily.Monospace,
                            fontSize = 11.sp
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Action Buttons
        val canApprove = walletConnected && (verdict?.isPayable == true) && !hashMismatch

        Button(
            onClick = {
                isApproving = true
            },
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 8.dp),
            enabled = canApprove && !isApproving && !isRejecting,
            colors = ButtonDefaults.buttonColors(
                disabledContainerColor = Color.Gray
            )
        ) {
            Text(if (isApproving) "Signing..." else "Approve")
        }

        OutlinedButton(
            onClick = {
                isRejecting = true
            },
            modifier = Modifier.fillMaxWidth(),
            enabled = !isRejecting && !isApproving
        ) {
            Text("Reject")
        }

        if (!walletConnected && canApprove) {
            Text(
                "Connect wallet to sign",
                style = MaterialTheme.typography.bodySmall,
                color = Color.Gray,
                modifier = Modifier.padding(top = 8.dp)
            )
        }
    }
}
