package com.deproof.domain.util

import android.util.Log
import kotlinx.coroutines.delay

data class RetryConfig(
    val maxRetries: Int = 3,
    val initialDelayMs: Long = 100L,
    val maxDelayMs: Long = 5000L,
    val backoffMultiplier: Double = 2.0
)

suspend inline fun <T> retryWithExponentialBackoff(
    config: RetryConfig = RetryConfig(),
    crossinline shouldRetry: (Throwable) -> Boolean = { isTransientError(it) },
    crossinline block: suspend () -> T
): T {
    var lastException: Throwable? = null
    var delayMs = config.initialDelayMs

    repeat(config.maxRetries) {
        try {
            return block()
        } catch (e: Throwable) {
            lastException = e
            if (!shouldRetry(e)) {
                throw e
            }
            if (it < config.maxRetries - 1) {
                Log.d("RetryPolicy", "Retry attempt ${it + 1} after ${delayMs}ms due to: ${e.message}")
                delay(delayMs)
                delayMs = (delayMs * config.backoffMultiplier).toLong().coerceAtMost(config.maxDelayMs)
            }
        }
    }

    throw lastException ?: Exception("Max retries exceeded")
}

fun isTransientError(throwable: Throwable): Boolean = when (throwable) {
    is java.net.SocketTimeoutException,
    is java.net.ConnectException,
    is java.net.UnknownHostException,
    is java.util.concurrent.TimeoutException -> true
    else -> false
}

fun isNetworkError(throwable: Throwable): Boolean {
    return throwable is java.net.SocketException ||
            throwable is java.net.UnknownHostException ||
            throwable is java.net.ConnectException ||
            throwable is java.io.IOException
}
