package io.github.chaevi.analytics.backend.ingest

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonObject

@Serializable
data class IngestRequest(
    val sdkInfo: SdkInfo? = null,
    val events: List<IngestEvent>,
)

@Serializable
data class SdkInfo(
    val sdkName: String? = null,
    val sdkVersion: String? = null,
)

@Serializable
data class IngestEvent(
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

@Serializable
data class IngestResponse(
    val accepted: Int,
    val duplicated: Int,
)
