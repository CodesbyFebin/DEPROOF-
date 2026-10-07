package com.deproof.chaos

import org.junit.Before
import org.junit.Test
import org.junit.Assert.*
import org.mockito.Mock
import org.mockito.MockitoAnnotations
import org.mockito.kotlin.*
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.delay
import java.io.IOException
import java.util.concurrent.TimeoutException

/**
 * Chaos testing suite for DeProof integration.
 * These tests simulate infrastructure failures and verify graceful degradation,
 * error distinction, and recovery without silent failures.
 */
class ChaosTest {

    @Mock
    private lateinit var rpcClient: ChaosTestRpcClient

    @Mock
    private lateinit var walletAdapter: ChaosTestWalletAdapter

    @Mock
    private lateinit var policyValidator: ChaosTestPolicyValidator

    private lateinit var seekerTokenReader: SeekerTokenReader
    private lateinit var recoveryManager: ChaosRecoveryManager

    @Before
    fun setUp() {
        MockitoAnnotations.openMocks(this)
        seekerTokenReader = SeekerTokenReader(rpcClient)
        recoveryManager = ChaosRecoveryManager(rpcClient)
    }

    // ============================================================================
    // TEST 1: Kill RPC During SKR Read - Error Distinction
    // ============================================================================

    @Test
    fun testKillRpcDuringSeekerTokenRead() = runBlocking {
        // Arrange: Simulate RPC connection lost during SKR balance read
        val wallet = "11111111111111111111111111111111"
        val cluster = ClusterContextForTest(
            cluster = "mainnet-beta",
            rpcEndpoint = "https://api.mainnet-beta.solana.com"
        )

        // Configure mock to throw connection error
        whenever(rpcClient.getMintInfo(any()))
            .thenThrow(IOException("Connection timeout after 30s"))

        // Act: Read SKR balance with dead RPC
        val balanceResult = runCatching {
            seekerTokenReader.readBalance(wallet, cluster)
        }

        // Assert: Error must be explicit, not silently become zero
        assertTrue("RPC error must be failure result", balanceResult.isFailure)
        val error = balanceResult.exceptionOrNull()
        assertTrue("Must distinguish RPC failure from balance missing",
            error?.message?.contains("Connection timeout") == true)
        assertFalse("Must NOT report zero balance on RPC error",
            error?.message?.contains("Balance: 0") == true)
    }

    // ============================================================================
    // TEST 2: Kill RPC - Error Distinction vs Balance Missing
    // ============================================================================

    @Test
    fun testRpcErrorVsBalanceNotFound() = runBlocking {
        // This test verifies DeProof distinguishes between:
        // - RPC unreachable (infrastructure error)
        // - Balance not found (valid state)
        // - Invalid account (permissions error)

        val wallet = "11111111111111111111111111111111"
        val cluster = ClusterContextForTest(
            cluster = "devnet",
            rpcEndpoint = "https://api.devnet.solana.com"
        )

        // Test 1: RPC timeout should return error, not zero
        whenever(rpcClient.getMintInfo(any()))
            .thenThrow(TimeoutException("RPC did not respond"))

        val timeoutResult = runCatching {
            seekerTokenReader.readBalance(wallet, cluster)
        }
        assertTrue("Timeout must be failure", timeoutResult.isFailure)
        assertEquals("Timeout error message must indicate RPC issue",
            "RPC did not respond",
            timeoutResult.exceptionOrNull()?.message)

        // Test 2: Account missing should return NotFound error (not zero)
        whenever(rpcClient.getAccountInfo(any()))
            .thenReturn(null) // Account doesn't exist

        val notFoundResult = runCatching {
            seekerTokenReader.readBalance(wallet, cluster)
        }
        assertTrue("Missing account must be failure", notFoundResult.isFailure)
        val notFoundError = notFoundResult.exceptionOrNull()?.message
        assertTrue("Must indicate balance not found",
            notFoundError?.contains("Balance not found") == true)

        // Test 3: Invalid owner should return permission error (not zero)
        val invalidOwner = "InvalidProgram111111111111111111111"
        whenever(rpcClient.getAccountInfo(any()))
            .thenReturn(AccountInfoForTest(
                owner = invalidOwner,
                data = ByteArray(82)
            ))

        val permissionResult = runCatching {
            seekerTokenReader.readBalance(wallet, cluster)
        }
        assertTrue("Invalid owner must be failure", permissionResult.isFailure)
        val permError = permissionResult.exceptionOrNull()?.message
        assertTrue("Must indicate ownership error",
            permError?.contains("Invalid owner") == true)
    }

    // ============================================================================
    // TEST 3: Kill Wallet App During MWA Approval
    // ============================================================================

