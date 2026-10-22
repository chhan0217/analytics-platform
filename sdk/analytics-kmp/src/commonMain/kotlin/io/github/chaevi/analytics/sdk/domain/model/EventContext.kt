package io.github.chaevi.analytics.sdk.domain.model

import kotlinx.serialization.Serializable

@Serializable
data class EventContext(
    val appVersion: String? = null,
    val sdkVersion: String? = null,
    val osName: String? = null,
    val osVersion: String? = null,
    val deviceModel: String? = null,
    val deviceManufacturer: String? = null,
    val locale: String? = null,
    val timezone: String? = null,
    val screenWidth: Int? = null,
    val screenHeight: Int? = null,
    val networkType: String? = null,
)
