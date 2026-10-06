package com.deproof.domain.model

import java.math.BigDecimal

data class TransactionInstruction(
    val programId: String,
    val accounts: List<String>,
    val data: String
)

data class Transaction(
    val instructions: List<TransactionInstruction>,
    val feePayer: String? = null,
    val recentBlockhash: String? = null,
    val signatures: List<String> = emptyList()
)

sealed class TransactionStatus {
    object Pending : TransactionStatus()
    object Confirmed : TransactionStatus()
    object Failed : TransactionStatus()
    data class Error(val message: String) : TransactionStatus()
}

data class TransactionFee(
    val lamports: Long,
    val sol: BigDecimal
)

data class TransactionResult(
    val signature: String,
    val status: TransactionStatus,
    val fee: TransactionFee? = null,
    val timestamp: Long = System.currentTimeMillis()
)
