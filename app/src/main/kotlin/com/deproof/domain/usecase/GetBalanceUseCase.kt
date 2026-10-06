package com.deproof.domain.usecase

import com.deproof.domain.model.Balance
import com.deproof.domain.model.Result
import com.deproof.data.repository.RpcRepository

class GetBalanceUseCase(private val rpcRepository: RpcRepository) {

    suspend fun getSolBalance(publicKey: String): Result<Balance> {
        return try {
            if (!isValidPublicKey(publicKey)) {
                Result.Error(IllegalArgumentException("Invalid public key format"))
            } else {
                rpcRepository.getBalance(publicKey)
            }
        } catch (e: Exception) {
            Result.Error(e)
        }
    }

    suspend fun getSkrBalance(publicKey: String): Result<Balance> {
        return try {
            if (!isValidPublicKey(publicKey)) {
                Result.Error(IllegalArgumentException("Invalid public key format"))
            } else {
                rpcRepository.getTokenBalance(publicKey)
            }
        } catch (e: Exception) {
            Result.Error(e)
        }
    }

    suspend fun getAllBalances(publicKey: String): Result<Pair<Balance?, Balance?>> {
        return try {
            if (!isValidPublicKey(publicKey)) {
                Result.Error(IllegalArgumentException("Invalid public key format"))
            }

            val solResult = rpcRepository.getBalance(publicKey)
            val skrResult = rpcRepository.getTokenBalance(publicKey)

            val solBalance = (solResult as? Result.Success)?.data
            val skrBalance = (skrResult as? Result.Success)?.data

            if (solBalance != null || skrBalance != null) {
                Result.Success(Pair(solBalance, skrBalance))
            } else {
                val error = (solResult as? Result.Error)?.exception
                    ?: (skrResult as? Result.Error)?.exception
                    ?: Exception("Failed to fetch balances")
                Result.Error(error)
            }
        } catch (e: Exception) {
            Result.Error(e)
        }
    }

    private fun isValidPublicKey(publicKey: String): Boolean {
        return publicKey.length in 34..44 && publicKey.isNotEmpty()
    }
}
