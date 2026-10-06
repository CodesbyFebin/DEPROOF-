package com.deproof.data.util

import java.math.BigDecimal
import java.math.RoundingMode

object BalanceFormatter {
    private const val SOL_DECIMALS = 9
    private const val SKR_DECIMALS = 2

    fun lamportsToSol(lamports: Long): BigDecimal {
        return BigDecimal(lamports)
            .divide(BigDecimal(1_000_000_000), SOL_DECIMALS, RoundingMode.HALF_UP)
    }

    fun formatSol(balance: BigDecimal): String {
        return balance.setScale(SOL_DECIMALS, RoundingMode.HALF_UP).toPlainString()
    }

    fun formatSkr(balance: BigDecimal): String {
        return balance.setScale(SKR_DECIMALS, RoundingMode.HALF_UP).toPlainString()
    }

    fun formatBalance(amount: BigDecimal, decimals: Int): String {
        return amount.setScale(decimals, RoundingMode.HALF_UP).toPlainString()
    }

    fun parseBalance(text: String): Result<BigDecimal> {
        return try {
            Result.success(BigDecimal(text))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    fun abbreviateBalance(amount: BigDecimal, decimals: Int = 2): String {
        val scaled = amount.setScale(decimals, RoundingMode.HALF_UP)
        return when {
            scaled >= BigDecimal(1_000_000) -> "${(scaled / BigDecimal(1_000_000)).toPlainString()}M"
            scaled >= BigDecimal(1_000) -> "${(scaled / BigDecimal(1_000)).toPlainString()}K"
            else -> scaled.toPlainString()
        }
    }

    fun isValidBalance(amount: BigDecimal): Boolean {
        return amount >= BigDecimal.ZERO
    }
}
