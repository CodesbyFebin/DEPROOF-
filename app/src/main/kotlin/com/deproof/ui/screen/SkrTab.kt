// SkrTab.kt — Jetpack Compose UI component for displaying SKR token balance and staking options.
//
// Integrates with NowScreen to show:
// - Current SKR balance with proper decimal formatting
// - Staking pool options
// - Connection to ReviewScreen for stake/unstake actions
package com.deproof.ui.screen

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.RefreshCw
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.deproof.data.SeekerTokenReader
import com.deproof.domain.SkrError
import com.deproof.domain.TokenAmount
import kotlinx.coroutines.launch

/**
 * View model state for the SKR tab.
 */
data class SkrTabState(
    val balance: String = "0",
    val isLoading: Boolean = false,
    val error: SkrError? = null,
    val walletAddress: String? = null
) {
    val displayBalance: String
        get() = when {
            error is SkrError.ConfigNotFound -> "N/A"
            error != null -> "Error"
            else -> balance
        }

    val isAvailable: Boolean
        get() = error !is SkrError.ConfigNotFound

    val errorMessage: String?
        get() = when (error) {
            is SkrError.ConfigNotFound -> "SKR staking configuration not available"
            is SkrError.RpcFailure -> "Network error: ${error.message}"
            is SkrError.InvalidAmount -> "Invalid amount"
            else -> error?.message
        }
}

/**
 * Sealed hierarchy of UI events for the SKR tab.
 */
sealed class SkrTabEvent {
    object RefreshBalance : SkrTabEvent()
    data class StakeRequested(val amount: String) : SkrTabEvent()
    data class UnstakeRequested(val amount: String) : SkrTabEvent()
}

/**
 * Composable for displaying SKR token balance and staking options.
 *
 * Usage:
 * ```
 * val reader = SeekerTokenReader(rpcClient)
 * SkrTab(
 *     reader = reader,
 *     walletAddress = "9B5X...",
 *     onEvent = { event ->
 *         when (event) {
 *             is SkrTabEvent.StakeRequested -> navigateToStake(event.amount)
 *             is SkrTabEvent.UnstakeRequested -> navigateToUnstake(event.amount)
 *             is SkrTabEvent.RefreshBalance -> refreshBalance()
 *         }
 *     }
 * )
 * ```
 */
@Composable
fun SkrTab(
    reader: SeekerTokenReader,
    walletAddress: String? = null,
    onEvent: (SkrTabEvent) -> Unit = {}
) {
    var state by remember { mutableStateOf(SkrTabState(walletAddress = walletAddress)) }
    val coroutineScope = rememberCoroutineScope()

    LaunchedEffect(walletAddress) {
        if (walletAddress != null && !state.isLoading) {
            refreshBalance(reader, walletAddress) { newState -> state = newState }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // ─── Balance Card ─────────────────────────────────────────────────
        BalanceCard(
            state = state,
            onRefresh = {
                if (walletAddress != null) {
                    coroutineScope.launch {
                        state = state.copy(isLoading = true)
                        refreshBalance(reader, walletAddress) { newState -> state = newState }
                        onEvent(SkrTabEvent.RefreshBalance)
                    }
                }
            }
        )

        // ─── Error Display ────────────────────────────────────────────────
        if (state.error != null && state.error !is SkrError.ConfigNotFound) {
            ErrorBanner(state.error!!)
        }

        // ─── Staking Section (only if available) ──────────────────────────
        if (state.isAvailable) {
            StakingSection(
                balance = state.balance,
                onStake = { amount ->
                    onEvent(SkrTabEvent.StakeRequested(amount))
                },
                onUnstake = { amount ->
                    onEvent(SkrTabEvent.UnstakeRequested(amount))
                }
            )
        } else {
            UnavailableMessage()
        }
    }
}

@Composable
private fun BalanceCard(
    state: SkrTabState,
    onRefresh: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(8.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Header with title and refresh button
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    "Seeker Token (SKR)",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Medium,
                    color = MaterialTheme.colorScheme.onSurface
                )
                IconButton(
                    onClick = onRefresh,
                    enabled = !state.isLoading,
                    modifier = Modifier.size(24.dp)
                ) {
                    Icon(
                        Icons.Filled.RefreshCw,
                        contentDescription = "Refresh balance",
                        tint = if (state.isLoading) {
                            MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                        } else {
                            MaterialTheme.colorScheme.primary
                        }
                    )
                }
            }

            // Balance display
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(
                        color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.1f),
                        shape = RoundedCornerShape(4.dp)
                    )
                    .padding(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        "Balance",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                    )
                    Text(
                        state.displayBalance,
                        fontSize = 24.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace,
                        color = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.padding(top = 4.dp)
                    )
                }

                if (state.isLoading) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(24.dp),
                        strokeWidth = 2.dp
                    )
                }
            }

            // Status text
            if (state.walletAddress != null) {
                Text(
                    "Address: ${state.walletAddress.take(8)}...${state.walletAddress.takeLast(8)}",
                    fontSize = 10.sp,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                    fontFamily = FontFamily.Monospace
                )
            }
        }
    }
}

