package com.deproof.domain

import java.math.BigDecimal

data class PublicKey(val value: String) {
    init {
        require(value.isNotEmpty() && value.length in 32..44) { "Invalid public key format" }
    }

    override fun toString() = value
}

data class AccountMeta(
    val pubkey: PublicKey,
    val isSigner: Boolean,
    val isWritable: Boolean
)

data class SolanaInstruction(
    val programId: PublicKey,
    val accounts: List<AccountMeta>,
    val data: ByteArray
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is SolanaInstruction) return false
        return programId == other.programId &&
               accounts == other.accounts &&
               data.contentEquals(other.data)
    }

    override fun hashCode(): Int {
        var result = programId.hashCode()
        result = 31 * result + accounts.hashCode()
        result = 31 * result + data.contentHashCode()
        return result
    }
}

sealed class SolanaAction {
    abstract suspend fun validate(): Result<Unit>
    abstract suspend fun toInstruction(): Result<SolanaInstruction>

    // Token Operations (SPL)
    data class TransferSplToken(
        val mint: PublicKey,
        val from: PublicKey,
        val to: PublicKey,
        val amount: Long,
        val decimals: Int,
        val owner: PublicKey
    ) : SolanaAction() {
        override suspend fun validate(): Result<Unit> = runCatching {
            require(amount > 0) { "Amount must be positive" }
            require(decimals in 0..8) { "Decimals must be 0-8" }
            require(from != to) { "Cannot transfer to self" }
        }

        override suspend fun toInstruction(): Result<SolanaInstruction> = runCatching {
            val tokenProgram = PublicKey("TokenkegQfeZyiNwAJsyFbPVwwQQfubRS1z1wWahpgX")
            SolanaInstruction(
                programId = tokenProgram,
                accounts = listOf(
                    AccountMeta(from, isSigner = false, isWritable = true),
                    AccountMeta(to, isSigner = false, isWritable = true),
                    AccountMeta(owner, isSigner = true, isWritable = false)
                ),
                data = byteArrayOf()
            )
        }
    }

    data class MintSplToken(
        val mint: PublicKey,
        val to: PublicKey,
        val authority: PublicKey,
        val amount: Long
    ) : SolanaAction() {
        override suspend fun validate(): Result<Unit> = runCatching {
            require(amount > 0) { "Amount must be positive" }
        }

        override suspend fun toInstruction(): Result<SolanaInstruction> = runCatching {
            val tokenProgram = PublicKey("TokenkegQfeZyiNwAJsyFbPVwwQQfubRS1z1wWahpgX")
            SolanaInstruction(
                programId = tokenProgram,
                accounts = listOf(
                    AccountMeta(mint, isSigner = false, isWritable = true),
                    AccountMeta(to, isSigner = false, isWritable = true),
                    AccountMeta(authority, isSigner = true, isWritable = false)
                ),
                data = byteArrayOf()
            )
        }
    }

    data class BurnSplToken(
        val mint: PublicKey,
        val token: PublicKey,
        val owner: PublicKey,
        val amount: Long
    ) : SolanaAction() {
        override suspend fun validate(): Result<Unit> = runCatching {
            require(amount > 0) { "Amount must be positive" }
        }

        override suspend fun toInstruction(): Result<SolanaInstruction> = runCatching {
            val tokenProgram = PublicKey("TokenkegQfeZyiNwAJsyFbPVwwQQfubRS1z1wWahpgX")
            SolanaInstruction(
                programId = tokenProgram,
                accounts = listOf(
                    AccountMeta(token, isSigner = false, isWritable = true),
                    AccountMeta(mint, isSigner = false, isWritable = true),
                    AccountMeta(owner, isSigner = true, isWritable = false)
                ),
                data = byteArrayOf()
            )
        }
    }

    data class FreezeTokenAccount(
        val mint: PublicKey,
        val token: PublicKey,
        val authority: PublicKey
    ) : SolanaAction() {
        override suspend fun validate(): Result<Unit> = Result.success(Unit)

        override suspend fun toInstruction(): Result<SolanaInstruction> = runCatching {
            val tokenProgram = PublicKey("TokenkegQfeZyiNwAJsyFbPVwwQQfubRS1z1wWahpgX")
            SolanaInstruction(
                programId = tokenProgram,
                accounts = listOf(
                    AccountMeta(token, isSigner = false, isWritable = true),
                    AccountMeta(mint, isSigner = false, isWritable = false),
                    AccountMeta(authority, isSigner = true, isWritable = false)
                ),
                data = byteArrayOf()
            )
        }
    }

