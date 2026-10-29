package io.github.chaevi.analytics.backend.replay

import io.github.chaevi.analytics.backend.common.ApiException
import io.github.chaevi.analytics.backend.ingest.AnalyticsEventTable
import io.github.chaevi.analytics.backend.ingest.AnalyticsSessionTable
import io.github.chaevi.analytics.backend.ingest.AnalyticsUserTable
import io.ktor.http.HttpStatusCode
import kotlinx.datetime.Instant
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import org.jetbrains.exposed.sql.Op
import org.jetbrains.exposed.sql.SortOrder
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.SqlExpressionBuilder.greaterEq
import org.jetbrains.exposed.sql.SqlExpressionBuilder.lessEq
import org.jetbrains.exposed.sql.and
import org.jetbrains.exposed.sql.count
import org.jetbrains.exposed.sql.selectAll
import org.jetbrains.exposed.sql.transactions.transaction
import java.util.UUID

class ReplayService {
    @Suppress("LongMethod")
    fun sessionList(
        projectId: String,
        start: Instant,
        end: Instant,
        applicationId: String?,
        page: Int,
        pageSize: Int,
    ): Pair<List<SessionListEntry>, Long> {
        val projUuid = UUID.fromString(projectId)

        return transaction {
            val baseCondition = buildSessionCondition(projUuid, start, end, applicationId)

            val total =
                AnalyticsSessionTable
                    .selectAll()
                    .where(baseCondition)
                    .count()

            val sessions =
                AnalyticsSessionTable
                    .selectAll()
                    .where(baseCondition)
                    .orderBy(AnalyticsSessionTable.startedAt, SortOrder.DESC)
                    .limit(pageSize)
                    .offset(((page - 1) * pageSize).toLong())
                    .map { row ->
                        val sessionId = row[AnalyticsSessionTable.id].value
                        val analyticsUserId = row[AnalyticsSessionTable.analyticsUserId]?.value

                        val externalUserId =
                            if (analyticsUserId != null) {
                                AnalyticsUserTable
                                    .selectAll()
                                    .where { AnalyticsUserTable.id eq analyticsUserId }
                                    .firstOrNull()
                                    ?.get(AnalyticsUserTable.externalUserId)
                            } else {
                                null
                            }

                        val eventStats =
                            AnalyticsEventTable
                                .selectAll()
                                .where {
                                    (AnalyticsEventTable.projectId eq projUuid) and
                                        (AnalyticsEventTable.sessionId eq sessionId)
                                }.toList()

                        val eventCount = eventStats.size.toLong()
                        val screenCount =
                            eventStats
                                .filter { it[AnalyticsEventTable.eventKey] == "screen_view" }
                                .mapNotNull { extractJsonField(it[AnalyticsEventTable.properties], "screen_id") }
                                .distinct()
                                .size
                                .toLong()

                        val device =
                            eventStats.firstOrNull()?.let { firstEvent ->
                                parseDeviceInfo(firstEvent[AnalyticsEventTable.context])
                            }

                        SessionListEntry(
                            sessionId = sessionId.toString(),
                            anonymousId = row[AnalyticsSessionTable.anonymousId].toString(),
                            externalUserId = externalUserId,
                            startedAt = row[AnalyticsSessionTable.startedAt].toString(),
                            endedAt = row[AnalyticsSessionTable.endedAt]?.toString(),
                            lastEventAt = row[AnalyticsSessionTable.lastEventAt].toString(),
                            eventCount = eventCount,
                            screenCount = screenCount,
                            device = device,
                        )
                    }

            Pair(sessions, total)
        }
    }