@Composable
private fun StakingSection(
    balance: String,
    onStake: (String) -> Unit,
    onUnstake: (String) -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(8.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                "Staking Options",
                fontSize = 14.sp,
                fontWeight = FontWeight.Medium,
                color = MaterialTheme.colorScheme.onSurface
            )

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                StakingPoolButton("0.1 SKR", { onStake("0.1") })
                StakingPoolButton("0.5 SKR", { onStake("0.5") })
                StakingPoolButton("1 SKR", { onStake("1") })
            }

            if (balance != "0") {
                Button(
                    onClick = { onUnstake(balance) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(40.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.error
                    )
                ) {
                    Text("Unstake All ($balance SKR)", fontSize = 12.sp)
                }
            }
        }
    }
}

@Composable
private fun StakingPoolButton(
    label: String,
    onClick: () -> Unit
) {
    OutlinedButton(
        onClick = onClick,
        modifier = Modifier.height(40.dp)
    ) {
        Text(label, fontSize = 11.sp)
    }
}

@Composable
private fun ErrorBanner(error: SkrError) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .background(
                color = MaterialTheme.colorScheme.errorContainer,
                shape = RoundedCornerShape(8.dp)
            ),
        shape = RoundedCornerShape(8.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.errorContainer
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.Top
        ) {
            Icon(
                Icons.Filled.Warning,
                contentDescription = "Error",
                tint = MaterialTheme.colorScheme.onErrorContainer,
                modifier = Modifier.size(20.dp)
            )
            Text(
                error.message ?: "An error occurred",
                fontSize = 12.sp,
                color = MaterialTheme.colorScheme.onErrorContainer,
                modifier = Modifier.weight(1f)
            )
        }
    }
}

@Composable
private fun UnavailableMessage() {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(8.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Icon(
                Icons.Filled.Warning,
                contentDescription = "Unavailable",
                tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f),
                modifier = Modifier.size(32.dp)
            )
            Text(
                "SKR Staking Unavailable",
                fontSize = 12.sp,
                fontWeight = FontWeight.Medium,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
            )
            Text(
                "The Seeker Token staking feature is not currently available.",
                fontSize = 11.sp,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
            )
        }
    }
}

/**
 * Helper function to refresh balance from the RPC.
 */
private suspend fun refreshBalance(
    reader: SeekerTokenReader,
    walletAddress: String,
    updateState: (SkrTabState) -> Unit
) {
    val result = reader.getBalanceAsDecimal(walletAddress)

    result.onSuccess { balance ->
        updateState(SkrTabState(
            balance = balance,
            isLoading = false,
            walletAddress = walletAddress
        ))
    }

    result.onFailure { error ->
        val skrError = if (error is SkrError) error else {
            SkrError.RpcFailure(originalError = error as? Exception)
        }
        updateState(SkrTabState(
            balance = "0",
            isLoading = false,
            error = skrError,
            walletAddress = walletAddress
        ))
    }
}