    data class ThawTokenAccount(
        val mint: PublicKey,
        val token: PublicKey,
        val authority: PublicKey
    ) : SolanaAction() {
        override suspend fun validate(): Result<Unit> = Result.success(Unit)

        override suspend fun toInstruction(): Result<SolanaInstruction> = runCatching {
            val tokenProgram = PublicKey("TokenkegQfeZyiNwAJsyFbPVwwQQfubRS1z1wWahpgX")
            SolanaInstruction(
                programId = tokenProgram,
                accounts = listOf(
                    AccountMeta(token, isSigner = false, isWritable = true),
                    AccountMeta(mint, isSigner = false, isWritable = false),
                    AccountMeta(authority, isSigner = true, isWritable = false)
                ),
                data = byteArrayOf()
            )
        }
    }

    // Stake Operations
    data class StakeCreateAccount(
        val stakeAccount: PublicKey,
        val authorized: PublicKey,
        val lockup: Long
    ) : SolanaAction() {
        override suspend fun validate(): Result<Unit> = Result.success(Unit)

        override suspend fun toInstruction(): Result<SolanaInstruction> = runCatching {
            val stakeProgram = PublicKey("Stake11111111111111111111111111111111111111")
            SolanaInstruction(
                programId = stakeProgram,
                accounts = listOf(
                    AccountMeta(stakeAccount, isSigner = true, isWritable = true)
                ),
                data = byteArrayOf()
            )
        }
    }

    data class StakeDelegateAction(
        val stakeAccount: PublicKey,
        val voteAccount: PublicKey,
        val clockSysvar: PublicKey,
        val authority: PublicKey
    ) : SolanaAction() {
        override suspend fun validate(): Result<Unit> = Result.success(Unit)

        override suspend fun toInstruction(): Result<SolanaInstruction> = runCatching {
            val stakeProgram = PublicKey("Stake11111111111111111111111111111111111111")
            SolanaInstruction(
                programId = stakeProgram,
                accounts = listOf(
                    AccountMeta(stakeAccount, isSigner = false, isWritable = true),
                    AccountMeta(voteAccount, isSigner = false, isWritable = false),
                    AccountMeta(clockSysvar, isSigner = false, isWritable = false),
                    AccountMeta(authority, isSigner = true, isWritable = false)
                ),
                data = byteArrayOf()
            )
        }
    }

    data class StakeDeactivate(
        val stakeAccount: PublicKey,
        val clockSysvar: PublicKey,
        val authority: PublicKey
    ) : SolanaAction() {
        override suspend fun validate(): Result<Unit> = Result.success(Unit)

        override suspend fun toInstruction(): Result<SolanaInstruction> = runCatching {
            val stakeProgram = PublicKey("Stake11111111111111111111111111111111111111")
            SolanaInstruction(
                programId = stakeProgram,
                accounts = listOf(
                    AccountMeta(stakeAccount, isSigner = false, isWritable = true),
                    AccountMeta(clockSysvar, isSigner = false, isWritable = false),
                    AccountMeta(authority, isSigner = true, isWritable = false)
                ),
                data = byteArrayOf()
            )
        }
    }

    data class StakeWithdraw(
        val stakeAccount: PublicKey,
        val to: PublicKey,
        val clockSysvar: PublicKey,
        val authority: PublicKey,
        val amount: Long
    ) : SolanaAction() {
        override suspend fun validate(): Result<Unit> = runCatching {
            require(amount > 0) { "Amount must be positive" }
        }

        override suspend fun toInstruction(): Result<SolanaInstruction> = runCatching {
            val stakeProgram = PublicKey("Stake11111111111111111111111111111111111111")
            SolanaInstruction(
                programId = stakeProgram,
                accounts = listOf(
                    AccountMeta(stakeAccount, isSigner = false, isWritable = true),
                    AccountMeta(to, isSigner = false, isWritable = true),
                    AccountMeta(clockSysvar, isSigner = false, isWritable = false),
                    AccountMeta(authority, isSigner = true, isWritable = false)
                ),
                data = byteArrayOf()
            )
        }
    }

    // System Program Operations
    data class TransferSol(
        val from: PublicKey,
        val to: PublicKey,
        val lamports: Long
    ) : SolanaAction() {
        override suspend fun validate(): Result<Unit> = runCatching {
            require(lamports > 0) { "Amount must be positive" }
            require(from != to) { "Cannot transfer to self" }
        }

        override suspend fun toInstruction(): Result<SolanaInstruction> = runCatching {
            val systemProgram = PublicKey("11111111111111111111111111111111")
            SolanaInstruction(
                programId = systemProgram,
                accounts = listOf(
                    AccountMeta(from, isSigner = true, isWritable = true),
                    AccountMeta(to, isSigner = false, isWritable = true)
                ),
                data = byteArrayOf()
            )
        }
    }