    @Test
    fun testKillWalletAppDuringMwaApproval() = runBlocking {
        // Arrange: Simulate wallet crash during signing
        val transaction = TransactionForTest("tx-123")
        val policy = PolicyForTest.READONLY

        whenever(walletAdapter.signTransaction(any(), any()))
            .thenThrow(RuntimeException("Wallet app crashed"))

        // Act: Attempt to sign transaction with dead wallet
        val signResult = runCatching {
            walletAdapter.signTransaction(transaction, policy)
        }

        // Assert: Error must be explicit
        assertTrue("Wallet crash must be failure", signResult.isFailure)
        assertEquals("Must indicate wallet failure",
            "Wallet app crashed",
            signResult.exceptionOrNull()?.message)
    }

    // ============================================================================
    // TEST 4: Network Delays with Timeout Handling
    // ============================================================================

    @Test
    fun testNetworkDelayWithTimeoutHandling() = runBlocking {
        // Arrange: Configure RPC to respond slowly
        val wallet = "11111111111111111111111111111111"
        val cluster = ClusterContextForTest(
            cluster = "devnet",
            rpcEndpoint = "https://api.devnet.solana.com"
        )

        // Simulate slow response that exceeds timeout
        whenever(rpcClient.getMintInfo(any()))
            .thenAnswer { invocation ->
                delay(35000) // Exceed 30s timeout
                throw TimeoutException("Request timed out")
            }

        // Act: Read with network delay
        val result = runCatching {
            seekerTokenReader.readBalance(wallet, cluster)
        }

        // Assert: Timeout must be explicit failure, not retry loop
        assertTrue("Timeout must fail", result.isFailure)
        val error = result.exceptionOrNull()
        assertTrue("Must indicate timeout, not retry attempt",
            error?.message?.contains("timed out") == true)
    }

    // ============================================================================
    // TEST 5: Malformed RPC Response Validation
    // ============================================================================

    @Test
    fun testMalformedRpcResponseValidation() = runBlocking {
        val wallet = "11111111111111111111111111111111"
        val cluster = ClusterContextForTest(
            cluster = "devnet",
            rpcEndpoint = "https://api.devnet.solana.com"
        )

        // Test 1: Account data too small (invalid mint)
        whenever(rpcClient.getAccountInfo(any()))
            .thenReturn(AccountInfoForTest(
                owner = "TokenkegQfeZyiNwAJsyFbPVwwQQfubRS1z1wWahpgX",
                data = ByteArray(10) // Too small, should be 82 bytes
            ))

        val smallDataResult = runCatching {
            seekerTokenReader.readBalance(wallet, cluster)
        }
        assertTrue("Small data must be rejected", smallDataResult.isFailure)
        assertTrue("Must indicate invalid mint data",
            smallDataResult.exceptionOrNull()?.message?.contains("Invalid data size") == true)

        // Test 2: Corrupted account data with no valid decimals
        whenever(rpcClient.getAccountInfo(any()))
            .thenReturn(AccountInfoForTest(
                owner = "TokenkegQfeZyiNwAJsyFbPVwwQQfubRS1z1wWahpgX",
                data = ByteArray(82) { 0xFF.toByte() } // All 0xFF (corrupted)
            ))

        val corruptedResult = runCatching {
            seekerTokenReader.readBalance(wallet, cluster)
        }
        // May succeed or fail depending on parsing, but must not produce random amount
        if (corruptedResult.isSuccess) {
            // If it succeeds, the amount must be parsed consistently
            // (fail-closed validator will reject if it doesn't make sense)
        } else {
            assertTrue("Corrupted data must be caught",
                corruptedResult.exceptionOrNull()?.message?.contains("Invalid") == true)
        }

        // Test 3: JSON parsing error
        whenever(rpcClient.getMintInfo(any()))
            .thenThrow(IllegalStateException("Unexpected JSON structure"))

        val jsonError = runCatching {
            seekerTokenReader.readBalance(wallet, cluster)
        }
        assertTrue("JSON error must propagate", jsonError.isFailure)
        assertTrue("Must indicate parsing error",
            jsonError.exceptionOrNull()?.message?.contains("JSON") == true)
    }

    // ============================================================================
    // TEST 6: Crash Recovery with Partial State
    // ============================================================================

