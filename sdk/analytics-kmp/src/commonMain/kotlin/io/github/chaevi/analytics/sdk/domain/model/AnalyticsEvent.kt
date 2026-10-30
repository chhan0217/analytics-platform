package io.github.chaevi.analytics.sdk.domain.model

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonObject

@Serializable
data class AnalyticsEvent(
    val eventId: String,
    val sessionId: String,
    val anonymousId: String,
    val eventKey: String,
    val schemaVersion: Int = 1,
    val occurredAt: String,
    val sequenceNumber: Int? = null,
    val properties: JsonObject = JsonObject(emptyMap()),
    val context: JsonObject = JsonObject(emptyMap()),
    val externalUserId: String? = null,
)
