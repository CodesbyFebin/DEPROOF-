package com.deproof.ui.screen

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.deproof.data.observations.*
import java.time.Instant
import java.time.format.DateTimeFormatter

/**
 * Unified device stats screen displaying any Observation type
 * Supports: AIOZ observations, Flux node observations, future providers
 * Uses type-safe when patterns for provider-specific rendering
 */
@Composable
fun DeviceStatsScreen(
    observation: Observation,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier.fillMaxSize(),
        color = Color(0xFF0A0E27)  // obsidian
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Header with provider name
            ProviderHeader(observation)

            // Observation metadata
            MetadataSection(observation)

            // Divider
            Divider(color = Color(0xFF1A1F3A), thickness = 1.dp)

            // Provider-specific metrics
            when (observation) {
                is AIZObservation -> AIZMetricsSection(observation)
                is FluxObservation -> FluxMetricsSection(observation)
                else -> UnknownObservationSection(observation)
            }

            Spacer(modifier = Modifier.weight(1f))

            // Evidence & audit trail
            AuditTrailSection(observation)
        }
    }
}

@Composable
private fun ProviderHeader(observation: Observation) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color(0xFF1A1F3A), shape = RoundedCornerShape(8.dp))
            .padding(16.dp)
    ) {
        Text(
            text = "${observation.provider} Observation",
            fontSize = 24.sp,
            fontWeight = FontWeight.Bold,
            color = Color(0xFF00FF9F)  // mint
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = "Schema: ${observation.schema}",
            fontSize = 12.sp,
            color = Color(0xFFB0B8D4),
            fontFamily = FontFamily.Monospace
        )
    }
}

@Composable
private fun MetadataSection(observation: Observation) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color(0xFF1A1F3A), shape = RoundedCornerShape(8.dp))
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // Timestamp
        val iso = DateTimeFormatter.ISO_INSTANT.format(
            Instant.ofEpochMilli(observation.timestamp))
        StatRow("Timestamp", iso)
        StatRow("Epoch", observation.timestamp.toString())

        // Assurance level
        StatRow("Assurance", observation.assurance)
        StatRow("Verification", observation.independentVerification)

        // Endpoint
        observation.endpoint?.let {
            StatRow("Endpoint", it)
        }

        // Asset tracking
        StatRow("Reward Asset", observation.rewardAsset)
        StatRow("Reward Status", observation.rewardStatus)
        StatRow("SKR Payment", observation.skrPaymentStatus)
    }
}

@Composable
private fun AIZMetricsSection(observation: AIZObservation) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color(0xFF1A1F3A), shape = RoundedCornerShape(8.dp))
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text(
            text = "Storage Metrics",
            fontSize = 16.sp,
            fontWeight = FontWeight.SemiBold,
            color = Color(0xFF00D9FF)  // cyan
        )

        val metrics = observation.metrics
        StatRow(
            "Storage Objects",
            metrics.storageObjectCount.toString()
        )
        StatRow(
            "Storage Size",
            formatBytes(metrics.storageSizeBytes)
        )
        StatRow(
            "Upstream Speed",
            "${metrics.upstreamSpeedRaw} Kbps"
        )
    }
}

@Composable
private fun FluxMetricsSection(observation: FluxObservation) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color(0xFF1A1F3A), shape = RoundedCornerShape(8.dp))
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text(
            text = "Node Metrics",
            fontSize = 16.sp,
            fontWeight = FontWeight.SemiBold,
            color = Color(0xFF00D9FF)  // cyan
        )

        val metrics = observation.nodeMetrics
        StatRow("Tier", metrics.tier)
        StatRow("Benchmark Score", metrics.benchmarkScore.toString())
        StatRow(
            "Uptime",
            "${String.format("%,d", metrics.uptime)} seconds"
        )

        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = "Performance",
            fontSize = 13.sp,
            fontWeight = FontWeight.Medium,
            color = Color(0xFF00D9FF)
        )

        StatRow("CPU Usage", "${String.format("%.1f", metrics.cpuUsage)}%")
        StatRow("Memory Usage", "${String.format("%.1f", metrics.memoryUsage)}%")
        StatRow("Storage Usage", "${String.format("%.1f", metrics.storageUsage)}%")
        StatRow(
            "Network Bandwidth",
            "${metrics.networkBandwidth} Mbps"
        )
        StatRow("Collateral Status", metrics.collateralStatus)
    }
}

@Composable
private fun UnknownObservationSection(observation: Observation) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color(0xFF1A1F3A), shape = RoundedCornerShape(8.dp))
            .padding(16.dp)
    ) {
        Text(
            text = "Unknown provider: ${observation.provider}",
            fontSize = 14.sp,
            color = Color(0xFFFF6B6B)
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = "Schema: ${observation.schema}",
            fontSize = 12.sp,
            color = Color(0xFFB0B8D4),
            fontFamily = FontFamily.Monospace
        )
    }
}

@Composable
private fun AuditTrailSection(observation: Observation) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color(0xFF1A1F3A), shape = RoundedCornerShape(8.dp))
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text(
            text = "Audit Trail",
            fontSize = 14.sp,
            fontWeight = FontWeight.SemiBold,
            color = Color(0xFF00D9FF)
        )

        // Source digest (abbreviated)
        val digestPreview = observation.sourceSha256.take(16) + "..."
        StatRow("Source Digest", digestPreview, monospace = true)

        // Disclaimer notice
        Text(
            text = "This is a ${observation.provider} ${observation.assurance.lowercase()} observation. " +
                   "It does NOT constitute independent proof. " +
                   "SKR payments are tracked separately and never conflated with ${observation.rewardAsset} rewards.",
            fontSize = 11.sp,
            color = Color(0xFFB0B8D4),
            lineHeight = 14.sp,
            modifier = Modifier.padding(top = 8.dp)
        )
    }
}

@Composable
private fun StatRow(
    label: String,
    value: String,
    color: Color = Color(0xFFB0B8D4),
    monospace: Boolean = false
) {
    Row(
        modifier = Modifier
            .fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = label,
            fontSize = 12.sp,
            color = Color(0xFF7A8199),
            fontWeight = FontWeight.Medium
        )
        Text(
            text = value,
            fontSize = 12.sp,
            color = color,
            fontFamily = if (monospace) FontFamily.Monospace else FontFamily.Default
        )
    }
}

private fun formatBytes(bytes: Long): String {
    return when {
        bytes >= 1024 * 1024 * 1024 -> String.format("%.2f GB", bytes / (1024.0 * 1024.0 * 1024.0))
        bytes >= 1024 * 1024 -> String.format("%.2f MB", bytes / (1024.0 * 1024.0))
        bytes >= 1024 -> String.format("%.2f KB", bytes / 1024.0)
        else -> "$bytes B"
    }
}