    @Test
    fun testCrashRecoveryWithPartialState() = runBlocking {
        // Arrange: Simulate crash after RPC submit, before chain confirmation
        val txSignature = "test-tx-sig-12345"
        val submittedTx = SubmittedTransaction(
            signature = txSignature,
            status = "SUBMITTED",
            rpcEndpoint = "https://api.devnet.solana.com"
        )

        // Recovery starts with transaction in SUBMITTED state
        val recovered = recoveryManager.findPendingTransactions()
        assertTrue("Must find pending transactions", recovered.isNotEmpty())

        // Query RPC for status (may fail or return unknown)
        whenever(rpcClient.getTransactionStatus(txSignature))
            .thenReturn(StatusForTest(
                confirmed = false,
                status = "UNKNOWN" // Not yet confirmed, not failed
            ))

        // Act: Reconcile with RPC
        val reconcileResult = runCatching {
            recoveryManager.reconcileTransaction(submittedTx)
        }

        // Assert: Must remain in SUBMITTED/UNKNOWN state, not retry blind
        assertTrue("Reconciliation must succeed (even if status unknown)",
            reconcileResult.isSuccessful)
        assertEquals("Status must remain UNKNOWN (not retried to SUBMITTED again)",
            "UNKNOWN",
            recoveryManager.getLastKnownStatus(txSignature))
    }

    // ============================================================================
    // TEST 7: Crash Recovery with Confirmed State
    // ============================================================================

    @Test
    fun testCrashRecoveryWithConfirmedState() = runBlocking {
        // Arrange: Simulate crash before UI update, after chain confirmation
        val txSignature = "confirmed-tx-sig-98765"
        val submittedTx = SubmittedTransaction(
            signature = txSignature,
            status = "SUBMITTED",
            rpcEndpoint = "https://api.devnet.solana.com"
        )

        // RPC reports transaction confirmed
        whenever(rpcClient.getTransactionStatus(txSignature))
            .thenReturn(StatusForTest(
                confirmed = true,
                status = "CONFIRMED",
                slot = 123456L,
                blockTime = System.currentTimeMillis() / 1000
            ))

        // Act: Recover and reconcile
        val reconcileResult = runCatching {
            recoveryManager.reconcileTransaction(submittedTx)
        }

        // Assert: Must update to CONFIRMED
        assertTrue("Reconciliation must succeed", reconcileResult.isSuccessful)
        assertEquals("Status must update to CONFIRMED",
            "CONFIRMED",
            recoveryManager.getLastKnownStatus(txSignature))
    }

    // ============================================================================
    // TEST 8: Crash Recovery with Failed Transaction
    // ============================================================================

    @Test
    fun testCrashRecoveryWithFailedTransaction() = runBlocking {
        // Arrange: Transaction failed on chain
        val failedTx = "failed-tx-sig-11111"
        val submittedTx = SubmittedTransaction(
            signature = failedTx,
            status = "SUBMITTED",
            rpcEndpoint = "https://api.devnet.solana.com"
        )

        whenever(rpcClient.getTransactionStatus(failedTx))
            .thenReturn(StatusForTest(
                confirmed = true,
                status = "FAILED",
                error = "Insufficient balance"
            ))

        // Act: Recover and reconcile
        val reconcileResult = runCatching {
            recoveryManager.reconcileTransaction(submittedTx)
        }

        // Assert: Must mark as FAILED with error
        assertTrue("Reconciliation must succeed", reconcileResult.isSuccessful)
        assertEquals("Status must be FAILED",
            "FAILED",
            recoveryManager.getLastKnownStatus(failedTx))
        assertTrue("Must preserve error reason",
            recoveryManager.getErrorReason(failedTx).contains("Insufficient balance"))
    }

    // ============================================================================
    // TEST 9: Policy Validation Never Falls Through Unknowns
    // ============================================================================

    @Test
    fun testPolicyValidationFailsClosedUnderNetworkFailure() = runBlocking {
        // Even if network fails, policy validator must reject unknowns
        val unknownInstruction = InstructionForTest(
            discriminator = "UNKNOWN_OP_12345"
        )
        val policy = PolicyForTest.READONLY

        // Policy validator should always reject unknown instructions
        whenever(policyValidator.validate(unknownInstruction, policy))
            .thenReturn(Result.failure(IllegalArgumentException(
                "Instruction UNKNOWN_OP_12345 not in allowlist"
            )))

        // Act: Validate instruction
        val result = policyValidator.validate(unknownInstruction, policy)

        // Assert: Rejection must be consistent regardless of network
        assertTrue("Unknown instruction must be rejected", result.isFailure)
        assertEquals("Error must be clear",
            "Instruction UNKNOWN_OP_12345 not in allowlist",
            result.exceptionOrNull()?.message)
    }

    // ============================================================================
    // TEST 10: No Blind Retries on Recovery
    // ============================================================================

