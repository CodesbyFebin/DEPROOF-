package com.deproof.data.rpc

import com.deproof.domain.repository.RpcRepository
import java.math.BigDecimal
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class SolanaRpcRepository(
    private val rpcClient: SolanaRpcClient
) : RpcRepository {

    override suspend fun getBalance(address: String): Result<BigDecimal> = withContext(Dispatchers.IO) {
        try {
            rpcClient.getBalance(address)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun getTokenBalance(address: String, mint: String): Result<BigDecimal> = withContext(Dispatchers.IO) {
        try {
            rpcClient.getTokenBalance(address, mint)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun getHealth(): Result<String> = withContext(Dispatchers.IO) {
        try {
            rpcClient.getHealth()
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun estimateFee(instruction: String): Result<Long> = withContext(Dispatchers.IO) {
        try {
            rpcClient.estimateFee(instruction)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun simulateTransaction(instruction: String): Result<Boolean> = withContext(Dispatchers.IO) {
        try {
            rpcClient.simulateTransaction(instruction)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
