package com.deproof

/**
 * Staging environment configuration.
 * This file is used for staging builds against devnet and testing infrastructure.
 * Enables additional logging and feature flags for testing.
 */
object EnvironmentConfig {
    const val ENVIRONMENT = "staging"
    const val DEBUG_LOGGING = true
    const val ENABLE_CRASH_REPORTING = true
    const val API_TIMEOUT_SECONDS = 60
    const val RETRY_MAX_ATTEMPTS = 5
    const val FEATURE_FLAGS_ENABLED = true
}
