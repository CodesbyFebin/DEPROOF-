package com.deproof.ui.observations

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.deproof.data.observations.AIZObservation

/**
 * Device Statistics Screen - Phase 2A
 * Displays AIOZ observations during proof generation
 * Shows: storage objects, storage size, upstream speed, assurance level
 */
@Composable
fun DeviceStatsScreen(
    observation: AIZObservation,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.surface)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // Header
        Text(
            text = "Device Observations",
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(bottom = 8.dp)
        )

        Text(
            text = "Telemetry from ${observation.provider}",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        Divider(modifier = Modifier.padding(vertical = 8.dp))

        // Storage Object Count
        StatItem(
            label = "Storage Objects",
            value = formatNumber(observation.metrics.storageObjectCount),
            unit = "items",
            icon = "📦"
        )

        // Storage Size
        StatItem(
            label = "Storage Size",
            value = formatBytes(observation.metrics.storageSizeBytes),
            unit = "",
            icon = "💾"
        )

        // Upstream Speed
        StatItem(
            label = "Upstream Speed",
            value = observation.metrics.upstreamSpeedRaw.toString(),
            unit = observation.speedUnit + " (unverified)",
            icon = "📡"
        )

        Divider(modifier = Modifier.padding(vertical = 8.dp))

        // Assurance Level
        AssuranceCard(observation)

        // Source Digest
        DigestCard(observation)

        // Reward Separation
        RewardSeparationCard(observation)
    }
}

/**
 * Individual statistic display
 */
@Composable
private fun StatItem(
    label: String,
    value: String,
    unit: String,
    icon: String,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .background(
                MaterialTheme.colorScheme.surfaceVariant,
                shape = RoundedCornerShape(8.dp)
            )
            .padding(12.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = label,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Row(
                modifier = Modifier.padding(top = 4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = value,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                if (unit.isNotEmpty()) {
                    Text(
                        text = unit,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(start = 4.dp)
                    )
                }
            }
        }

        Text(
            text = icon,
            fontSize = 24.sp,
            modifier = Modifier.padding(start = 8.dp)
        )
    }
}

/**
 * Assurance level card
 */
@Composable
private fun AssuranceCard(observation: AIZObservation) {
    Surface(
        modifier = Modifier
            .fillMaxWidth(),
        shape = RoundedCornerShape(8.dp),
        color = MaterialTheme.colorScheme.primaryContainer
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Text(
                text = "Assurance Level",
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onPrimaryContainer
            )
            Text(
                text = observation.assurance,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onPrimaryContainer,
                modifier = Modifier.padding(top = 4.dp)
            )
            Text(
                text = "Device-reported, not yet verified on-chain",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onPrimaryContainer,
                modifier = Modifier.padding(top = 4.dp)
            )
        }
    }
}

/**
 * Source digest card (audit trail)
 */
@Composable
private fun DigestCard(observation: AIZObservation) {
    Surface(
        modifier = Modifier
            .fillMaxWidth(),
        shape = RoundedCornerShape(8.dp),
        color = MaterialTheme.colorScheme.secondaryContainer
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Text(
                text = "Source Digest (SHA256)",
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSecondaryContainer
            )
            Text(
                text = observation.sourceSha256.substring(0, 32) + "...",
                style = MaterialTheme.typography.bodySmall,
                fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace,
                color = MaterialTheme.colorScheme.onSecondaryContainer,
                modifier = Modifier.padding(top = 4.dp)
            )
            Text(
                text = "Proves exact CLI stats parsed",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSecondaryContainer,
                modifier = Modifier.padding(top = 4.dp)
            )
        }
    }
}

/**
 * Reward separation card
 */
@Composable
private fun RewardSeparationCard(observation: AIZObservation) {
    Surface(
        modifier = Modifier
            .fillMaxWidth(),
        shape = RoundedCornerShape(8.dp),
        color = MaterialTheme.colorScheme.tertiaryContainer
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Text(
                text = "Reward Assets",
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onTertiaryContainer
            )

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "AIOZ Rewards",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onTertiaryContainer
                    )
                    Text(
                        text = observation.rewardAsset,
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onTertiaryContainer,
                        modifier = Modifier.padding(top = 2.dp)
                    )
                }

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "SKR Payment",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onTertiaryContainer
                    )
                    Text(
                        text = observation.skrPayment,
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onTertiaryContainer,
                        modifier = Modifier.padding(top = 2.dp)
                    )
                }
            }
        }
    }
}

/**
 * Format number with commas
 */
private fun formatNumber(num: Long): String {
    return String.format("%,d", num)
}

/**
 * Format bytes as human-readable (B, KB, MB, GB)
 */
private fun formatBytes(bytes: Long): String {
    return when {
        bytes < 1024 -> "$bytes B"
        bytes < 1024 * 1024 -> String.format("%.1f KB", bytes.toDouble() / 1024)
        bytes < 1024 * 1024 * 1024 -> String.format("%.1f MB", bytes.toDouble() / (1024 * 1024))
        else -> String.format("%.1f GB", bytes.toDouble() / (1024 * 1024 * 1024))
    }
}

/**
 * Preview - shows sample observation data
 */
@Composable
fun DeviceStatsScreenPreview() {
    val sampleObservation = AIZObservation(
        schema = "deproof-aioz-observation-v1",
        provider = "AIOZ",
        assurance = "LOCAL_OBSERVATION",
        source = "operator-supplied-cli-stats",
        sourceSha256 = "e908a7da76643f008927ce7e28170d05bd25e6f94f33cad8fed853d560a6d6ea",
        metrics = com.deproof.data.observations.Metrics(
            storageObjectCount = 12345,
            storageSizeBytes = 5368709120,  // 5 GB
            upstreamSpeedRaw = 125
        ),
        speedUnit = "UNVERIFIED",
        signature = null,
        providerAcknowledgement = null,
        independentVerification = "NOT_RUN",
        rewardAsset = "AIOZ",
        skrPayment = "NOT_SUBMITTED"
    )

    MaterialTheme {
        Surface {
            DeviceStatsScreen(sampleObservation)
        }
    }
}
