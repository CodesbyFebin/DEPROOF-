package com.deproof.presentation.ui.screen

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.RadioButtonUnchecked
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.deproof.presentation.viewmodel.TasksViewModel
import com.deproof.util.Formatters

@Composable
fun TasksScreen(viewModel: TasksViewModel) {
    val uiState by viewModel.uiState.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(title = { Text("Tasks & Evidence") })
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
                ProgressCard(
                    completedCount = uiState.completedCount,
                    totalCount = uiState.totalCount,
                    percentage = viewModel.getProgressPercentage()
                )
            }

            items(uiState.tasks) { task ->
                TaskItemCard(
                    task = task,
                    onSelect = { viewModel.selectTask(task) }
                )
            }
        }

        uiState.selectedTask?.let { task ->
            TaskDetailDialog(
                task = task,
                onDismiss = { viewModel.deselectTask() }
            )
        }
    }
}

@Composable
private fun ProgressCard(
    completedCount: Int,
    totalCount: Int,
    percentage: Float
) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                "Progress",
                style = MaterialTheme.typography.headlineSmall
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text("Completed", style = MaterialTheme.typography.labelSmall)
                    Text(
                        "$completedCount of $totalCount",
                        style = MaterialTheme.typography.headlineSmall
                    )
                }

                Text(
                    "${percentage.toInt()}%",
                    style = MaterialTheme.typography.displaySmall
                )
            }

            LinearProgressIndicator(
                progress = { percentage / 100f },
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}

@Composable
private fun TaskItemCard(
    task: com.deproof.presentation.viewmodel.TaskItem,
    onSelect: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        onClick = onSelect
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = if (task.isCompleted) Icons.Default.CheckCircle else Icons.Default.RadioButtonUnchecked,
                contentDescription = "Status",
                tint = if (task.isCompleted) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline,
                modifier = Modifier.size(24.dp)
            )

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    task.title,
                    style = MaterialTheme.typography.bodyLarge
                )

                Text(
                    task.description,
                    style = MaterialTheme.typography.bodySmall
                )

                if (!task.evidence.isNullOrEmpty()) {
                    Text(
                        "Evidence: ${task.evidence}",
                        style = MaterialTheme.typography.labelSmall
                    )
                }

                Text(
                    Formatters.formatTimestamp(task.timestamp),
                    style = MaterialTheme.typography.labelSmall
                )
            }
        }
    }
}

@Composable
private fun TaskDetailDialog(
    task: com.deproof.presentation.viewmodel.TaskItem,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(task.title) },
        text = {
            Column(
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    task.description,
                    style = MaterialTheme.typography.bodyMedium
                )

                Divider()

                Text(
                    "Status: ${if (task.isCompleted) "Completed" else "Pending"}",
                    style = MaterialTheme.typography.bodySmall
                )

                if (!task.evidence.isNullOrEmpty()) {
                    Text("Evidence:", style = MaterialTheme.typography.labelSmall)
                    Text(
                        task.evidence,
                        style = MaterialTheme.typography.bodySmall
                    )
                }

                Text(
                    "Date: ${Formatters.formatTimestamp(task.timestamp)}",
                    style = MaterialTheme.typography.labelSmall
                )
            }
        },
        confirmButton = {
            Button(onClick = onDismiss) {
                Text("Close")
            }
        }
    )
}
