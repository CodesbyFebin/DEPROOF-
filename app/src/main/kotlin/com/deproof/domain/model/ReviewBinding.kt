package com.deproof.domain.model

import androidx.annotation.NonNull

data class ReviewBinding(
    @NonNull val txHash: String,
    @NonNull val messageText: String,
    @NonNull val messageHash: String,
    @NonNull val verdict: String,
    val timestamp: Long
)
