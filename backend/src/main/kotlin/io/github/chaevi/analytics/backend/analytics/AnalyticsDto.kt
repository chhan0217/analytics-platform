package io.github.chaevi.analytics.backend.analytics

import kotlinx.serialization.Serializable

@Serializable
data class OverviewResponse(
    val totalEvents: Long,
    val totalSessions: Long,
    val totalUsers: Long,
    val period: PeriodInfo,
)

@Serializable
data class PeriodInfo(
    val startDate: String,
    val endDate: String,
)

@Serializable
data class ScreenStats(
    val screens: List<ScreenEntry>,
)

@Serializable
data class ScreenEntry(
    val screenId: String,
    val viewCount: Long,
    val uniqueUsers: Long,
)

@Serializable
data class JourneyStats(
    val transitions: List<TransitionEntry>,
)

@Serializable
data class TransitionEntry(
    val fromScreen: String,
    val toScreen: String,
    val count: Long,
)

@Serializable
data class FunnelRequest(
    val startDate: String,
    val endDate: String,
    val steps: List<FunnelStep>,
    val scope: String = "SESSION",
    val applicationId: String? = null,
)

@Serializable
data class FunnelStep(
    val eventKey: String,
    val filter: Map<String, String>? = null,
)

@Serializable
data class FunnelResponse(
    val steps: List<FunnelStepResult>,
    val overallConversionRate: Double,
    val scope: String,
)

@Serializable
data class FunnelStepResult(
    val step: Int,
    val eventKey: String,
    val uniqueUsers: Long,
    val uniqueSessions: Long,
    val conversionRate: Double,
)
