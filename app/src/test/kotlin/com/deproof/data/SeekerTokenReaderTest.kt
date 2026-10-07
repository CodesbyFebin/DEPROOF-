// SeekerTokenReaderTest.kt — Comprehensive unit tests for SeekerTokenReader.
package com.deproof.data

import com.deproof.data.rpc.SolanaRpcClient
import com.deproof.domain.*
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

class SeekerTokenReaderTest {
    private lateinit var reader: SeekerTokenReader
    private lateinit var mockRpc: FakeSolanaRpcClient

    @Before
    fun setup() {
        mockRpc = FakeSolanaRpcClient()
        reader = SeekerTokenReader(mockRpc)
    }

    // ─── Mint Info Tests ────────────────────────────────────────────────────

    @Test
    fun `getMintInfo returns valid mint information`() = runTest {
        val result = reader.getMintInfo()
        assertTrue(result.isSuccess)

        val mintInfo = result.getOrNull()
        assertNotNull(mintInfo)
        assertEquals(SeekerTokenReader.SKR_MINT, mintInfo.mint)
        assertEquals(SeekerTokenReader.SKR_DECIMALS, mintInfo.decimals)
    }

    @Test
    fun `getMintInfo handles RPC failure`() = runTest {
        mockRpc.failNextCall = true
        val result = reader.getMintInfo()
        assertTrue(result.isFailure)
        assertTrue(result.exceptionOrNull() is SkrError.RpcFailure)
    }

    // ─── Token Accounts Tests ───────────────────────────────────────────────

    @Test
    fun `getAllTokenAccounts returns empty list for account with no SKR`() = runTest {
        val result = reader.getAllTokenAccounts("9B5X4...") // valid owner address
        assertTrue(result.isSuccess)
        assertEquals(emptyList(), result.getOrNull())
    }

    @Test
    fun `getAllTokenAccounts handles RPC failure`() = runTest {
        mockRpc.failNextCall = true
        val result = reader.getAllTokenAccounts("9B5X4...")
        assertTrue(result.isFailure)
        assertTrue(result.exceptionOrNull() is SkrError.RpcFailure)
    }

    // ─── Balance Tests ──────────────────────────────────────────────────────

    @Test
    fun `getBalance returns zero for account with no accounts`() = runTest {
        val result = reader.getBalance("9B5X4...")
        assertTrue(result.isSuccess)

        val balance = result.getOrNull()
        assertNotNull(balance)
        assertEquals("0", balance.rawAmount)
        assertEquals(6, balance.decimals)
    }

    @Test
    fun `getBalance handles RPC failure`() = runTest {
        mockRpc.failNextCall = true
        val result = reader.getBalance("9B5X4...")
        assertTrue(result.isFailure)
        assertTrue(result.exceptionOrNull() is SkrError.RpcFailure)
    }

    @Test
    fun `getBalanceAsDecimal returns zero as "0"`() = runTest {
        val result = reader.getBalanceAsDecimal("9B5X4...")
        assertTrue(result.isSuccess)
        assertEquals("0", result.getOrNull())
    }

    @Test
    fun `getBalanceAsDecimal handles RPC failure`() = runTest {
        mockRpc.failNextCall = true
        val result = reader.getBalanceAsDecimal("9B5X4...")
        assertTrue(result.isFailure)
    }

    // ─── Amount Validation Tests ────────────────────────────────────────────

    @Test
    fun `validateAmount accepts "0"`() = runTest {
        val result = reader.validateAmount("0")
        assertTrue(result.isSuccess)

        val amount = result.getOrNull()
        assertNotNull(amount)
        assertEquals("0", amount.rawAmount)
    }

    @Test
    fun `validateAmount accepts "1"`() = runTest {
        val result = reader.validateAmount("1")
        assertTrue(result.isSuccess)

        val amount = result.getOrNull()
        assertNotNull(amount)
        assertEquals("1000000", amount.rawAmount) // 1 with 6 decimals
    }

