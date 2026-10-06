package com.deproof.presentation.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.deproof.domain.model.Balance
import com.deproof.domain.model.Result
import com.deproof.domain.model.SignatureInfo
import com.deproof.domain.model.WalletInfo
import com.deproof.data.repository.RpcRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class NowUiState(
    val walletInfo: WalletInfo? = null,
    val solBalance: Balance? = null,
    val skrBalance: Balance? = null,
    val recentSignatures: List<SignatureInfo> = emptyList(),
    val isLoading: Boolean = false,
    val error: String? = null,
    val lastRefreshTime: Long = 0L
)

class NowViewModel(private val rpcRepository: RpcRepository) : ViewModel() {

    private val _uiState = MutableStateFlow(NowUiState())
    val uiState: StateFlow<NowUiState> = _uiState.asStateFlow()

    fun connectWallet(publicKey: String, walletType: String = "Phantom") {
        _uiState.value = _uiState.value.copy(
            walletInfo = WalletInfo(
                publicKey = publicKey,
                isConnected = true,
                walletType = walletType
            )
        )
        loadBalances(publicKey)
        loadRecentSignatures(publicKey)
    }

    fun disconnectWallet() {
        _uiState.value = NowUiState()
    }

    fun loadBalances(publicKey: String) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true)

            val solResult = rpcRepository.getBalance(publicKey)
            val solBalance = when (solResult) {
                is Result.Success -> solResult.data
                else -> null
            }

            val skrResult = rpcRepository.getTokenBalance(publicKey)
            val skrBalance = when (skrResult) {
                is Result.Success -> skrResult.data
                else -> null
            }

            _uiState.value = _uiState.value.copy(
                solBalance = solBalance,
                skrBalance = skrBalance,
                isLoading = false,
                error = if (solResult is Result.Error) solResult.exception.message else null,
                lastRefreshTime = System.currentTimeMillis()
            )
        }
    }

    fun loadRecentSignatures(publicKey: String, limit: Int = 10) {
        viewModelScope.launch {
            val result = rpcRepository.getSignaturesForAddress(publicKey, limit)
            val signatures = when (result) {
                is Result.Success -> result.data
                else -> emptyList()
            }

            _uiState.value = _uiState.value.copy(
                recentSignatures = signatures
            )
        }
    }

    fun refreshAll() {
        _uiState.value.walletInfo?.let { wallet ->
            loadBalances(wallet.publicKey)
            loadRecentSignatures(wallet.publicKey)
        }
    }

    fun clearError() {
        _uiState.value = _uiState.value.copy(error = null)
    }
}
