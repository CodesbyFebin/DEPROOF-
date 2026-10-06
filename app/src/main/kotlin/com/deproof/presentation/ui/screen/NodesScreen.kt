package com.deproof.presentation.ui.screen

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

data class NetworkNode(
    val id: String,
    val name: String,
    val status: String,
    val latency: Int,
    val bandwidth: Float,
    val isActive: Boolean
)

@Composable
fun NodesScreen() {
    val mockNodes = listOf(
        NetworkNode(
            id = "1",
            name = "Solana Validator 1",
            status = "Connected",
            latency = 45,
            bandwidth = 98.5f,
            isActive = true
        ),
        NetworkNode(
            id = "2",
            name = "Solana Validator 2",
            status = "Connected",
            latency = 52,
            bandwidth = 95.2f,
            isActive = true
        ),
        NetworkNode(
            id = "3",
            name = "Solana Validator 3",
            status = "Disconnected",
            latency = 0,
            bandwidth = 0f,
            isActive = false
        ),
        NetworkNode(
            id = "4",
            name = "Local RPC Node",
            status = "Connected",
            latency = 10,
            bandwidth = 99.8f,
            isActive = true
        )
    )

    val activeNodes = mockNodes.count { it.isActive }
    val totalNodes = mockNodes.size
    val avgLatency = if (activeNodes > 0) {
        mockNodes.filter { it.isActive }.map { it.latency }.average().toInt()
    } else {
        0
    }
    val avgBandwidth = if (activeNodes > 0) {
        mockNodes.filter { it.isActive }.map { it.bandwidth }.average().toFloat()
    } else {
        0f
    }

    Scaffold(
        topBar = {
            TopAppBar(title = { Text("Nodes & Bandwidth") })
        }
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item {
                NetworkStatusCard(
                    activeNodes = activeNodes,
                    totalNodes = totalNodes,
                    avgLatency = avgLatency,
                    avgBandwidth = avgBandwidth
                )
            }

            items(mockNodes) { node ->
                NodeCard(node = node)
            }
        }
    }
}

@Composable
private fun NetworkStatusCard(
    activeNodes: Int,
    totalNodes: Int,
    avgLatency: Int,
    avgBandwidth: Float
) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                "Network Status",
                style = MaterialTheme.typography.headlineSmall
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                StatusItem(
                    label = "Active Nodes",
                    value = "$activeNodes/$totalNodes"
                )

                StatusItem(
                    label = "Avg Latency",
                    value = "${avgLatency}ms"
                )

                StatusItem(
                    label = "Avg Bandwidth",
                    value = "${String.format("%.1f", avgBandwidth)}%"
                )
            }

            LinearProgressIndicator(
                progress = activeNodes.toFloat() / totalNodes.toFloat(),
                modifier = Modifier.fillMaxWidth()
            )

            Text(
                "Network is ${if (activeNodes > 0) "operational" else "offline"}",
                style = MaterialTheme.typography.bodySmall
            )
        }
    }
}

@Composable
private fun StatusItem(label: String, value: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(label, style = MaterialTheme.typography.labelSmall)
        Text(value, style = MaterialTheme.typography.headlineSmall)
    }
}

@Composable
private fun NodeCard(node: NetworkNode) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = if (node.isActive) Icons.Default.CheckCircle else Icons.Default.Warning,
                contentDescription = "Node Status",
                tint = if (node.isActive) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error,
                modifier = Modifier.size(32.dp)
            )

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    node.name,
                    style = MaterialTheme.typography.bodyLarge
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Text(
                        "Status: ${node.status}",
                        style = MaterialTheme.typography.bodySmall
                    )

                    if (node.isActive) {
                        Text(
                            "Latency: ${node.latency}ms",
                            style = MaterialTheme.typography.bodySmall
                        )

                        Text(
                            "BW: ${String.format("%.1f", node.bandwidth)}%",
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                }
            }

            if (node.isActive) {
                StatusBadge(
                    value = "${node.bandwidth.toInt()}%",
                    color = when {
                        node.bandwidth >= 90 -> MaterialTheme.colorScheme.primary
                        node.bandwidth >= 70 -> MaterialTheme.colorScheme.tertiary
                        else -> MaterialTheme.colorScheme.error
                    }
                )
            }
        }
    }
}

@Composable
private fun StatusBadge(value: String, color: androidx.compose.ui.graphics.Color) {
    Surface(
        color = color.copy(alpha = 0.2f),
        shape = RoundedCornerShape(12.dp)
    ) {
        Text(
            value,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
            style = MaterialTheme.typography.labelSmall,
            color = color
        )
    }
}
