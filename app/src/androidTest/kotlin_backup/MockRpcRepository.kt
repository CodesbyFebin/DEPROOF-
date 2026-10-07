package com.deproof.data.rpc

import com.deproof.domain.repository.RpcRepository
import java.io.IOException
import java.math.BigDecimal
import java.util.concurrent.TimeoutException

class MockRpcRepository : RpcRepository {
    var simulateNetworkFailure = false
    var simulateTimeout = false
    var simulateInsufficientFunds = false

    var mockBalance = BigDecimal("1.5")
    var mockTokenBalance = BigDecimal("5000.00")
    var mockHealth = "ok"
    var mockFee = 5000L

    override suspend fun getBalance(address: String): Result<BigDecimal> {
        return when {
            simulateNetworkFailure -> Result.failure(IOException("Network error"))
            simulateTimeout -> Result.failure(TimeoutException("Request timeout"))
            simulateInsufficientFunds -> Result.success(BigDecimal.ZERO)
            else -> Result.success(mockBalance)
        }
    }

    override suspend fun getTokenBalance(address: String, mint: String): Result<BigDecimal> {
        return when {
            simulateNetworkFailure -> Result.failure(IOException("Network error"))
            simulateTimeout -> Result.failure(TimeoutException("Request timeout"))
            else -> Result.success(mockTokenBalance)
        }
    }

    override suspend fun getHealth(): Result<String> {
        return when {
            simulateNetworkFailure -> Result.failure(IOException("Network error"))
            simulateTimeout -> Result.failure(TimeoutException("Request timeout"))
            else -> Result.success(mockHealth)
        }
    }

    override suspend fun estimateFee(instruction: String): Result<Long> {
        return when {
            simulateNetworkFailure -> Result.failure(IOException("Network error"))
            simulateTimeout -> Result.failure(TimeoutException("Request timeout"))
            else -> Result.success(mockFee)
        }
    }

    override suspend fun simulateTransaction(instruction: String): Result<Boolean> {
        return when {
            simulateNetworkFailure -> Result.failure(IOException("Network error"))
            simulateTimeout -> Result.failure(TimeoutException("Request timeout"))
            simulateInsufficientFunds -> Result.failure(IllegalArgumentException("insufficient funds"))
            else -> Result.success(true)
        }
    }
}
