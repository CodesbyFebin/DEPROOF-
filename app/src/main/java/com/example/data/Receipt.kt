package com.example.data

data class Receipt(
    val id: Int = 0,
    val outcome: String, // "signed", "rejected"
    val summary: String,
    val messageHash: String,
    val signature: String?, // null if rejected
    val slot: Long?,
    val broadcast: Boolean,
    val timestamp: Long,
    val submittedByClearance: Boolean = true,
    val walletAddress: String? = null
)