    @Suppress("LongMethod")
    fun sessionReplay(
        projectId: String,
        sessionId: String,
    ): SessionReplayResponse {
        val projUuid = UUID.fromString(projectId)
        val sessUuid = UUID.fromString(sessionId)

        return transaction {
            val session =
                AnalyticsSessionTable
                    .selectAll()
                    .where {
                        (AnalyticsSessionTable.id eq sessUuid) and
                            (AnalyticsSessionTable.projectId eq projUuid)
                    }.firstOrNull()
                    ?: throw ApiException("NOT_FOUND", HttpStatusCode.NotFound, "세션을 찾을 수 없습니다")

            val analyticsUserId = session[AnalyticsSessionTable.analyticsUserId]?.value
            val externalUserId =
                if (analyticsUserId != null) {
                    AnalyticsUserTable
                        .selectAll()
                        .where { AnalyticsUserTable.id eq analyticsUserId }
                        .firstOrNull()
                        ?.get(AnalyticsUserTable.externalUserId)
                } else {
                    null
                }

            val events =
                AnalyticsEventTable
                    .selectAll()
                    .where {
                        (AnalyticsEventTable.projectId eq projUuid) and
                            (AnalyticsEventTable.sessionId eq sessUuid)
                    }.orderBy(AnalyticsEventTable.occurredAt, SortOrder.ASC)
                    .toList()

            val device =
                events.firstOrNull()?.let {
                    parseDeviceInfo(it[AnalyticsEventTable.context])
                }

            // screen_view 이벤트에 체류시간(duration_ms) 계산
            val screenViewDurations = calculateScreenDurations(events)

            val timeline =
                events.map { row ->
                    val eventId = row[AnalyticsEventTable.id].value.toString()
                    val eventKey = row[AnalyticsEventTable.eventKey]
                    val props = row[AnalyticsEventTable.properties]
                    val screenId = extractJsonField(props, "screen_id")

                    // screen_view가 아닌 이벤트의 주요 속성 추출
                    val properties =
                        if (eventKey != "screen_view" && eventKey != "screen_leave") {
                            extractMainProperties(props)
                        } else {
                            emptyMap()
                        }

                    TimelineEvent(
                        eventId = eventId,
                        eventKey = eventKey,
                        occurredAt = row[AnalyticsEventTable.occurredAt].toString(),
                        screenId = screenId,
                        durationMs = if (eventKey == "screen_view") screenViewDurations[eventId] else null,
                        properties = properties,
                    )
                }

            // 세션에서 방문한 screen_id 목록으로 템플릿 조회
            val screenIds =
                timeline
                    .filter { it.eventKey == "screen_view" }
                    .mapNotNull { it.screenId }
                    .distinct()

            val appVersion =
                events.firstOrNull()?.let {
                    extractJsonField(it[AnalyticsEventTable.context], "app_version")
                }

            val templates =
                if (screenIds.isNotEmpty() && appVersion != null) {
                    loadTemplates(projUuid, session[AnalyticsSessionTable.applicationId].value, screenIds, appVersion)
                } else {
                    emptyMap()
                }

            SessionReplayResponse(
                session =
                    SessionSummary(
                        sessionId = sessUuid.toString(),
                        anonymousId = session[AnalyticsSessionTable.anonymousId].toString(),
                        externalUserId = externalUserId,
                        startedAt = session[AnalyticsSessionTable.startedAt].toString(),
                        endedAt = session[AnalyticsSessionTable.endedAt]?.toString(),
                        device = device,
                    ),
                templates = templates,
                timeline = timeline,
            )
        }
    }

