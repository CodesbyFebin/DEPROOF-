package com.deproof.ui.wallet

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.deproof.data.wallet.MobileWalletAdapterClient
import com.deproof.data.wallet.SolanaWalletRepository
import com.deproof.domain.repository.WalletAccount
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

sealed class WalletState {
    object Disconnected : WalletState()
    object Connecting : WalletState()
    data class Connected(val account: WalletAccount) : WalletState()
    data class Error(val message: String) : WalletState()
}

class WalletViewModel(
    private val walletRepository: SolanaWalletRepository
) : ViewModel() {

    private val _walletState = MutableStateFlow<WalletState>(WalletState.Disconnected)
    val walletState: StateFlow<WalletState> = _walletState

    private val _availableWallets = MutableStateFlow<List<String>>(emptyList())
    val availableWallets: StateFlow<List<String>> = _availableWallets

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading

    init {
        loadAvailableWallets()
    }

    fun connect(walletPackage: String = "com.phantom") {
        viewModelScope.launch {
            _isLoading.value = true
            _walletState.value = WalletState.Connecting

            val result = walletRepository.connect()
            result
                .onSuccess { account ->
                    _walletState.value = WalletState.Connected(account)
                }
                .onFailure { error ->
                    _walletState.value = WalletState.Error(error.message ?: "Connection failed")
                }
            _isLoading.value = false
        }
    }

    fun disconnect() {
        viewModelScope.launch {
            walletRepository.disconnect()
            _walletState.value = WalletState.Disconnected
        }
    }

    fun signTransaction(transactionData: String) {
        viewModelScope.launch {
            _isLoading.value = true
            val result = walletRepository.signTransaction(transactionData)
            result
                .onSuccess { signResult ->
                    // Handle signature
                }
                .onFailure { error ->
                    _walletState.value = WalletState.Error(error.message ?: "Sign failed")
                }
            _isLoading.value = false
        }
    }

    fun signMessage(message: String) {
        viewModelScope.launch {
            _isLoading.value = true
            val result = walletRepository.signMessage(message)
            result
                .onSuccess { signature ->
                    // Handle signed message
                }
                .onFailure { error ->
                    _walletState.value = WalletState.Error(error.message ?: "Sign failed")
                }
            _isLoading.value = false
        }
    }

    private fun loadAvailableWallets() {
        viewModelScope.launch {
            val result = walletRepository.getAvailableWallets()
            result
                .onSuccess { wallets ->
                    _availableWallets.value = wallets
                }
                .onFailure { error ->
                    _availableWallets.value = listOf("com.phantom")
                }
        }
    }
}
