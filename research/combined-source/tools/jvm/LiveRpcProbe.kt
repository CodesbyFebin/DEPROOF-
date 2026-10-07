package com.example.probe

import com.example.data.Rpc
import com.example.domain.*
import java.io.File
import java.time.Instant

fun main(args: Array<String>) {
    val results=listOf("devnet" to "https://api.devnet.solana.com","mainnet-beta" to "https://api.mainnet-beta.solana.com").map {(cluster,endpoint) ->
        val start=Instant.now().toString()
        try {
            // Public System program account, not a user or connected wallet.
            val rpc=Rpc(endpoint,cluster);val genesis=rpc.genesis();val balance=rpc.balance("11111111111111111111111111111111")
            Json.obj("cluster" to cluster,"endpoint" to endpoint,"availability" to "AVAILABLE","genesis" to genesis,"account" to balance.address,"accountRole" to "PUBLIC_SYSTEM_PROGRAM_ACCOUNT_NOT_USER_WALLET","sol" to balance.value,"slot" to balance.slot,"observedAt" to balance.observedAt,"methods" to listOf("getGenesisHash","getBalance"),"assurance" to "RPC_PROVIDER_OBSERVATION_NOT_CRYPTOGRAPHIC_PROOF")
        } catch(e: Exception) {
            Json.obj("cluster" to cluster,"endpoint" to endpoint,"availability" to "UNAVAILABLE","balance" to null,"observedAt" to start,"error" to ((e as? Failure)?.code ?: e.javaClass.simpleName),"zeroBalanceClaimed" to false)
        }
    }
    val result=Json.obj("schema" to "deproof-read-only-rpc-probe-v1","results" to results,"spending" to false)
    File(args.single()).writeText(Json.mapper.writerWithDefaultPrettyPrinter().writeValueAsString(result))
    println(Json.mapper.writeValueAsString(result))
}
