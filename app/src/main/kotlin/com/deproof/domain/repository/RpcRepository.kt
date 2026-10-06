package com.deproof.domain.repository

import java.math.BigDecimal

interface RpcRepository {
    suspend fun getBalance(address: String): Result<BigDecimal>
    suspend fun getTokenBalance(address: String, mint: String): Result<BigDecimal>
    suspend fun getHealth(): Result<String>
    suspend fun estimateFee(instruction: String): Result<Long>
    suspend fun simulateTransaction(instruction: String): Result<Boolean>
}
