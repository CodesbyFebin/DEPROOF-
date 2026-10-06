package com.deproof

/**
 * Production environment configuration.
 * This file is used for production builds and production releases.
 */
object EnvironmentConfig {
    const val ENVIRONMENT = "production"
    const val DEBUG_LOGGING = false
    const val ENABLE_CRASH_REPORTING = true
    const val API_TIMEOUT_SECONDS = 30
    const val RETRY_MAX_ATTEMPTS = 3
    const val FEATURE_FLAGS_ENABLED = false
}
