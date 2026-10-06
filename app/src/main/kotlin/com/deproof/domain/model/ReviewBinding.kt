package com.deproof.domain.model

data class ReviewBinding(
    val txHash: String,
    val messageText: String,
    val messageHash: String,
    val verdict: String,
    val timestamp: Long
)
