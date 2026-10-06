package com.deproof.presentation.viewmodel

import androidx.lifecycle.ViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

data class TaskItem(
    val id: String,
    val title: String,
    val description: String,
    val isCompleted: Boolean,
    val timestamp: Long,
    val evidence: String? = null
)

data class TasksUiState(
    val tasks: List<TaskItem> = emptyList(),
    val selectedTask: TaskItem? = null,
    val completedCount: Int = 0,
    val totalCount: Int = 0
)

class TasksViewModel : ViewModel() {

    private val _uiState = MutableStateFlow(TasksUiState())
    val uiState: StateFlow<TasksUiState> = _uiState.asStateFlow()

    init {
        loadMockTasks()
    }

    private fun loadMockTasks() {
        val mockTasks = listOf(
            TaskItem(
                id = "1",
                title = "Connect Wallet",
                description = "Connect your Solana wallet to Deproof",
                isCompleted = true,
                timestamp = System.currentTimeMillis() - 86400000,
                evidence = "Phantom Wallet - [WALLET_ADDRESS]"
            ),
            TaskItem(
                id = "2",
                title = "Review First Transaction",
                description = "Review and sign your first transaction",
                isCompleted = true,
                timestamp = System.currentTimeMillis() - 43200000,
                evidence = "Transaction Signature: [SIGNATURE_HASH]"
            ),
            TaskItem(
                id = "3",
                title = "Verify SKR Balance",
                description = "Check your SKR token balance",
                isCompleted = false,
                timestamp = System.currentTimeMillis()
            ),
            TaskItem(
                id = "4",
                title = "Submit Contribution Proof",
                description = "Submit proof of network contribution",
                isCompleted = false,
                timestamp = System.currentTimeMillis()
            )
        )

        val completedCount = mockTasks.count { it.isCompleted }
        _uiState.value = _uiState.value.copy(
            tasks = mockTasks,
            completedCount = completedCount,
            totalCount = mockTasks.size
        )
    }

    fun selectTask(task: TaskItem) {
        _uiState.value = _uiState.value.copy(selectedTask = task)
    }

    fun deselectTask() {
        _uiState.value = _uiState.value.copy(selectedTask = null)
    }

    fun markTaskComplete(taskId: String, evidence: String? = null) {
        val updatedTasks = _uiState.value.tasks.map { task ->
            if (task.id == taskId) {
                task.copy(isCompleted = true, evidence = evidence ?: task.evidence)
            } else {
                task
            }
        }

        val completedCount = updatedTasks.count { it.isCompleted }
        _uiState.value = _uiState.value.copy(
            tasks = updatedTasks,
            completedCount = completedCount
        )
    }

    fun getProgressPercentage(): Float {
        val totalCount = _uiState.value.totalCount
        return if (totalCount > 0) {
            (_uiState.value.completedCount.toFloat() / totalCount.toFloat()) * 100
        } else {
            0f
        }
    }
}
