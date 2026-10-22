package io.github.chaevi.analytics.backend.ingest

import kotlinx.serialization.Serializable

@Serializable
data class EventResponse(
    val id: String,
    val projectId: String,
    val applicationId: String,
    val sessionId: String,
    val analyticsUserId: String?,
    val anonymousId: String,
    val eventKey: String,
    val eventSchemaVersion: Int,
    val properties: String,
    val context: String,
    val occurredAt: String,
    val receivedAt: String,
    val sequenceNumber: Int?,
)

@Serializable
data class CursorMeta(
    val nextCursor: String?,
    val hasMore: Boolean,
)

@Serializable
data class CursorListResponse<T>(
    val data: List<T>,
    val meta: CursorMeta,
)
