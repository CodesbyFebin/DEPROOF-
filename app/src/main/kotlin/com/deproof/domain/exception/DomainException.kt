package com.deproof.domain.exception

sealed class DomainException(message: String, cause: Throwable? = null) : Exception(message, cause) {
    data class NetworkError(
        val statusCode: Int? = null,
        override val message: String = "Network request failed",
        override val cause: Throwable? = null
    ) : DomainException(message, cause)

    data class TimeoutError(
        override val message: String = "Request timed out",
        override val cause: Throwable? = null
    ) : DomainException(message, cause)

    data class ParseError(
        override val message: String = "Failed to parse response",
        override val cause: Throwable? = null
    ) : DomainException(message, cause)

    data class NotFoundError(
        override val message: String = "Resource not found",
        override val cause: Throwable? = null
    ) : DomainException(message, cause)

    data class ValidationError(
        override val message: String = "Validation failed",
        override val cause: Throwable? = null
    ) : DomainException(message, cause)

    data class StorageError(
        override val message: String = "Storage operation failed",
        override val cause: Throwable? = null
    ) : DomainException(message, cause)

    data class UnknownError(
        override val message: String = "An unknown error occurred",
        override val cause: Throwable? = null
    ) : DomainException(message, cause)
}

fun Throwable.toDomainException(): DomainException = when (this) {
    is DomainException -> this
    is java.net.SocketTimeoutException, is java.util.concurrent.TimeoutException -> {
        DomainException.TimeoutError(cause = this)
    }
    is java.net.UnknownHostException, is java.net.ConnectException -> {
        DomainException.NetworkError(message = this.message ?: "Network connection failed", cause = this)
    }
    else -> DomainException.UnknownError(message = this.message ?: "Unknown error", cause = this)
}
