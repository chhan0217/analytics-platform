package io.github.chaevi.analytics.sdk.api

data class AnalyticsConfig(
    val apiKey: String,
    val baseUrl: String,
    val environment: String = "production",
    val appVersion: String? = null,
    val sessionTimeoutMs: Long = 30 * 60 * 1000L,
    val sessionMaxDurationMs: Long = 24 * 60 * 60 * 1000L,
    val flushIntervalMs: Long = 30 * 1000L,
    val maxBatchSize: Int = 100,
    val maxQueueSize: Int = 1000,
    val maxRetries: Int = 3,
    val enableLogging: Boolean = false,
    val errorListener: ((AnalyticsError) -> Unit)? = null,
)

data class AnalyticsError(
    val code: String,
    val message: String,
)
