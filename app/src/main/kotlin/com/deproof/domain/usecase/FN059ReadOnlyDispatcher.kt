package com.deproof.domain.usecase

import androidx.annotation.NonNull
import com.deproof.domain.exception.DomainException
import com.deproof.domain.model.Result

/**
 * FN059: Read-Only Connector Dispatcher
 *
 * Dispatches read-only queries to verified connectors.
 * Unverified or unsupported adapters return BLOCKED with explicit reason.
 * Performs NO network calls; uses only local verification state.
 */
data class ConnectorAdapter(
    @NonNull val id: String,
    @NonNull val name: String,
    @NonNull val type: AdapterType,
    val isVerified: Boolean = false,
    @NonNull val supportedQueries: List<String> = emptyList()
)

enum class AdapterType {
    LOCAL_STORAGE,      // Local filesystem, database
    NETWORK_RPC,        // Solana RPC, block explorers
    PROVIDER_API,       // Service APIs (e.g., Quicknode)
    UNKNOWN             // Unrecognized
}

data class DispatcherQuery(
    @NonNull val queryType: String,
    @NonNull val targetAdapter: String,
    @NonNull val parameters: Map<String, String> = emptyMap()
)

sealed class DispatcherResult {
    data class Success(
        @NonNull val adapterId: String,
        @NonNull val queryType: String,
        @NonNull val data: String
    ) : DispatcherResult()

    data class Blocked(
        @NonNull val adapterId: String,
        @NonNull val reason: BlockReason
    ) : DispatcherResult()

    enum class BlockReason {
        ADAPTER_NOT_VERIFIED,     // Adapter exists but not verified
        ADAPTER_NOT_FOUND,        // Adapter ID not recognized
        QUERY_NOT_SUPPORTED,      // Query type not in supported list
        NETWORK_DISABLED,         // Network queries disabled by policy
        INVALID_PARAMETERS        // Query parameters invalid
    }
}

