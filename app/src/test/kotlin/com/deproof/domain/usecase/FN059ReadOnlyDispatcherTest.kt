package com.deproof.domain.usecase

import kotlinx.coroutines.runBlocking
import org.junit.Before
import org.junit.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class FN059ReadOnlyDispatcherTest {
    private lateinit var dispatcher: FN059ReadOnlyDispatcher

    @Before
    fun setup() {
        val verifiedAdapter = ConnectorAdapter(
            id = "local_db",
            name = "Local Database",
            type = AdapterType.LOCAL_STORAGE,
            isVerified = true,
            supportedQueries = listOf("get_receipt", "list_receipts")
        )

        val unverifiedAdapter = ConnectorAdapter(
            id = "rpc_mainnet",
            name = "Solana Mainnet RPC",
            type = AdapterType.NETWORK_RPC,
            isVerified = false,
            supportedQueries = listOf("get_transaction", "get_balance")
        )

        dispatcher = FN059ReadOnlyDispatcher(
            mapOf(
                "local_db" to verifiedAdapter,
                "rpc_mainnet" to unverifiedAdapter
            ),
            allowNetworkQueries = false
        )
    }

    @Test
    fun verifiedAdapterDispatchesSuccessfully() = runBlocking {
        val query = DispatcherQuery(
            queryType = "get_receipt",
            targetAdapter = "local_db",
            parameters = mapOf("receipt_id" to "rec-123")
        )

        val result = dispatcher.dispatch(query)
        assertTrue(result.isSuccess())

        val dispatchResult = (result as com.deproof.domain.model.Result.Success).data
        assertTrue(dispatchResult is DispatcherResult.Success)
    }

    @Test
    fun unverifiedAdapterBlockedWithReason() = runBlocking {
        val query = DispatcherQuery(
            queryType = "get_transaction",
            targetAdapter = "rpc_mainnet",
            parameters = mapOf("tx_sig" to "abc123")
        )

        val result = dispatcher.dispatch(query)
        assertTrue(result.isSuccess())

        val dispatchResult = (result as com.deproof.domain.model.Result.Success).data
        assertTrue(dispatchResult is DispatcherResult.Blocked)
        assertEquals(
            DispatcherResult.BlockReason.ADAPTER_NOT_VERIFIED,
            (dispatchResult as DispatcherResult.Blocked).reason
        )
    }

    @Test
    fun nonexistentAdapterBlockedAsNotFound() = runBlocking {
        val query = DispatcherQuery(
            queryType = "query",
            targetAdapter = "unknown_adapter"
        )

        val result = dispatcher.dispatch(query)
        assertTrue(result.isSuccess())

        val dispatchResult = (result as com.deproof.domain.model.Result.Success).data
        assertTrue(dispatchResult is DispatcherResult.Blocked)
        assertEquals(
            DispatcherResult.BlockReason.ADAPTER_NOT_FOUND,
            (dispatchResult as DispatcherResult.Blocked).reason
        )
    }

    @Test
    fun unsupportedQueryTypeBlockedWithReason() = runBlocking {
        val query = DispatcherQuery(
            queryType = "unsupported_query",
            targetAdapter = "local_db"
        )

        val result = dispatcher.dispatch(query)
        assertTrue(result.isSuccess())

        val dispatchResult = (result as com.deproof.domain.model.Result.Success).data
        assertTrue(dispatchResult is DispatcherResult.Blocked)
        assertEquals(
            DispatcherResult.BlockReason.QUERY_NOT_SUPPORTED,
            (dispatchResult as DispatcherResult.Blocked).reason
        )
    }

    @Test
    fun adapterVerificationChangesDispatchOutcome() = runBlocking {
        // Initially unverified adapter should block
        val initialQuery = DispatcherQuery(
            queryType = "get_transaction",
            targetAdapter = "rpc_mainnet"
        )
        val initialResult = (dispatcher.dispatch(initialQuery)
            as com.deproof.domain.model.Result.Success).data
        assertTrue(initialResult is DispatcherResult.Blocked)

        // Verify the adapter
        val verifiedDispatcher = dispatcher.verifyAdapter("rpc_mainnet")

        // But network queries still blocked by policy
        val afterVerifyResult = (verifiedDispatcher.dispatch(initialQuery)
            as com.deproof.domain.model.Result.Success).data
        assertTrue(afterVerifyResult is DispatcherResult.Blocked)
        assertEquals(
            DispatcherResult.BlockReason.NETWORK_DISABLED,
            (afterVerifyResult as DispatcherResult.Blocked).reason
        )
    }

    @Test
    fun networkAdapterBlockedByPolicy() = runBlocking {
        val verifiedRpcDispatcher = dispatcher.verifyAdapter("rpc_mainnet")

        val query = DispatcherQuery(
            queryType = "get_balance",
            targetAdapter = "rpc_mainnet"
        )

        val result = (verifiedRpcDispatcher.dispatch(query)
            as com.deproof.domain.model.Result.Success).data
        assertTrue(result is DispatcherResult.Blocked)
        assertEquals(
            DispatcherResult.BlockReason.NETWORK_DISABLED,
            (result as DispatcherResult.Blocked).reason
        )
    }

    @Test
    fun registerNewAdapter() = runBlocking {
        val newAdapter = ConnectorAdapter(
            id = "local_cache",
            name = "Local Cache",
            type = AdapterType.LOCAL_STORAGE,
            isVerified = true,
            supportedQueries = listOf("get_cached_value")
        )

        val updatedDispatcher = dispatcher.registerAdapter(newAdapter)

        val query = DispatcherQuery(
            queryType = "get_cached_value",
            targetAdapter = "local_cache"
        )

        val result = updatedDispatcher.dispatch(query)
        assertTrue(result.isSuccess())

        val dispatchResult = (result as com.deproof.domain.model.Result.Success).data
        assertTrue(dispatchResult is DispatcherResult.Success)
    }

    @Test
    fun getAdapterStatus() = runBlocking {
        val statusResult = dispatcher.getAdapterStatus("local_db")
        assertTrue(statusResult.isSuccess())

        val status = (statusResult as com.deproof.domain.model.Result.Success).data
        assertEquals("local_db", status.adapterId)
        assertEquals("Local Database", status.name)
        assertEquals(AdapterType.LOCAL_STORAGE, status.type)
        assertTrue(status.isVerified)
        assertTrue(status.supportedQueries.contains("get_receipt"))
    }

    @Test
    fun getAdapterStatusNotFound() = runBlocking {
        val statusResult = dispatcher.getAdapterStatus("nonexistent")
        assertFalse(statusResult.isSuccess())
    }

    @Test
    fun listAllAdapters() = runBlocking {
        val listResult = dispatcher.listAdapters()
        assertTrue(listResult.isSuccess())

        val adapters = (listResult as com.deproof.domain.model.Result.Success).data
        assertEquals(2, adapters.size)

        val verifiedCount = adapters.count { it.isVerified }
        assertEquals(1, verifiedCount)

        val localAdapter = adapters.find { it.adapterId == "local_db" }
        assertNotNull(localAdapter)
        assertTrue(localAdapter.isVerified)
    }

    @Test
    fun invalidParametersBlocked() = runBlocking {
        val query = DispatcherQuery(
            queryType = "get_receipt",
            targetAdapter = "local_db",
            parameters = mapOf("receipt_id" to "")  // Empty parameter
        )

        val result = dispatcher.dispatch(query)
        assertTrue(result.isSuccess())

        val dispatchResult = (result as com.deproof.domain.model.Result.Success).data
        assertTrue(dispatchResult is DispatcherResult.Blocked)
        assertEquals(
            DispatcherResult.BlockReason.INVALID_PARAMETERS,
            (dispatchResult as DispatcherResult.Blocked).reason
        )
    }

    @Test
    fun emptyParametersAllowedForSomeQueries() = runBlocking {
        val query = DispatcherQuery(
            queryType = "list_receipts",
            targetAdapter = "local_db",
            parameters = emptyMap()  // Empty parameters acceptable
        )

        val result = dispatcher.dispatch(query)
        assertTrue(result.isSuccess())

        val dispatchResult = (result as com.deproof.domain.model.Result.Success).data
        assertTrue(dispatchResult is DispatcherResult.Success)
    }

    @Test
    fun noNetworkCallsPerformed() = runBlocking {
        // Verify that even verified adapters don't make network calls
        val verifiedRpc = dispatcher.verifyAdapter("rpc_mainnet")

        // Allow network queries in this test instance
        val networkEnabledDispatcher = FN059ReadOnlyDispatcher(
            mapOf(
                "rpc_mainnet" to ConnectorAdapter(
                    id = "rpc_mainnet",
                    name = "Solana Mainnet RPC",
                    type = AdapterType.NETWORK_RPC,
                    isVerified = true,
                    supportedQueries = listOf("get_transaction")
                )
            ),
            allowNetworkQueries = true
        )

        val query = DispatcherQuery(
            queryType = "get_transaction",
            targetAdapter = "rpc_mainnet"
        )

        val result = networkEnabledDispatcher.dispatch(query)
        assertTrue(result.isSuccess())

        val dispatchResult = (result as com.deproof.domain.model.Result.Success).data
        // Even with network enabled, should not make actual calls - BLOCKED is expected
        assertTrue(dispatchResult is DispatcherResult.Blocked)
    }
}
