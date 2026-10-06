package com.deproof.presentation.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.deproof.domain.model.Receipt
import com.deproof.domain.model.Result
import com.deproof.data.repository.ReceiptRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class ReceiptsUiState(
    val receipts: List<Receipt> = emptyList(),
    val selectedReceipt: Receipt? = null,
    val isLoading: Boolean = false,
    val error: String? = null,
    val receiptCount: Int = 0,
    val exportedJson: String? = null,
    val deleteInProgress: Boolean = false,
    val successMessage: String? = null
)

class ReceiptsViewModel(private val receiptRepository: ReceiptRepository) : ViewModel() {

    private val _uiState = MutableStateFlow(ReceiptsUiState())
    val uiState: StateFlow<ReceiptsUiState> = _uiState.asStateFlow()

    init {
        loadReceipts()
        loadReceiptCount()
    }

    private fun loadReceipts() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true)

            val result = receiptRepository.getAllReceipts()
            when (result) {
                is Result.Success -> {
                    _uiState.value = _uiState.value.copy(
                        receipts = result.data.sortedByDescending { it.timestamp },
                        isLoading = false
                    )
                }
                is Result.Error -> {
                    _uiState.value = _uiState.value.copy(
                        error = result.exception.message ?: "Failed to load receipts",
                        isLoading = false
                    )
                }
                else -> {}
            }
        }
    }

    fun loadRecentReceipts(limit: Int = 10) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true)

            val result = receiptRepository.getRecentReceipts(limit)
            when (result) {
                is Result.Success -> {
                    _uiState.value = _uiState.value.copy(
                        receipts = result.data,
                        isLoading = false
                    )
                }
                is Result.Error -> {
                    _uiState.value = _uiState.value.copy(
                        error = result.exception.message,
                        isLoading = false
                    )
                }
                else -> {}
            }
        }
    }

    fun selectReceipt(receipt: Receipt) {
        _uiState.value = _uiState.value.copy(selectedReceipt = receipt)
    }

    fun deselectReceipt() {
        _uiState.value = _uiState.value.copy(selectedReceipt = null)
    }

    fun exportReceiptAsJson(receipt: Receipt) {
        viewModelScope.launch {
            val result = receiptRepository.exportReceiptAsJson(receipt)
            when (result) {
                is Result.Success -> {
                    _uiState.value = _uiState.value.copy(exportedJson = result.data)
                }
                is Result.Error -> {
                    _uiState.value = _uiState.value.copy(
                        error = "Export failed: ${result.exception.message}"
                    )
                }
                else -> {}
            }
        }
    }

    fun deleteReceipt(receipt: Receipt) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(deleteInProgress = true)

            val result = receiptRepository.deleteReceipt(receipt)
            when (result) {
                is Result.Success -> {
                    _uiState.value = _uiState.value.copy(
                        deleteInProgress = false,
                        successMessage = "Receipt deleted"
                    )
                    loadReceipts()
                    loadReceiptCount()
                }
                is Result.Error -> {
                    _uiState.value = _uiState.value.copy(
                        deleteInProgress = false,
                        error = "Delete failed: ${result.exception.message}"
                    )
                }
                else -> {}
            }
        }
    }

    private fun loadReceiptCount() {
        viewModelScope.launch {
            val result = receiptRepository.getReceiptCount()
            when (result) {
                is Result.Success -> {
                    _uiState.value = _uiState.value.copy(receiptCount = result.data)
                }
                else -> {}
            }
        }
    }

    fun refreshReceipts() {
        loadReceipts()
        loadReceiptCount()
    }

    fun clearError() {
        _uiState.value = _uiState.value.copy(error = null)
    }

    fun clearSuccessMessage() {
        _uiState.value = _uiState.value.copy(successMessage = null)
    }

    fun clearExportedJson() {
        _uiState.value = _uiState.value.copy(exportedJson = null)
    }
}