class FN059ReadOnlyDispatcher(
    @NonNull private val adapters: Map<String, ConnectorAdapter> = emptyMap(),
    private val allowNetworkQueries: Boolean = false
) {
    private val tag = "FN059Dispatcher"

    suspend fun dispatch(@NonNull query: DispatcherQuery): Result<DispatcherResult> = try {
        android.util.Log.d(tag, "Dispatching query: type=${query.queryType}, adapter=${query.targetAdapter}")

        // Step 1: Verify adapter exists
        val adapter = adapters[query.targetAdapter]
            ?: return Result.Success(
                DispatcherResult.Blocked(
                    adapterId = query.targetAdapter,
                    reason = DispatcherResult.BlockReason.ADAPTER_NOT_FOUND
                )
            )

        // Step 2: Verify adapter is verified
        if (!adapter.isVerified) {
            android.util.Log.w(tag, "Adapter not verified: ${adapter.id}")
            return Result.Success(
                DispatcherResult.Blocked(
                    adapterId = adapter.id,
                    reason = DispatcherResult.BlockReason.ADAPTER_NOT_VERIFIED
                )
            )
        }

        // Step 3: Verify query type is supported
        if (!adapter.supportedQueries.contains(query.queryType)) {
            android.util.Log.w(tag, "Query type not supported: ${query.queryType} on ${adapter.id}")
            return Result.Success(
                DispatcherResult.Blocked(
                    adapterId = adapter.id,
                    reason = DispatcherResult.BlockReason.QUERY_NOT_SUPPORTED
                )
            )
        }

        // Step 4: Validate parameters
        val paramValidation = validateParameters(query.parameters)
        if (!paramValidation) {
            android.util.Log.w(tag, "Invalid parameters for query: ${query.queryType}")
            return Result.Success(
                DispatcherResult.Blocked(
                    adapterId = adapter.id,
                    reason = DispatcherResult.BlockReason.INVALID_PARAMETERS
                )
            )
        }

        // Step 5: Check network policy for network-based adapters
        if (adapter.type == AdapterType.NETWORK_RPC || adapter.type == AdapterType.PROVIDER_API) {
            if (!allowNetworkQueries) {
                android.util.Log.w(tag, "Network queries disabled for adapter: ${adapter.id}")
                return Result.Success(
                    DispatcherResult.Blocked(
                        adapterId = adapter.id,
                        reason = DispatcherResult.BlockReason.NETWORK_DISABLED
                    )
                )
            }
        }

        // Step 6: Dispatch to verified local adapter (no actual network call)
        val result = dispatchToAdapter(adapter, query)
        android.util.Log.d(tag, "Query dispatched successfully to ${adapter.id}")

        Result.Success(result)
    } catch (e: Exception) {
        android.util.Log.e(tag, "Dispatcher error", e)
        Result.Error(DomainException.UnknownError("Dispatcher error: ${e.message}", e))
    }

    private suspend fun dispatchToAdapter(
        @NonNull adapter: ConnectorAdapter,
        @NonNull query: DispatcherQuery
    ): DispatcherResult {
        // No network calls performed - only local operations
        return when (adapter.type) {
            AdapterType.LOCAL_STORAGE -> {
                DispatcherResult.Success(
                    adapterId = adapter.id,
                    queryType = query.queryType,
                    data = "{\"source\": \"local_storage\", \"adapter\": \"${adapter.name}\"}"
                )
            }
            AdapterType.NETWORK_RPC, AdapterType.PROVIDER_API -> {
                // Network adapters are BLOCKED unless explicitly allowed and implementation is provided
                DispatcherResult.Blocked(
                    adapterId = adapter.id,
                    reason = DispatcherResult.BlockReason.NETWORK_DISABLED
                )
            }
            AdapterType.UNKNOWN -> {
                DispatcherResult.Blocked(
                    adapterId = adapter.id,
                    reason = DispatcherResult.BlockReason.ADAPTER_NOT_FOUND
                )
            }
        }
    }

    private fun validateParameters(@NonNull params: Map<String, String>): Boolean {
        // Validate parameter structure and content
        if (params.isEmpty()) {
            return true // Empty params valid for some queries
        }

        // Check for required fields depending on query type
        return params.values.all { it.isNotEmpty() }
    }

    fun registerAdapter(@NonNull adapter: ConnectorAdapter): FN059ReadOnlyDispatcher {
        val updated = adapters.toMutableMap()
        updated[adapter.id] = adapter
        return FN059ReadOnlyDispatcher(updated.toMap(), allowNetworkQueries)
    }

    fun verifyAdapter(@NonNull adapterId: String): FN059ReadOnlyDispatcher {
        val adapter = adapters[adapterId]
            ?: return this

        val verified = adapter.copy(isVerified = true)
        val updated = adapters.toMutableMap()
        updated[adapterId] = verified

        android.util.Log.d(tag, "Adapter verified: $adapterId")
        return FN059ReadOnlyDispatcher(updated.toMap(), allowNetworkQueries)
    }

    fun getAdapterStatus(@NonNull adapterId: String): Result<AdapterStatus> = try {
        val adapter = adapters[adapterId]
            ?: return Result.Error(
                DomainException.NotFoundError("Adapter not found: $adapterId")
            )

        val status = AdapterStatus(
            adapterId = adapter.id,
            name = adapter.name,
            type = adapter.type,
            isVerified = adapter.isVerified,
            supportedQueries = adapter.supportedQueries,
            queryCount = 0 // TODO: track query counts
        )
        Result.Success(status)
    } catch (e: Exception) {
        Result.Error(DomainException.UnknownError("Failed to get adapter status", e))
    }

    fun listAdapters(): Result<List<AdapterStatus>> = try {
        val statuses = adapters.values.map { adapter ->
            AdapterStatus(
                adapterId = adapter.id,
                name = adapter.name,
                type = adapter.type,
                isVerified = adapter.isVerified,
                supportedQueries = adapter.supportedQueries,
                queryCount = 0
            )
        }
        Result.Success(statuses)
    } catch (e: Exception) {
        Result.Error(DomainException.UnknownError("Failed to list adapters", e))
    }
}

data class AdapterStatus(
    @NonNull val adapterId: String,
    @NonNull val name: String,
    @NonNull val type: AdapterType,
    val isVerified: Boolean,
    @NonNull val supportedQueries: List<String>,
    val queryCount: Int = 0
)
