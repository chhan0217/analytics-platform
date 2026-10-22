package io.github.chaevi.analytics.sdk.api

data class AnalyticsConfig(
    val apiKey: String,
    val baseUrl: String,
    val environment: String = "production",
    val sessionTimeoutMs: Long = 30 * 60 * 1000L,
    val flushIntervalMs: Long = 30 * 1000L,
    val maxBatchSize: Int = 100,
    val maxQueueSize: Int = 1000,
    val errorListener: ((AnalyticsError) -> Unit)? = null,
)

data class AnalyticsError(
    val code: String,
    val message: String,
)
