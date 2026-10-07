// MintInfo.kt — On-chain SPL token mint information.
package com.deproof.domain

/**
 * Represents the core fields of an SPL Token mint account.
 * Used to track token metadata needed for balance calculations and transfers.
 */
data class MintInfo(
    /** Base58-encoded mint address (public key). */
    val mint: String,
    /** Decimal places (0-128, typically 6 or 9). */
    val decimals: Int,
    /** Total supply in raw units (lamports). */
    val supply: String,
    /** Mint authority account (may be burned). */
    val mintAuthority: String? = null
)

/**
 * Represents an SPL Token account (holding balance of a specific token).
 */
data class TokenAccount(
    /** Base58-encoded account address. */
    val address: String,
    /** The mint of the token stored in this account. */
    val mint: String,
    /** The owner of this token account (typically a wallet). */
    val owner: String,
    /** Raw token amount in this account (as a string). */
    val amount: String,
    /** Whether this is a delegate-authorized account. */
    val delegated: Boolean = false,
    /** Delegate authority if delegated=true. */
    val delegate: String? = null
)

/**
 * Container for all token-related data needed by the UI.
 */
data class TokenBalance(
    /** The raw amount (as TokenAmount with decimals). */
    val amount: TokenAmount,
    /** Human-readable decimal string (e.g., "1.5"). */
    val decimalString: String,
    /** The mint this balance is for. */
    val mint: String,
    /** Metadata about the mint. */
    val mintInfo: MintInfo? = null
)
