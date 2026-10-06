package com.deproof.data.rpc

import java.net.URL
import java.time.Duration

enum class SolanaNetwork {
    MAINNET, TESTNET, DEVNET
}

data class RpcEndpoint(
    val network: SolanaNetwork,
    val url: String,
    val timeout: Duration = Duration.ofSeconds(30)
)

class RpcClientFactory {
    fun createEndpoint(network: SolanaNetwork): RpcEndpoint {
        val url = when (network) {
            SolanaNetwork.MAINNET -> "https://api.mainnet-beta.solana.com"
            SolanaNetwork.TESTNET -> "https://api.testnet.solana.com"
            SolanaNetwork.DEVNET -> "https://api.devnet.solana.com"
        }
        return RpcEndpoint(network, url)
    }

    fun validateEndpoint(endpoint: RpcEndpoint): Result<Unit> {
        return try {
            val url = URL(endpoint.url)
            if (url.protocol != "https") {
                Result.failure(IllegalArgumentException("RPC endpoint must use HTTPS"))
            } else {
                Result.success(Unit)
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    fun getBackupEndpoints(network: SolanaNetwork): List<RpcEndpoint> {
        return when (network) {
            SolanaNetwork.MAINNET -> listOf(
                RpcEndpoint(network, "https://api.mainnet-beta.solana.com"),
                RpcEndpoint(network, "https://solana-api.projectserum.com")
            )
            SolanaNetwork.TESTNET -> listOf(
                RpcEndpoint(network, "https://api.testnet.solana.com")
            )
            SolanaNetwork.DEVNET -> listOf(
                RpcEndpoint(network, "https://api.devnet.solana.com")
            )
        }
    }
}
