package com.aistudio.deproof.sdwk

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.aistudio.deproof.sdwk.chain.RpcClient
import com.aistudio.deproof.sdwk.chain.SignatureInfo
import com.aistudio.deproof.sdwk.domain.AppDatabase
import com.aistudio.deproof.sdwk.domain.ReceiptEntity
import com.aistudio.deproof.sdwk.domain.ReceiptExporter
import com.aistudio.deproof.sdwk.ui.screens.NowScreen
import com.aistudio.deproof.sdwk.ui.screens.ReceiptsScreen
import com.aistudio.deproof.sdwk.ui.screens.ReviewScreen
import com.aistudio.deproof.sdwk.ui.theme.DeproofTheme
import com.aistudio.deproof.sdwk.wallet.WalletManager
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {
    private lateinit var walletManager: WalletManager
    private lateinit var database: AppDatabase
    private lateinit var rpcClient: RpcClient

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        walletManager = WalletManager(this)
        database = AppDatabase.getInstance(this)
        rpcClient = RpcClient("https://api.mainnet-beta.solana.com")

        setContent {
            DeproofTheme {
                AppContent(
                    walletManager = walletManager,
                    database = database,
                    rpcClient = rpcClient
                )
            }
        }
    }
}

@Composable
fun AppContent(
    walletManager: WalletManager,
    database: AppDatabase,
    rpcClient: RpcClient
) {
    val currentScreen = remember { mutableStateOf("now") }
    val selectedSignature = remember { mutableStateOf<SignatureInfo?>(null) }
    val selectedTx = remember { mutableStateOf<com.aistudio.deproof.sdwk.chain.TransactionResponse?>(null) }
    val walletConnected = remember { mutableStateOf(false) }
    val walletAddress = remember { mutableStateOf("") }
    val receipts = remember { mutableStateListOf<ReceiptEntity>() }

    LaunchedEffect(Unit) {
        try {
            val allReceipts = database.receiptDao().getAll()
            receipts.clear()
            receipts.addAll(allReceipts)
        } catch (e: Exception) {
            // Ignore DB load errors
        }
    }

    Scaffold(
        topBar = {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(8.dp),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text("Deproof")
                if (walletConnected.value) {
                    Text("✓ Connected", modifier = Modifier.padding(end = 8.dp))
                }
            }
        },
        bottomBar = {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(8.dp),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                Button(onClick = { currentScreen.value = "now" }) {
                    Text("Now")
                }
                Button(onClick = { currentScreen.value = "receipts" }) {
                    Text("Receipts")
                }
                Button(onClick = {
                    // Wallet action
                    if (walletConnected.value) {
                        walletConnected.value = false
                        walletAddress.value = ""
                    }
                }) {
                    Text(if (walletConnected.value) "Disconnect" else "Connect")
                }
            }
        }
    ) { padding ->
        Column(modifier = Modifier.padding(padding)) {
            when (currentScreen.value) {
                "now" -> NowScreen(
                    onSignatureTap = { sig ->
                        selectedSignature.value = sig
                        currentScreen.value = "review"
                        // Load transaction in background
                    },
                    onWalletConnect = {
                        walletAddress.value
                    }
                )
                "review" -> {
                    if (selectedTx.value != null) {
                        ReviewScreen(
                            tx = selectedTx.value!!,
                            onApprove = {
                                // Handle approval
                                val receipt = ReceiptExporter.createReceipt(
                                    signature = "signed_sig",
                                    verdict = "approved",
                                    summary = "Transaction approved"
                                )
                                database.receiptDao().insert(receipt)
                                receipts.add(receipt)
                                currentScreen.value = "receipts"
                            },
                            onReject = {
                                // Handle rejection
                                val receipt = ReceiptExporter.createReceipt(
                                    signature = null,
                                    verdict = "rejected",
                                    summary = "Transaction rejected"
                                )
                                database.receiptDao().insert(receipt)
                                receipts.add(receipt)
                                currentScreen.value = "receipts"
                            },
                            walletConnected = walletConnected.value
                        )
                    }
                }
                "receipts" -> ReceiptsScreen(receipts = receipts)
            }
        }
    }
}