    /** screen_view 이벤트 간 시간 차이로 체류시간 계산 */
    @Suppress("NestedBlockDepth")
    private fun calculateScreenDurations(events: List<org.jetbrains.exposed.sql.ResultRow>): Map<String, Long> {
        val durations = mutableMapOf<String, Long>()
        val screenViews =
            events
                .filter { it[AnalyticsEventTable.eventKey] == "screen_view" }

        for (i in screenViews.indices) {
            val current = screenViews[i]
            val eventId = current[AnalyticsEventTable.id].value.toString()
            val currentTime = current[AnalyticsEventTable.occurredAt]

            if (i + 1 < screenViews.size) {
                val nextTime = screenViews[i + 1][AnalyticsEventTable.occurredAt]
                val durationMs = (nextTime - currentTime).inWholeMilliseconds
                durations[eventId] = durationMs
            } else {
                // 마지막 화면: screen_leave의 duration_ms 사용, 없으면 null
                val screenId = extractJsonField(current[AnalyticsEventTable.properties], "screen_id")
                if (screenId != null) {
                    val leaveEvent =
                        events.find { row ->
                            row[AnalyticsEventTable.eventKey] == "screen_leave" &&
                                extractJsonField(row[AnalyticsEventTable.properties], "screen_id") == screenId &&
                                row[AnalyticsEventTable.occurredAt] >= currentTime
                        }
                    if (leaveEvent != null) {
                        val durationStr =
                            extractJsonField(
                                leaveEvent[AnalyticsEventTable.properties],
                                "duration_ms",
                            )
                        durationStr?.toLongOrNull()?.let { durations[eventId] = it }
                    }
                }
            }
        }

        return durations
    }

    @Suppress("LoopWithTooManyJumpStatements")
    private fun loadTemplates(
        projectId: UUID,
        applicationId: UUID,
        screenIds: List<String>,
        appVersion: String,
    ): Map<String, TemplateSnapshot> {
        val templates = mutableMapOf<String, TemplateSnapshot>()

        for (sid in screenIds) {
            val row =
                ScreenTemplateTable
                    .selectAll()
                    .where {
                        (ScreenTemplateTable.projectId eq projectId) and
                            (ScreenTemplateTable.applicationId eq applicationId) and
                            (ScreenTemplateTable.screenId eq sid) and
                            (ScreenTemplateTable.appVersion eq appVersion)
                    }.firstOrNull() ?: continue

            val wireframeJson = row[ScreenTemplateTable.wireframe]
            val nodesArray =
                try {
                    Json.parseToJsonElement(wireframeJson).jsonArray
                } catch (_: Exception) {
                    continue
                }

            templates[sid] =
                TemplateSnapshot(
                    viewport =
                        ViewportDto(
                            width = row[ScreenTemplateTable.viewportWidth],
                            height = row[ScreenTemplateTable.viewportHeight],
                        ),
                    wireframe = nodesArray,
                    screenshot = row[ScreenTemplateTable.screenshot],
                )
        }

        return templates
    }

    private fun buildSessionCondition(
        projectId: UUID,
        start: Instant,
        end: Instant,
        applicationId: String?,
    ): Op<Boolean> {
        var condition: Op<Boolean> =
            (AnalyticsSessionTable.projectId eq projectId) and
                (AnalyticsSessionTable.startedAt greaterEq start) and
                (AnalyticsSessionTable.startedAt lessEq end)

        if (applicationId != null) {
            condition = condition and
                (AnalyticsSessionTable.applicationId eq UUID.fromString(applicationId))
        }

        return condition
    }

    companion object {
        fun extractJsonField(
            json: String,
            field: String,
        ): String? =
            try {
                val obj = Json.parseToJsonElement(json).jsonObject
                obj[field]?.jsonPrimitive?.content
            } catch (_: Exception) {
                null
            }

        fun parseDeviceInfo(contextJson: String): DeviceInfo? =
            try {
                val obj = Json.parseToJsonElement(contextJson).jsonObject
                DeviceInfo(
                    osName = obj["os_name"]?.jsonPrimitive?.content,
                    osVersion = obj["os_version"]?.jsonPrimitive?.content,
                    deviceModel = obj["device_model"]?.jsonPrimitive?.content,
                )
            } catch (_: Exception) {
                null
            }

        /** 이벤트 properties에서 주요 속성 최대 5개 추출 */
        fun extractMainProperties(propsJson: String): Map<String, String> =
            try {
                val obj = Json.parseToJsonElement(propsJson).jsonObject
                obj.entries
                    .take(5)
                    .associate { (k, v) -> k to v.jsonPrimitive.content }
            } catch (_: Exception) {
                emptyMap()
            }
    }
}
