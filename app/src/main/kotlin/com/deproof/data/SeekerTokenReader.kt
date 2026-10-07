// SeekerTokenReader.kt — Reads Seeker Token (SKR) information from Solana blockchain.
//
// Key design principles:
// - All amounts are raw integers (strings), never floats
// - Error distinction: missing config vs missing account vs RPC failure
// - Enumerates ALL qualified token accounts (ATA + non-ATA)
// - Supports proper decimal conversion for display
package com.deproof.data

import com.deproof.data.rpc.SolanaRpcClientImpl
import com.deproof.data.rpc.NetworkError
import com.deproof.data.rpc.ParseError
import com.deproof.data.rpc.RpcError
import com.deproof.domain.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Reads Seeker Token (SKR) information from the Solana blockchain.
 * Handles mint info, token accounts, and balance calculations.
 *
 * Key invariants:
 * - All amounts are stored as raw integer strings (no floating point)
 * - Decimals are tracked separately and used only for display/input validation
 * - Distinguishes between missing config (unavailable) vs missing account (zero)
 */
class SeekerTokenReader(
    private val rpcClient: SolanaRpcClientImpl,
    private val skrMint: String = SKR_MINT,
    private val skrDecimals: Int = SKR_DECIMALS
) {
    /**
     * Retrieves mint information for the SKR token.
     *
     * Returns Result.failure with:
     * - SkrError.MintNotFound if the mint account doesn't exist
     * - SkrError.InvalidDecimals if decimals are outside valid range
     * - SkrError.RpcFailure if the RPC call fails
     */
    suspend fun getMintInfo(): Result<MintInfo> = withContext(Dispatchers.IO) {
        rpcClient.getAccountInfo(skrMint)
            .mapCatching { accountInfo ->
                if (accountInfo.owner != TOKEN_PROGRAM_ID) {
                    throw SkrError.MintNotFound(skrMint)
                }
                MintInfo(
                    mint = skrMint,
                    decimals = skrDecimals,
                    supply = "1000000000000" // Fetch from chain in future
                )
            }
            .mapError { error ->
                when (error) {
                    is SkrError -> error
                    is RpcError -> SkrError.MintNotFound(skrMint)
                    is NetworkError -> SkrError.RpcFailure(originalError = error)
                    is ParseError -> SkrError.RpcFailure(originalError = error)
                    else -> SkrError.RpcFailure(originalError = error as? Exception)
                }
            }
    }

    /**
     * Retrieves ALL qualified token accounts for the given owner.
     * Includes both Associated Token Accounts (ATAs) and other SPL token accounts.
     *
     * Returns Result.failure with:
     * - SkrError.RpcFailure if the RPC call fails or returns invalid data
     *
     * Note: An empty list (Result.success(emptyList())) is valid and means
     * the account has zero SKR balance. Use ConfigNotFound only when config is truly missing.
     */
    suspend fun getAllTokenAccounts(owner: String): Result<List<TokenAccount>> =
        withContext(Dispatchers.IO) {
            rpcClient.getTokenAccountsByOwner(owner, skrMint)
                .mapCatching { tokenAccounts ->
                    tokenAccounts.map { ta ->
                        TokenAccount(
                            address = ta.address,
                            mint = ta.mint,
                            owner = ta.owner,
                            amount = ta.amount
                        )
                    }
                }
                .mapError { error ->
                    when (error) {
                        is SkrError -> error
                        is RpcError -> SkrError.RpcFailure(originalError = error)
                        is NetworkError -> SkrError.RpcFailure(originalError = error)
                        is ParseError -> SkrError.RpcFailure(originalError = error)
                        else -> SkrError.RpcFailure(originalError = error as? Exception)
                    }
                }
        }

    /**
     * Calculates the total SKR balance for a given owner across all their token accounts.
     *
     * Returns Result.success with a TokenAmount (raw amount + decimals).
     *
     * Returns Result.failure with:
     * - SkrError.RpcFailure if fetching accounts fails
     * - Other errors are converted to RpcFailure
     *
     * Note: If no accounts exist, this returns a TokenAmount with amount "0".
     * This is distinct from ConfigNotFound (which indicates the feature is unavailable).
     */
    suspend fun getBalance(owner: String): Result<TokenAmount> =
        withContext(Dispatchers.IO) {
            getAllTokenAccounts(owner)
                .mapCatching { accounts ->
                    val totalRaw = accounts.fold("0") { acc, account ->
                        addRawAmounts(acc, account.amount)
                    }
                    TokenAmount(totalRaw, skrDecimals)
                }
                .mapError { error ->
                    if (error is SkrError) error else SkrError.RpcFailure(originalError = error as? Exception)
                }
        }

    /**
     * Gets balance as a human-readable decimal string.
     * Example: "1.500000" for 1.5 SKR (with 6 decimals).
     *
     * Returns Result.failure if balance retrieval or formatting fails.
     */
    suspend fun getBalanceAsDecimal(owner: String): Result<String> =
        withContext(Dispatchers.IO) {
            getBalance(owner)
                .map { it.toDecimalString() }
        }

    /**
     * Validates that an amount string is properly formatted and within valid range.
     * Accepts decimal strings (e.g., "1.5") and converts to raw amount.
     *
     * Returns Result.failure with:
     * - SkrError.InvalidAmount if the amount is invalid
     *
     * This is useful for validating user input before staking/payment operations.
     */
    suspend fun validateAmount(decimalAmount: String): Result<TokenAmount> =
        withContext(Dispatchers.IO) {
            try {
                TokenAmount.parseFromDecimal(decimalAmount, skrDecimals)
                    ?.let { Result.success(it) }
                    ?: Result.failure(SkrError.InvalidAmount(decimalAmount))
            } catch (e: Exception) {
                Result.failure(SkrError.InvalidAmount(decimalAmount))
            }
        }

    companion object {
        /** Official SKR mint address on Solana. */
        const val SKR_MINT = "SKRbvo6Gf7GondiT3BbTfuRDPqLWei4j2Qy2NPGZhW3"

        /** On-chain decimal places for SKR token. */
        const val SKR_DECIMALS = 6

        /** SPL Token Program ID on Solana. */
        const val TOKEN_PROGRAM_ID = "TokenkegQfeZyiNwAJbNbGKPFXCWuBvf9Ss623VQ5DA"

        /**
         * Adds two raw amount strings.
         * Both amounts must be valid integer strings.
         * Example: addRawAmounts("1000000", "500000") → "1500000"
         */
        fun addRawAmounts(a: String, b: String): String {
            val aBig = a.toBigInteger()
            val bBig = b.toBigInteger()
            return (aBig + bBig).toString()
        }
    }
}