    @Test
    fun `validateAmount accepts "1.5"`() = runTest {
        val result = reader.validateAmount("1.5")
        assertTrue(result.isSuccess)

        val amount = result.getOrNull()
        assertNotNull(amount)
        assertEquals("1500000", amount.rawAmount)
    }

    @Test
    fun `validateAmount accepts "0.000001"`() = runTest {
        val result = reader.validateAmount("0.000001")
        assertTrue(result.isSuccess)

        val amount = result.getOrNull()
        assertNotNull(amount)
        assertEquals("1", amount.rawAmount)
    }

    @Test
    fun `validateAmount rejects empty string`() = runTest {
        val result = reader.validateAmount("")
        assertTrue(result.isFailure)
        assertTrue(result.exceptionOrNull() is SkrError.InvalidAmount)
    }

    @Test
    fun `validateAmount rejects negative amount`() = runTest {
        val result = reader.validateAmount("-1")
        assertTrue(result.isFailure)
        assertTrue(result.exceptionOrNull() is SkrError.InvalidAmount)
    }

    @Test
    fun `validateAmount rejects too many decimals`() = runTest {
        val result = reader.validateAmount("1.0000001") // 7 decimals, max is 6
        assertTrue(result.isFailure)
        assertTrue(result.exceptionOrNull() is SkrError.InvalidAmount)
    }

    @Test
    fun `validateAmount rejects invalid characters`() = runTest {
        val result = reader.validateAmount("1.5abc")
        assertTrue(result.isFailure)
        assertTrue(result.exceptionOrNull() is SkrError.InvalidAmount)
    }

    // ─── Amount Arithmetic Tests ────────────────────────────────────────────

    @Test
    fun `addRawAmounts correctly adds zero and zero`() {
        val result = SeekerTokenReader.addRawAmounts("0", "0")
        assertEquals("0", result)
    }

    @Test
    fun `addRawAmounts correctly adds non-zero amounts`() {
        val result = SeekerTokenReader.addRawAmounts("1000000", "500000")
        assertEquals("1500000", result)
    }

    @Test
    fun `addRawAmounts handles large numbers`() {
        val result = SeekerTokenReader.addRawAmounts("999999999999999", "1")
        assertEquals("1000000000000000", result)
    }

    @Test
    fun `addRawAmounts handles leading zeros in input`() {
        val result = SeekerTokenReader.addRawAmounts("000100", "000200")
        assertEquals("300", result)
    }
}

// ─── Test Double ────────────────────────────────────────────────────────────

/**
 * Fake implementation of SolanaRpcClient for testing.
 * Supports injecting failures and custom responses.
 */
class FakeSolanaRpcClient : SolanaRpcClient(
    object : RpcEndpoint {
        override val url: String = "http://localhost:8899"
        override val timeout: java.time.Duration = java.time.Duration.ofSeconds(5)
    }
) {
    var failNextCall = false

    override suspend fun getHealth(): Result<String> {
        return if (failNextCall) {
            failNextCall = false
            Result.failure(Exception("RPC failure"))
        } else {
            Result.success("ok")
        }
    }

    override suspend fun getBalance(address: String): Result<java.math.BigDecimal> {
        return if (failNextCall) {
            failNextCall = false
            Result.failure(Exception("RPC failure"))
        } else {
            Result.success(java.math.BigDecimal.ZERO)
        }
    }

    override suspend fun getTokenBalance(address: String, mint: String): Result<java.math.BigDecimal> {
        return if (failNextCall) {
            failNextCall = false
            Result.failure(Exception("RPC failure"))
        } else {
            Result.success(java.math.BigDecimal.ZERO)
        }
    }

    override suspend fun estimateFee(transaction: String): Result<Long> {
        return Result.success(5000L)
    }

    override suspend fun simulateTransaction(instruction: String): Result<Boolean> {
        return Result.success(true)
    }
}

// ─── RpcEndpoint interface (mock) ────────────────────────────────────────────

interface RpcEndpoint {
    val url: String
    val timeout: java.time.Duration
}
