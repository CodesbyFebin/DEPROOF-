package com.deproof.presentation.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/**
 * Represents the revocation state of a session/operation.
 */
sealed class RevocationState {
    object None : RevocationState()
    data class Revoked(val reason: String, val timestamp: Long) : RevocationState()
}

/**
 * Displays revocation information in the review screen.
 */
@Composable
fun RevocationBanner(revocationState: RevocationState, modifier: Modifier = Modifier) {
    when (revocationState) {
        RevocationState.None -> {
            // No revocation, show nothing
        }
        is RevocationState.Revoked -> {
            RevocationWarningCard(
                reason = revocationState.reason,
                timestamp = revocationState.timestamp,
                modifier = modifier
            )
        }
    }
}

/**
 * Displays a warning card when session/operation is revoked.
 */
@Composable
private fun RevocationWarningCard(
    reason: String,
    timestamp: Long,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .padding(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = Color(0xFFFFEBEE) // Light red background
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            // Header with warning icon
            androidx.compose.foundation.layout.Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Default.Warning,
                    contentDescription = "Session Revoked",
                    tint = Color(0xFFC62828), // Dark red
                    modifier = Modifier.padding(end = 12.dp)
                )
                Text(
                    text = "Session Revoked",
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp,
                    color = Color(0xFFC62828),
                    modifier = Modifier.weight(1f)
                )
            }

            // Reason and details
            Text(
                text = "Reason: ${formatRevocationReason(reason)}",
                fontSize = 14.sp,
                color = Color(0xD62828),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 8.dp)
            )

            Text(
                text = "Revoked at: ${formatTimestamp(timestamp)}",
                fontSize = 12.sp,
                color = Color(0x99000000),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 4.dp)
            )

            // Action message
            Text(
                text = "No further operations can be performed under this session. Please restart or establish a new connection.",
                fontSize = 12.sp,
                color = Color(0x99000000),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 12.dp),
                fontStyle = androidx.compose.ui.text.font.FontStyle.Italic
            )
        }
    }
}

/**
 * Formats revocation reason for display.
 */
private fun formatRevocationReason(reason: String): String {
    return when (reason.uppercase()) {
        "EXPLICIT" -> "Explicitly revoked by user or administrator"
        "COMPROMISED" -> "Session marked as compromised"
        "SCOPE_VIOLATION" -> "Scope violation detected"
        "POLICY_CHANGE" -> "Policy has changed, session no longer valid"
        "TOKEN_EXPIRED" -> "Authorization token expired"
        else -> reason
    }
}

/**
 * Formats timestamp for display.
 */
private fun formatTimestamp(timestampMs: Long): String {
    val sdf = java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss", java.util.Locale.US)
    sdf.timeZone = java.util.TimeZone.getTimeZone("UTC")
    return sdf.format(java.util.Date(timestampMs))
}

/**
 * Composable that handles revocation state and shows appropriate UI.
 */
@Composable
fun ReviewScreenWithRevocationHandling(
    sessionId: String,
    revocationState: RevocationState,
    onRevocationDetected: (String) -> Unit,
    content: @Composable () -> Unit
) {
    Column {
        // Show revocation banner if needed
        RevocationBanner(revocationState = revocationState)

        // Show appropriate content based on revocation state
        when (revocationState) {
            RevocationState.None -> {
                // Show normal content
                content()
            }
            is RevocationState.Revoked -> {
                // Show disabled state
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(Color(0xFFF5F5F5))
                        .padding(16.dp)
                ) {
                    Text(
                        text = "This session has been revoked and is no longer usable.",
                        modifier = Modifier.fillMaxWidth(),
                        color = Color.Gray,
                        fontSize = 14.sp
                    )
                    Text(
                        text = "Please establish a new connection to continue.",
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 8.dp),
                        color = Color.Gray,
                        fontSize = 12.sp
                    )
                }

                // Trigger callback
                onRevocationDetected(revocationState.reason)
            }
        }
    }
}

/**
 * Hook to observe revocation changes and handle state updates.
 * Should be called in LaunchedEffect or similar lifecycle handler.
 */
suspend fun observeRevocationChanges(
    revocationPoller: com.deproof.data.RevocationPoller?,
    sessionId: String,
    onRevocationDetected: (String) -> Unit
) {
    revocationPoller?.getRevocationFlow(sessionId)?.collect { revocationEntry ->
        if (revocationEntry != null) {
            val reason = revocationEntry.split("@").getOrNull(0) ?: "UNKNOWN"
            onRevocationDetected(reason)
        }
    }
}
