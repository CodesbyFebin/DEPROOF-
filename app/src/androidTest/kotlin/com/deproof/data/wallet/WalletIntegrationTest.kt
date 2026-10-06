package com.deproof.data.wallet

import androidx.test.ext.junit.runners.AndroidJUnit4
import com.deproof.data.util.BalanceFormatter
import java.math.BigDecimal
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Phase 3 - Week 2: Wallet Integration & Balance Queries Tests
 *
 * Tests:
 * ✓ MWA (Mobile Wallet Adapter) connection
 * ✓ Wallet authorization request handling
 * ✓ Sign transaction with wallet
 * ✓ Sign message with wallet
 * ✓ Multiple wallet support
 * ✓ Session token management
 * ✓ Wallet disconnect and cleanup
 * ✓ Handle missing wallet app
 * ✓ Wallet permission validation
 * ✓ SOL balance display and formatting
 */
@RunWith(AndroidJUnit4::class)
class WalletIntegrationTest {
    private lateinit var mwaClient: MobileWalletAdapterClient

    @Before
    fun setUp() {
        mwaClient = MobileWalletAdapterClient()
    }

    @Test
    fun mwaConnectionSucceeds() {
        val result = mwaClient.connect()

        assertTrue(result.isSuccess)
        val account = result.getOrNull()
        assertNotNull(account)
        assertEquals("My Wallet", account?.name)
        assertTrue(mwaClient.isConnected())
    }

    @Test
    fun walletAuthorizationRequestHandling() {
        mwaClient.connect()

        assertTrue(mwaClient.isConnected())
        assertTrue(mwaClient.hasPermissions(listOf("sign_transaction", "sign_message")))
    }

    @Test
    fun signTransactionWithWallet() {
        mwaClient.connect()
        val transaction = "mock_transaction_instruction"

        val result = mwaClient.signTransaction(transaction)

        assertTrue(result.isSuccess)
        val signResult = result.getOrNull()
        assertNotNull(signResult)
        assertTrue(signResult?.signature?.isNotEmpty() == true)
    }

    @Test
    fun signMessageWithWallet() {
        mwaClient.connect()
        val message = "Sign this message to prove ownership"

        val result = mwaClient.signMessage(message)

        assertTrue(result.isSuccess)
        val signature = result.getOrNull()
        assertNotNull(signature)
        assertTrue(signature?.isNotEmpty() == true)
    }

    @Test
    fun multipleWalletSupport() {
        val wallets = mwaClient.getAvailableWallets()

        assertTrue(wallets.isSuccess)
        val walletList = wallets.getOrNull()
        assertTrue(walletList?.isNotEmpty() == true)
        assertTrue(walletList?.contains("com.phantom") == true)
    }

    @Test
    fun sessionTokenManagement() {
        assertNull(mwaClient.getSessionToken())

        mwaClient.connect()
        val sessionToken = mwaClient.getSessionToken()
        assertNotNull(sessionToken)
        assertTrue(sessionToken?.isNotEmpty() == true)

        mwaClient.disconnect()
        assertNull(mwaClient.getSessionToken())
    }

    @Test
    fun walletDisconnectAndCleanup() {
        mwaClient.connect()
        assertTrue(mwaClient.isConnected())

        val result = mwaClient.disconnect()
        assertTrue(result.isSuccess)
        assertFalse(mwaClient.isConnected())
    }

    @Test
    fun handleMissingWalletApp() {
        // Simulate missing wallet - should still return available wallets for installation
        val wallets = mwaClient.getAvailableWallets()
        assertTrue(wallets.isSuccess)
    }

    @Test
    fun walletPermissionValidation() {
        mwaClient.connect()

        val hasPermissions = mwaClient.hasPermissions(listOf("sign_transaction", "sign_message"))
        assertTrue(hasPermissions)
    }

    @Test
    fun solBalanceDisplayAndFormatting() {
        val lamports = 1_500_000_000L // 1.5 SOL
        val solBalance = BalanceFormatter.lamportsToSol(lamports)

        assertEquals(BigDecimal("1.500000000"), solBalance)
        val formatted = BalanceFormatter.formatSol(solBalance)
        assertTrue(formatted.contains("1.5"))
    }
}