    data class CreateAccount(
        val from: PublicKey,
        val newAccount: PublicKey,
        val owner: PublicKey,
        val lamports: Long,
        val space: Long
    ) : SolanaAction() {
        override suspend fun validate(): Result<Unit> = runCatching {
            require(lamports > 0) { "Lamports must be positive" }
            require(space > 0) { "Space must be positive" }
        }

        override suspend fun toInstruction(): Result<SolanaInstruction> = runCatching {
            val systemProgram = PublicKey("11111111111111111111111111111111")
            SolanaInstruction(
                programId = systemProgram,
                accounts = listOf(
                    AccountMeta(from, isSigner = true, isWritable = true),
                    AccountMeta(newAccount, isSigner = true, isWritable = true)
                ),
                data = byteArrayOf()
            )
        }
    }

    // DeFi Operations
    data class SwapOnRaydium(
        val userTokenAccountIn: PublicKey,
        val userTokenAccountOut: PublicKey,
        val poolId: PublicKey,
        val amountIn: Long,
        val minAmountOut: Long
    ) : SolanaAction() {
        override suspend fun validate(): Result<Unit> = runCatching {
            require(amountIn > 0) { "Amount in must be positive" }
            require(minAmountOut >= 0) { "Minimum amount out must be non-negative" }
            require(amountIn > minAmountOut) { "Input must exceed minimum output" }
        }

        override suspend fun toInstruction(): Result<SolanaInstruction> = runCatching {
            SolanaInstruction(
                programId = poolId,
                accounts = listOf(
                    AccountMeta(userTokenAccountIn, isSigner = false, isWritable = true),
                    AccountMeta(userTokenAccountOut, isSigner = false, isWritable = true)
                ),
                data = byteArrayOf()
            )
        }
    }

    data class LendOnSolend(
        val userCollateral: PublicKey,
        val market: PublicKey,
        val amount: Long
    ) : SolanaAction() {
        override suspend fun validate(): Result<Unit> = runCatching {
            require(amount > 0) { "Amount must be positive" }
        }

        override suspend fun toInstruction(): Result<SolanaInstruction> = runCatching {
            SolanaInstruction(
                programId = market,
                accounts = listOf(
                    AccountMeta(userCollateral, isSigner = false, isWritable = true)
                ),
                data = byteArrayOf()
            )
        }
    }

    data class BorrowOnSolend(
        val userLoan: PublicKey,
        val market: PublicKey,
        val amount: Long
    ) : SolanaAction() {
        override suspend fun validate(): Result<Unit> = runCatching {
            require(amount > 0) { "Amount must be positive" }
        }

        override suspend fun toInstruction(): Result<SolanaInstruction> = runCatching {
            SolanaInstruction(
                programId = market,
                accounts = listOf(
                    AccountMeta(userLoan, isSigner = false, isWritable = true)
                ),
                data = byteArrayOf()
            )
        }
    }

    data class RepayOnSolend(
        val userLoan: PublicKey,
        val market: PublicKey,
        val amount: Long
    ) : SolanaAction() {
        override suspend fun validate(): Result<Unit> = runCatching {
            require(amount > 0) { "Amount must be positive" }
        }

        override suspend fun toInstruction(): Result<SolanaInstruction> = runCatching {
            SolanaInstruction(
                programId = market,
                accounts = listOf(
                    AccountMeta(userLoan, isSigner = false, isWritable = true)
                ),
                data = byteArrayOf()
            )
        }
    }

    // NFT/Metaplex Operations
    data class CreateMasterNft(
        val metadata: PublicKey,
        val mint: PublicKey,
        val authority: PublicKey
    ) : SolanaAction() {
        override suspend fun validate(): Result<Unit> = Result.success(Unit)

        override suspend fun toInstruction(): Result<SolanaInstruction> = runCatching {
            val metaplexProgram = PublicKey("metaqbxxUerdq28cj1RbAqKEsbh1HkMi8cHz92RsmjK")
            SolanaInstruction(
                programId = metaplexProgram,
                accounts = listOf(
                    AccountMeta(metadata, isSigner = false, isWritable = true),
                    AccountMeta(mint, isSigner = true, isWritable = true),
                    AccountMeta(authority, isSigner = true, isWritable = false)
                ),
                data = byteArrayOf()
            )
        }
    }

