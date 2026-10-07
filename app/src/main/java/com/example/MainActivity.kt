package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import com.example.chain.Verdict
import com.example.data.AppDatabase
import com.example.domain.WalletSession
import com.example.ui.theme.DeproofTheme
import com.example.ui.screens.NowScreen
import com.example.ui.screens.ReviewScreen
import com.example.ui.screens.ReceiptsScreen

class MainActivity : ComponentActivity() {
    private lateinit var database: AppDatabase

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        database = AppDatabase.getInstance(this)

        setContent {
            DeproofTheme {
                val selectedTab = remember { mutableStateOf(0) }
                val reviewVerdict = remember { mutableStateOf<Verdict?>(null) }
                val reviewProgramId = remember { mutableStateOf("") }
                val reviewAccounts = remember { mutableStateOf<List<String>>(emptyList()) }
                val reviewData = remember { mutableStateOf("") }
                val walletSession = remember { mutableStateOf<WalletSession?>(null) }

                Scaffold(
                    bottomBar = {
                        TabRow(selectedTabIndex = selectedTab.value) {
                            Tab(
                                selected = selectedTab.value == 0,
                                onClick = { selectedTab.value = 0 },
                                text = { Text("Now") }
                            )
                            Tab(
                                selected = selectedTab.value == 1,
                                onClick = { selectedTab.value = 1 },
                                text = { Text("Review") }
                            )
                            Tab(
                                selected = selectedTab.value == 2,
                                onClick = { selectedTab.value = 2 },
                                text = { Text("Receipts") }
                            )
                        }
                    }
                ) { padding ->
                    Box(modifier = Modifier.fillMaxSize().padding(padding)) {
                        when (selectedTab.value) {
                            0 -> NowScreen(
                                onSignatureTap = { selectedTab.value = 1 },
                                database = database,
                                onWalletChanged = { walletSession.value = it },
                                walletSession = walletSession.value
                            )
                            1 -> ReviewScreen(
                                verdict = reviewVerdict.value,
                                programId = reviewProgramId.value,
                                accounts = reviewAccounts.value,
                                data = reviewData.value,
                                onReject = { selectedTab.value = 2 },
                                onApprove = { selectedTab.value = 2 },
                                database = database,
                                walletSession = walletSession.value
                            )
                            2 -> ReceiptsScreen(database = database)
                        }
                    }
                }
            }
        }
    }
}
