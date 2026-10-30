package io.github.chaevi.analytics.backend.replay

import kotlinx.serialization.Serializable

@Serializable
data class SessionListEntry(
    val sessionId: String,
    val anonymousId: String,
    val externalUserId: String? = null,
    val startedAt: String,
    val endedAt: String? = null,
    val lastEventAt: String,
    val eventCount: Long,
    val screenCount: Long,
    val device: DeviceInfo? = null,
)

@Serializable
data class DeviceInfo(
    val osName: String? = null,
    val osVersion: String? = null,
    val deviceModel: String? = null,
)

@Serializable
data class SessionListResponse(
    val sessions: List<SessionListEntry>,
)

@Serializable
data class SessionReplayResponse(
    val session: SessionSummary,
    val templates: Map<String, TemplateSnapshot> = emptyMap(),
    val timeline: List<TimelineEvent>,
)

@Serializable
data class TemplateSnapshot(
    val viewport: ViewportDto,
    val wireframe: kotlinx.serialization.json.JsonArray,
    val screenshot: String? = null,
)

@Serializable
data class SessionSummary(
    val sessionId: String,
    val anonymousId: String,
    val externalUserId: String? = null,
    val startedAt: String,
    val endedAt: String? = null,
    val device: DeviceInfo? = null,
)

@Serializable
data class TimelineEvent(
    val eventId: String,
    val eventKey: String,
    val occurredAt: String,
    val screenId: String? = null,
    val durationMs: Long? = null,
    val properties: Map<String, String> = emptyMap(),
)

// 화면 템플릿 수집
@Serializable
data class ScreenTemplateRequest(
    val appVersion: String,
    val templates: List<ScreenTemplateEntry>,
)

@Serializable
data class ScreenTemplateEntry(
    val screenId: String,
    val viewport: ViewportDto,
    val nodes: kotlinx.serialization.json.JsonArray,
)

@Serializable
data class ViewportDto(
    val width: Int,
    val height: Int,
)

@Serializable
data class ScreenTemplateResponse(
    val accepted: Int,
    val skipped: Int,
)

// 앱 화면 관리
@Serializable
data class AppScreenEntry(
    val id: String,
    val screenId: String,
    val appVersion: String,
    val viewportWidth: Int,
    val viewportHeight: Int,
    val nodeCount: Int,
    val hasScreenshot: Boolean,
    val screenshotUrl: String? = null,
)

@Serializable
data class ScreenshotUploadRequest(
    val screenshot: String,
)

// 화면 레이아웃 (수동 제작)
@Serializable
data class ScreenLayoutEntry(
    val id: String,
    val screenId: String,
    val displayName: String,
    val createdAt: String,
    val updatedAt: String,
)

@Serializable
data class ScreenLayoutDetail(
    val id: String,
    val screenId: String,
    val displayName: String,
    val layoutJson: String,
    val createdAt: String,
    val updatedAt: String,
)

@Serializable
data class CreateScreenLayoutRequest(
    val screenId: String,
    val displayName: String,
    val layoutJson: String,
)

@Serializable
data class UpdateScreenLayoutRequest(
    val displayName: String? = null,
    val layoutJson: String? = null,
)