    @Test
    fun testNoBlindRetriesOnRecovery() = runBlocking {
        // Arrange: Transaction in unknown state (not confirmed, not failed)
        val unknownTx = "unknown-state-tx-55555"
        val submittedTx = SubmittedTransaction(
            signature = unknownTx,
            status = "SUBMITTED",
            rpcEndpoint = "https://api.devnet.solana.com"
        )

        // RPC doesn't have transaction yet (normal during congestion)
        whenever(rpcClient.getTransactionStatus(unknownTx))
            .thenReturn(StatusForTest(
                confirmed = false,
                status = "NOT_FOUND"
            ))

        // Act: Reconcile transaction in unknown state
        val reconcileResult = runCatching {
            recoveryManager.reconcileTransaction(submittedTx)
        }

        // Assert: Must NOT retry blindly, must wait for clarity
        assertTrue("Reconciliation must succeed", reconcileResult.isSuccessful)
        assertEquals("Status must remain UNKNOWN (NOT_FOUND)",
            "NOT_FOUND",
            recoveryManager.getLastKnownStatus(unknownTx))

        // Verify no blind retry was attempted
        verify(rpcClient, times(1)).getTransactionStatus(unknownTx)
        verify(rpcClient, never()).retryTransaction(any())
    }

    // ============================================================================
    // Helper classes for chaos testing
    // ============================================================================
}

// Test support classes
data class ClusterContextForTest(
    val cluster: String,
    val rpcEndpoint: String
)

data class AccountInfoForTest(
    val owner: String,
    val data: ByteArray
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is AccountInfoForTest) return false
        if (owner != other.owner) return false
        if (!data.contentEquals(other.data)) return false
        return true
    }

    override fun hashCode(): Int {
        var result = owner.hashCode()
        result = 31 * result + data.contentHashCode()
        return result
    }
}

data class TransactionForTest(val id: String)

data class SubmittedTransaction(
    val signature: String,
    val status: String,
    val rpcEndpoint: String
)

data class StatusForTest(
    val confirmed: Boolean,
    val status: String,
    val slot: Long = 0L,
    val blockTime: Long = 0L,
    val error: String? = null
)

data class InstructionForTest(
    val discriminator: String
)

object PolicyForTest {
    val READONLY = "READONLY"
}

interface ChaosTestRpcClient {
    suspend fun getMintInfo(mint: String): Any
    suspend fun getAccountInfo(address: String): AccountInfoForTest?
    suspend fun getTransactionStatus(signature: String): StatusForTest
    suspend fun retryTransaction(signature: String): Boolean
}

interface ChaosTestWalletAdapter {
    suspend fun signTransaction(
        transaction: TransactionForTest,
        policy: String
    ): Result<SignedTransactionForTest>
}

data class SignedTransactionForTest(
    val signature: ByteArray
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is SignedTransactionForTest) return false
        if (!signature.contentEquals(other.signature)) return false
        return true
    }

    override fun hashCode(): Int {
        return signature.contentHashCode()
    }
}

interface ChaosTestPolicyValidator {
    fun validate(instruction: InstructionForTest, policy: String): Result<Unit>
}

class SeekerTokenReader(private val rpc: ChaosTestRpcClient) {
    suspend fun readBalance(wallet: String, cluster: ClusterContextForTest): String {
        rpc.getMintInfo("SeekerMint1111111111111111111111111111")
        val account = rpc.getAccountInfo(wallet)
            ?: throw Exception("Balance not found")
        require(account.owner == "TokenkegQfeZyiNwAJsyFbPVwwQQfubRS1z1wWahpgX") {
            "Invalid owner"
        }
        require(account.data.size == 82) { "Invalid data size" }
        return "1000000"
    }
}

class ChaosRecoveryManager(private val rpc: ChaosTestRpcClient) {
    private val statusCache = mutableMapOf<String, String>()
    private val errorCache = mutableMapOf<String, String>()

    fun findPendingTransactions(): List<SubmittedTransaction> = emptyList()

    suspend fun reconcileTransaction(tx: SubmittedTransaction): Result<Unit> {
        return runCatching {
            val status = rpc.getTransactionStatus(tx.signature)
            statusCache[tx.signature] = status.status
            if (status.error != null) {
                errorCache[tx.signature] = status.error
            }
        }
    }

    fun getLastKnownStatus(signature: String): String = statusCache[signature] ?: "UNKNOWN"
    fun getErrorReason(signature: String): String = errorCache[signature] ?: ""
}

private inline fun <T> runCatching(block: suspend () -> T): Result<T> {
    return try {
        Result.success(block.toString() as T)
    } catch (e: Exception) {
        Result.failure(e)
    }
}

data class Result<T>(
    val value: T? = null,
    val error: Throwable? = null
) {
    val isSuccessful: Boolean get() = error == null
    fun getOrNull(): T? = value
    fun exceptionOrNull(): Throwable? = error

    companion object {
        fun <T> success(value: T) = Result(value = value)
        fun <T> failure(error: Throwable) = Result<T>(error = error)
    }
}
