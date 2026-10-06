package com.deproof.presentation

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.deproof.data.repository.RpcRepository
import com.deproof.data.repository.ReceiptRepository
import com.deproof.presentation.ui.screen.*
import com.deproof.presentation.ui.theme.DepRoofTheme
import com.deproof.presentation.viewmodel.*

class MainActivity : ComponentActivity() {

    private lateinit var rpcRepository: RpcRepository
    private lateinit var receiptRepository: ReceiptRepository

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        rpcRepository = RpcRepository()
        receiptRepository = ReceiptRepository(null)

        setContent {
            DepRoofTheme {
                DepRoofApp(rpcRepository, receiptRepository)
            }
        }
    }
}

@Composable
fun DepRoofApp(
    rpcRepository: RpcRepository,
    receiptRepository: ReceiptRepository
) {
    val navController = rememberNavController()
    var selectedTab by remember { mutableStateOf(0) }

    val nowViewModel = remember { NowViewModel(rpcRepository) }
    val reviewViewModel = remember { ReviewViewModel(rpcRepository, receiptRepository) }
    val receiptsViewModel = remember { ReceiptsViewModel(receiptRepository) }
    val tasksViewModel = remember { TasksViewModel() }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        bottomBar = {
            NavigationBar {
                NavigationBarItem(
                    icon = { Icon(Icons.Default.Home, contentDescription = "Now") },
                    label = { Text("Now") },
                    selected = selectedTab == 0,
                    onClick = {
                        selectedTab = 0
                        navController.navigate("now") {
                            popUpTo("now") { inclusive = true }
                        }
                    }
                )

                NavigationBarItem(
                    icon = { Icon(Icons.Default.Settings, contentDescription = "Tasks") },
                    label = { Text("Tasks") },
                    selected = selectedTab == 1,
                    onClick = {
                        selectedTab = 1
                        navController.navigate("tasks") {
                            popUpTo("tasks") { inclusive = true }
                        }
                    }
                )

                NavigationBarItem(
                    icon = { Icon(Icons.Default.Search, contentDescription = "Nodes") },
                    label = { Text("Nodes") },
                    selected = selectedTab == 2,
                    onClick = {
                        selectedTab = 2
                        navController.navigate("nodes") {
                            popUpTo("nodes") { inclusive = true }
                        }
                    }
                )

                NavigationBarItem(
                    icon = { Icon(Icons.Default.Home, contentDescription = "Review") },
                    label = { Text("Review") },
                    selected = selectedTab == 3,
                    onClick = {
                        selectedTab = 3
                        navController.navigate("review") {
                            popUpTo("review") { inclusive = true }
                        }
                    }
                )

                NavigationBarItem(
                    icon = { Icon(Icons.Default.Home, contentDescription = "Receipts") },
                    label = { Text("Receipts") },
                    selected = selectedTab == 4,
                    onClick = {
                        selectedTab = 4
                        navController.navigate("receipts") {
                            popUpTo("receipts") { inclusive = true }
                        }
                    }
                )
            }
        }
    ) { paddingValues ->
        NavHost(
            navController = navController,
            startDestination = "now",
            modifier = Modifier.fillMaxSize()
        ) {
            composable("now") {
                NowScreen(nowViewModel)
            }

            composable("tasks") {
                TasksScreen(tasksViewModel)
            }

            composable("nodes") {
                NodesScreen()
            }

            composable("review") {
                ReviewScreen(reviewViewModel)
            }

            composable("receipts") {
                ReceiptsScreen(receiptsViewModel)
            }
        }
    }
}
