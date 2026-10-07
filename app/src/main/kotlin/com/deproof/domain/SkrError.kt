// SkrError.kt — Type-safe error hierarchy for SKR token operations.
//
// Distinguishes between:
// - ConfigNotFound (unavailable, not zero)
// - MintNotFound / AccountNotFound (zero balance)
// - RpcFailure (transient error)
// - InvalidDecimals (configuration error)
package com.deproof.domain

/**
 * Sealed hierarchy of errors that can occur during SKR token operations.
 * Each variant is distinct to allow proper error handling at the UI level.
 */
sealed class SkrError : Exception() {
    /**
     * The SKR mint account does not exist on-chain.
     * This typically means the token has not been deployed or the mint address is wrong.
     */
    data class MintNotFound(
        val mint: String,
        override val message: String = "SKR mint not found: $mint"
    ) : SkrError()

    /**
     * The SPL Token program PDA (Program Derived Address) for the account does not exist.
     * The account may not have been created yet, or the address is invalid.
     */
    data class AccountNotFound(
        val account: String,
        override val message: String = "Token account not found: $account"
    ) : SkrError()

    /**
     * The configuration (mint info, staking pool, etc.) is missing or invalid.
     * This is distinctly different from an account being absent — it means
     * the feature is unavailable, not that the user has zero balance.
     */
    data class ConfigNotFound(
        val configPda: String,
        override val message: String = "SKR configuration not found: $configPda"
    ) : SkrError()

    /**
     * The token's decimal places are invalid (negative or greater than 128).
     * This indicates a configuration error on-chain.
     */
    data class InvalidDecimals(
        val decimals: Int,
        override val message: String = "Invalid decimal places: $decimals"
    ) : SkrError()

    /**
     * An RPC call failed (network error, timeout, invalid response format, etc.).
     * This is a transient error that may succeed on retry.
     */
    data class RpcFailure(
        val endpoint: String? = null,
        val originalError: Exception? = null,
        override val message: String = "RPC call failed${endpoint?.let { " at $it" } ?: ""}: ${originalError?.message ?: "unknown error"}"
    ) : SkrError()

    /**
     * The RPC returned a response that could not be parsed.
     * May indicate an API version mismatch or corrupted response.
     */
    data class ParseError(
        val fieldName: String? = null,
        val rawValue: String? = null,
        override val message: String = "Failed to parse RPC response${fieldName?.let { " field: $it" } ?: ""}${rawValue?.let { ": $it" } ?: ""}"
    ) : SkrError()

    /**
     * The given amount is invalid (negative, exceeds u64 max, etc.).
     */
    data class InvalidAmount(
        val amount: String,
        override val message: String = "Invalid token amount: $amount"
    ) : SkrError()
}
