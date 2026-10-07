package com.example.domain

import java.util.*

data class WalletSession(
    val publicKey: String,
    val authToken: String,
    val walletName: String = "Unknown",
    val devnetMemoSigned: Boolean = false,
    val devnetMemoTimestamp: Long = 0L
) {
    fun isValid(): Boolean = publicKey.isNotEmpty() && authToken.isNotEmpty()

    fun isReadyForMainnet(): Boolean = isValid() && devnetMemoSigned
}

sealed class WalletError {
    data class AuthorizationFailed(val reason: String) : WalletError()
    data class SigningFailed(val reason: String) : WalletError()
    data class DevnetMemoFailed(val reason: String) : WalletError()
    data class InvalidPublicKey(val reason: String) : WalletError()
}