    data class MintNft(
        val metadata: PublicKey,
        val edition: PublicKey,
        val mint: PublicKey,
        val authority: PublicKey
    ) : SolanaAction() {
        override suspend fun validate(): Result<Unit> = Result.success(Unit)

        override suspend fun toInstruction(): Result<SolanaInstruction> = runCatching {
            val metaplexProgram = PublicKey("metaqbxxUerdq28cj1RbAqKEsbh1HkMi8cHz92RsmjK")
            SolanaInstruction(
                programId = metaplexProgram,
                accounts = listOf(
                    AccountMeta(metadata, isSigner = false, isWritable = true),
                    AccountMeta(edition, isSigner = false, isWritable = true),
                    AccountMeta(mint, isSigner = false, isWritable = true),
                    AccountMeta(authority, isSigner = true, isWritable = false)
                ),
                data = byteArrayOf()
            )
        }
    }

    data class BurnNft(
        val metadata: PublicKey,
        val mint: PublicKey,
        val authority: PublicKey
    ) : SolanaAction() {
        override suspend fun validate(): Result<Unit> = Result.success(Unit)

        override suspend fun toInstruction(): Result<SolanaInstruction> = runCatching {
            val metaplexProgram = PublicKey("metaqbxxUerdq28cj1RbAqKEsbh1HkMi8cHz92RsmjK")
            SolanaInstruction(
                programId = metaplexProgram,
                accounts = listOf(
                    AccountMeta(metadata, isSigner = false, isWritable = true),
                    AccountMeta(mint, isSigner = false, isWritable = true),
                    AccountMeta(authority, isSigner = true, isWritable = false)
                ),
                data = byteArrayOf()
            )
        }
    }

    data class ListNftForSale(
        val nftMint: PublicKey,
        val nftMetadata: PublicKey,
        val price: Long,
        val marketplace: PublicKey
    ) : SolanaAction() {
        override suspend fun validate(): Result<Unit> = runCatching {
            require(price > 0) { "Price must be positive" }
        }

        override suspend fun toInstruction(): Result<SolanaInstruction> = runCatching {
            SolanaInstruction(
                programId = marketplace,
                accounts = listOf(
                    AccountMeta(nftMint, isSigner = false, isWritable = false),
                    AccountMeta(nftMetadata, isSigner = false, isWritable = true)
                ),
                data = byteArrayOf()
            )
        }
    }

    // Governance/DAO Operations
    data class CreateGovernanceToken(
        val mint: PublicKey,
        val authority: PublicKey,
        val supply: Long
    ) : SolanaAction() {
        override suspend fun validate(): Result<Unit> = runCatching {
            require(supply > 0) { "Supply must be positive" }
        }

        override suspend fun toInstruction(): Result<SolanaInstruction> = runCatching {
            val tokenProgram = PublicKey("TokenkegQfeZyiNwAJsyFbPVwwQQfubRS1z1wWahpgX")
            SolanaInstruction(
                programId = tokenProgram,
                accounts = listOf(
                    AccountMeta(mint, isSigner = true, isWritable = true),
                    AccountMeta(authority, isSigner = false, isWritable = false)
                ),
                data = byteArrayOf()
            )
        }
    }

    data class CastVote(
        val proposal: PublicKey,
        val voterTokenAccount: PublicKey,
        val voter: PublicKey,
        val voteOption: Int
    ) : SolanaAction() {
        override suspend fun validate(): Result<Unit> = runCatching {
            require(voteOption in 0..2) { "Vote option must be 0-2" }
        }

        override suspend fun toInstruction(): Result<SolanaInstruction> = runCatching {
            SolanaInstruction(
                programId = proposal,
                accounts = listOf(
                    AccountMeta(proposal, isSigner = false, isWritable = true),
                    AccountMeta(voterTokenAccount, isSigner = false, isWritable = false),
                    AccountMeta(voter, isSigner = true, isWritable = false)
                ),
                data = byteArrayOf()
            )
        }
    }

    // Compressed NFT Operations (State Compression)
    data class MintCompressedNft(
        val compressionProgram: PublicKey,
        val leafDelegate: PublicKey,
        val leafOwner: PublicKey
    ) : SolanaAction() {
        override suspend fun validate(): Result<Unit> = Result.success(Unit)

        override suspend fun toInstruction(): Result<SolanaInstruction> = runCatching {
            SolanaInstruction(
                programId = compressionProgram,
                accounts = listOf(
                    AccountMeta(leafDelegate, isSigner = false, isWritable = true),
                    AccountMeta(leafOwner, isSigner = true, isWritable = false)
                ),
                data = byteArrayOf()
            )
        }
    }

