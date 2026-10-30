package io.github.chaevi.analytics.sdk.domain.model

import kotlinx.serialization.Serializable

@Serializable
data class IngestPayload(
    val sdkInfo: SdkInfo? = null,
    val events: List<AnalyticsEvent>,
)

@Serializable
data class SdkInfo(
    val sdkName: String,
    val sdkVersion: String,
)

@Serializable
data class IngestResult(
    val accepted: Int = 0,
    val duplicated: Int = 0,
)