    // Oracle/Price Feed Operations
    data class UpdatePriceFeed(
        val priceFeed: PublicKey,
        val oracle: PublicKey,
        val newPrice: Long
    ) : SolanaAction() {
        override suspend fun validate(): Result<Unit> = Result.success(Unit)

        override suspend fun toInstruction(): Result<SolanaInstruction> = runCatching {
            SolanaInstruction(
                programId = oracle,
                accounts = listOf(
                    AccountMeta(priceFeed, isSigner = false, isWritable = true),
                    AccountMeta(oracle, isSigner = true, isWritable = false)
                ),
                data = byteArrayOf()
            )
        }
    }

    // Liquidity Pool Operations
    data class AddLiquidity(
        val poolId: PublicKey,
        val tokenAAccount: PublicKey,
        val tokenBAccount: PublicKey,
        val amountA: Long,
        val amountB: Long
    ) : SolanaAction() {
        override suspend fun validate(): Result<Unit> = runCatching {
            require(amountA > 0) { "Amount A must be positive" }
            require(amountB > 0) { "Amount B must be positive" }
        }

        override suspend fun toInstruction(): Result<SolanaInstruction> = runCatching {
            SolanaInstruction(
                programId = poolId,
                accounts = listOf(
                    AccountMeta(tokenAAccount, isSigner = false, isWritable = true),
                    AccountMeta(tokenBAccount, isSigner = false, isWritable = true)
                ),
                data = byteArrayOf()
            )
        }
    }

    data class RemoveLiquidity(
        val poolId: PublicKey,
        val lpTokenAccount: PublicKey,
        val amount: Long
    ) : SolanaAction() {
        override suspend fun validate(): Result<Unit> = runCatching {
            require(amount > 0) { "Amount must be positive" }
        }

        override suspend fun toInstruction(): Result<SolanaInstruction> = runCatching {
            SolanaInstruction(
                programId = poolId,
                accounts = listOf(
                    AccountMeta(lpTokenAccount, isSigner = false, isWritable = true)
                ),
                data = byteArrayOf()
            )
        }
    }

    // Generic/Custom Program Instructions
    data class CustomInstruction(
        val programId: PublicKey,
        val accounts: List<AccountMeta>,
        val data: ByteArray,
        val description: String
    ) : SolanaAction() {
        override suspend fun validate(): Result<Unit> = runCatching {
            require(accounts.isNotEmpty()) { "Accounts list cannot be empty" }
        }

        override suspend fun toInstruction(): Result<SolanaInstruction> = runCatching {
            SolanaInstruction(
                programId = programId,
                accounts = accounts,
                data = data
            )
        }

        override fun equals(other: Any?): Boolean {
            if (this === other) return true
            if (other !is CustomInstruction) return false
            return programId == other.programId &&
                   accounts == other.accounts &&
                   data.contentEquals(other.data) &&
                   description == other.description
        }

        override fun hashCode(): Int {
            var result = programId.hashCode()
            result = 31 * result + accounts.hashCode()
            result = 31 * result + data.contentHashCode()
            result = 31 * result + description.hashCode()
            return result
        }
    }

    // Magic Eden marketplace operations
    data class ListNftOnMagicEden(
        val nftMint: PublicKey,
        val price: Long
    ) : SolanaAction() {
        override suspend fun validate(): Result<Unit> = runCatching {
            require(price > 0) { "Price must be positive" }
        }

        override suspend fun toInstruction(): Result<SolanaInstruction> = runCatching {
            SolanaInstruction(
                programId = PublicKey("M2mx93ekt1fmXSVkTrUL9xVFHkmME8HTUi5Cyc5aF7K"),
                accounts = listOf(
                    AccountMeta(nftMint, isSigner = false, isWritable = false)
                ),
                data = byteArrayOf()
            )
        }
    }

    data class BuyNftOnMagicEden(
        val nftMint: PublicKey,
        val seller: PublicKey,
        val buyer: PublicKey,
        val price: Long
    ) : SolanaAction() {
        override suspend fun validate(): Result<Unit> = runCatching {
            require(price > 0) { "Price must be positive" }
            require(seller != buyer) { "Seller and buyer must be different" }
        }

        override suspend fun toInstruction(): Result<SolanaInstruction> = runCatching {
            SolanaInstruction(
                programId = PublicKey("M2mx93ekt1fmXSVkTrUL9xVFHkmME8HTUi5Cyc5aF7K"),
                accounts = listOf(
                    AccountMeta(nftMint, isSigner = false, isWritable = false),
                    AccountMeta(seller, isSigner = false, isWritable = true),
                    AccountMeta(buyer, isSigner = true, isWritable = true)
                ),
                data = byteArrayOf()
            )
        }
    }
}
