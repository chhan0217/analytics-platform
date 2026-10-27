package io.github.chaevi.analytics.backend.analytics

import io.github.chaevi.analytics.backend.ingest.AnalyticsEventTable
import kotlinx.datetime.Instant
import org.jetbrains.exposed.sql.Op
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.SqlExpressionBuilder.greaterEq
import org.jetbrains.exposed.sql.SqlExpressionBuilder.lessEq
import org.jetbrains.exposed.sql.and
import org.jetbrains.exposed.sql.count
import org.jetbrains.exposed.sql.selectAll
import org.jetbrains.exposed.sql.transactions.transaction
import java.util.UUID

class AnalyticsService {
    fun overview(
        projectId: String,
        start: Instant,
        end: Instant,
        applicationId: String?,
    ): OverviewResponse {
        val projUuid = UUID.fromString(projectId)

        return transaction {
            var condition: Op<Boolean> =
                (AnalyticsEventTable.projectId eq projUuid) and
                    (AnalyticsEventTable.occurredAt greaterEq start) and
                    (AnalyticsEventTable.occurredAt lessEq end)

            applicationId?.let {
                condition = condition and (AnalyticsEventTable.applicationId eq UUID.fromString(it))
            }

            val totalEvents =
                AnalyticsEventTable.selectAll().where { condition }.count()

            val sessionIds =
                AnalyticsEventTable
                    .select(AnalyticsEventTable.sessionId)
                    .where { condition }
                    .withDistinct()
                    .count()

            val userIds =
                AnalyticsEventTable
                    .select(AnalyticsEventTable.anonymousId)
                    .where { condition }
                    .withDistinct()
                    .count()

            OverviewResponse(
                totalEvents = totalEvents,
                totalSessions = sessionIds,
                totalUsers = userIds,
                period = PeriodInfo(start.toString(), end.toString()),
            )
        }
    }

    fun screens(
        projectId: String,
        start: Instant,
        end: Instant,
        applicationId: String?,
    ): ScreenStats {
        val projUuid = UUID.fromString(projectId)

        return transaction {
            var viewCondition: Op<Boolean> =
                (AnalyticsEventTable.projectId eq projUuid) and
                    (AnalyticsEventTable.eventKey eq "screen_view") and
                    (AnalyticsEventTable.occurredAt greaterEq start) and
                    (AnalyticsEventTable.occurredAt lessEq end)

            var leaveCondition: Op<Boolean> =
                (AnalyticsEventTable.projectId eq projUuid) and
                    (AnalyticsEventTable.eventKey eq "screen_leave") and
                    (AnalyticsEventTable.occurredAt greaterEq start) and
                    (AnalyticsEventTable.occurredAt lessEq end)

            applicationId?.let {
                val appUuid = UUID.fromString(it)
                viewCondition = viewCondition and (AnalyticsEventTable.applicationId eq appUuid)
                leaveCondition = leaveCondition and (AnalyticsEventTable.applicationId eq appUuid)
            }

            val viewRows =
                AnalyticsEventTable
                    .selectAll()
                    .where { viewCondition }
                    .map {
                        val props = it[AnalyticsEventTable.properties]
                        val screenId = extractJsonField(props, "screen_id") ?: "unknown"
                        Pair(screenId, it[AnalyticsEventTable.anonymousId].toString())
                    }

            val durationByScreen = mutableMapOf<String, MutableList<Long>>()
            AnalyticsEventTable
                .selectAll()
                .where { leaveCondition }
                .forEach {
                    val props = it[AnalyticsEventTable.properties]
                    val screenId = extractJsonField(props, "screen_id") ?: return@forEach
                    val duration = extractJsonField(props, "duration_ms")?.toLongOrNull() ?: return@forEach
                    durationByScreen.getOrPut(screenId) { mutableListOf() }.add(duration)
                }

            val grouped = viewRows.groupBy { it.first }
            val screens =
                grouped
                    .map { (screenId, entries) ->
                        val durations = durationByScreen[screenId]
                        val avgDuration = durations?.let { it.sum() / it.size }
                        ScreenEntry(
                            screenId = screenId,
                            viewCount = entries.size.toLong(),
                            uniqueUsers =
                                entries
                                    .map { it.second }
                                    .toSet()
                                    .size
                                    .toLong(),
                            avgDurationMs = avgDuration,
                        )
                    }.sortedByDescending { it.viewCount }

            ScreenStats(screens = screens)
        }
    }

    @Suppress("LongMethod")
    fun journeys(
        projectId: String,
        start: Instant,
        end: Instant,
        applicationId: String?,
    ): JourneyStats {
        val projUuid = UUID.fromString(projectId)

        return transaction {
            var condition: Op<Boolean> =
                (AnalyticsEventTable.projectId eq projUuid) and
                    (AnalyticsEventTable.eventKey eq "screen_view") and
                    (AnalyticsEventTable.occurredAt greaterEq start) and
                    (AnalyticsEventTable.occurredAt lessEq end)

            applicationId?.let {
                condition = condition and (AnalyticsEventTable.applicationId eq UUID.fromString(it))
            }

            val events =
                AnalyticsEventTable
                    .selectAll()
                    .where { condition }
                    .orderBy(AnalyticsEventTable.occurredAt)
                    .map {
                        Triple(
                            it[AnalyticsEventTable.sessionId].toString(),
                            extractJsonField(it[AnalyticsEventTable.properties], "screen_id") ?: "unknown",
                            it[AnalyticsEventTable.occurredAt],
                        )
                    }

            val transitionCounts = mutableMapOf<Pair<String, String>, Long>()
            val bySession = events.groupBy { it.first }

            bySession.values.forEach { sessionEvents ->
                val sorted = sessionEvents.sortedBy { it.third }
                for (i in 0 until sorted.size - 1) {
                    val from = sorted[i].second
                    val to = sorted[i + 1].second
                    if (from != to) {
                        val key = Pair(from, to)
                        transitionCounts[key] = (transitionCounts[key] ?: 0) + 1
                    }
                }
            }

            val transitions =
                transitionCounts
                    .map { (key, count) ->
                        TransitionEntry(fromScreen = key.first, toScreen = key.second, count = count)
                    }.sortedByDescending { it.count }

            JourneyStats(transitions = transitions)
        }
    }

    @Suppress("LongMethod")
    fun funnel(
        projectId: String,
        request: FunnelRequest,
    ): FunnelResponse {
        val projUuid = UUID.fromString(projectId)
        val start = Instant.parse(request.startDate)
        val end = Instant.parse(request.endDate)

        return transaction {
            var baseCondition: Op<Boolean> =
                (AnalyticsEventTable.projectId eq projUuid) and
                    (AnalyticsEventTable.occurredAt greaterEq start) and
                    (AnalyticsEventTable.occurredAt lessEq end)

            request.applicationId?.let {
                baseCondition = baseCondition and (AnalyticsEventTable.applicationId eq UUID.fromString(it))
            }

            val allEvents =
                AnalyticsEventTable
                    .selectAll()
                    .where { baseCondition }
                    .map { row ->
                        FunnelEvent(
                            sessionId = row[AnalyticsEventTable.sessionId].toString(),
                            anonymousId = row[AnalyticsEventTable.anonymousId].toString(),
                            eventKey = row[AnalyticsEventTable.eventKey],
                            properties = row[AnalyticsEventTable.properties],
                            occurredAt = row[AnalyticsEventTable.occurredAt],
                        )
                    }

            val bySession = allEvents.groupBy { it.sessionId }
            val steps = mutableListOf<FunnelStepResult>()
            var remainingSessions = bySession.keys.toSet()
            var remainingUsers =
                bySession.values
                    .flatten()
                    .map { it.anonymousId }
                    .toSet()

            for ((index, step) in request.steps.withIndex()) {
                val matchingSessions = mutableSetOf<String>()
                val matchingUsers = mutableSetOf<String>()

                for (sessionId in remainingSessions) {
                    val sessionEvents = bySession[sessionId] ?: continue
                    val matched =
                        sessionEvents.any { evt ->
                            evt.eventKey == step.eventKey && matchesFilter(evt.properties, step.filter)
                        }
                    if (matched) {
                        matchingSessions.add(sessionId)
                        sessionEvents.forEach { matchingUsers.add(it.anonymousId) }
                    }
                }

                val conversionRate =
                    if (index == 0) {
                        1.0
                    } else {
                        val prevUsers = steps[index - 1].uniqueUsers
                        if (prevUsers > 0) matchingUsers.size.toDouble() / prevUsers else 0.0
                    }

                steps.add(
                    FunnelStepResult(
                        step = index + 1,
                        eventKey = step.eventKey,
                        uniqueUsers = matchingUsers.size.toLong(),
                        uniqueSessions = matchingSessions.size.toLong(),
                        conversionRate = conversionRate,
                    ),
                )

                remainingSessions = matchingSessions
                remainingUsers = matchingUsers
            }

            val overallRate =
                if (steps.isNotEmpty() && steps.first().uniqueUsers > 0) {
                    steps.last().uniqueUsers.toDouble() / steps.first().uniqueUsers
                } else {
                    0.0
                }

            FunnelResponse(
                steps = steps,
                overallConversionRate = overallRate,
                scope = request.scope,
            )
        }
    }

    fun eventSummary(
        projectId: String,
        start: Instant,
        end: Instant,
        applicationId: String?,
    ): EventSummaryResponse {
        val projUuid = UUID.fromString(projectId)

        return transaction {
            var condition: Op<Boolean> =
                (AnalyticsEventTable.projectId eq projUuid) and
                    (AnalyticsEventTable.occurredAt greaterEq start) and
                    (AnalyticsEventTable.occurredAt lessEq end)

            applicationId?.let {
                condition = condition and (AnalyticsEventTable.applicationId eq UUID.fromString(it))
            }

            val rows =
                AnalyticsEventTable
                    .selectAll()
                    .where { condition }
                    .map {
                        Triple(
                            it[AnalyticsEventTable.eventKey],
                            it[AnalyticsEventTable.anonymousId].toString(),
                            it[AnalyticsEventTable.sessionId].toString(),
                        )
                    }

            val grouped = rows.groupBy { it.first }
            val events =
                grouped
                    .map { (eventKey, entries) ->
                        EventSummaryEntry(
                            eventKey = eventKey,
                            count = entries.size.toLong(),
                            uniqueUsers =
                                entries
                                    .map { it.second }
                                    .toSet()
                                    .size
                                    .toLong(),
                            uniqueSessions =
                                entries
                                    .map { it.third }
                                    .toSet()
                                    .size
                                    .toLong(),
                        )
                    }.sortedByDescending { it.count }

            EventSummaryResponse(
                events = events,
                period = PeriodInfo(start.toString(), end.toString()),
            )
        }
    }

    fun eventBreakdown(
        projectId: String,
        start: Instant,
        end: Instant,
        eventKey: String,
        propertyKey: String,
        applicationId: String?,
    ): EventBreakdownResponse {
        val projUuid = UUID.fromString(projectId)

        return transaction {
            var condition: Op<Boolean> =
                (AnalyticsEventTable.projectId eq projUuid) and
                    (AnalyticsEventTable.eventKey eq eventKey) and
                    (AnalyticsEventTable.occurredAt greaterEq start) and
                    (AnalyticsEventTable.occurredAt lessEq end)

            applicationId?.let {
                condition = condition and (AnalyticsEventTable.applicationId eq UUID.fromString(it))
            }

            val rows =
                AnalyticsEventTable
                    .selectAll()
                    .where { condition }
                    .mapNotNull {
                        val value =
                            extractJsonField(it[AnalyticsEventTable.properties], propertyKey)
                                ?: return@mapNotNull null
                        Pair(value, it[AnalyticsEventTable.anonymousId].toString())
                    }

            val grouped = rows.groupBy { it.first }
            val breakdown =
                grouped
                    .map { (value, entries) ->
                        BreakdownEntry(
                            value = value,
                            count = entries.size.toLong(),
                            uniqueUsers =
                                entries
                                    .map { it.second }
                                    .toSet()
                                    .size
                                    .toLong(),
                        )
                    }.sortedByDescending { it.count }

            EventBreakdownResponse(
                eventKey = eventKey,
                propertyKey = propertyKey,
                breakdown = breakdown,
                period = PeriodInfo(start.toString(), end.toString()),
            )
        }
    }

    @Suppress("LongMethod")
    fun retention(
        projectId: String,
        start: Instant,
        end: Instant,
        retentionDays: List<Int>,
        type: String,
        applicationId: String?,
    ): RetentionResponse {
        val projUuid = UUID.fromString(projectId)

        return transaction {
            var baseCondition: Op<Boolean> =
                (AnalyticsEventTable.projectId eq projUuid)

            applicationId?.let {
                baseCondition = baseCondition and (AnalyticsEventTable.applicationId eq UUID.fromString(it))
            }

            // 사용자별 활동 날짜 목록 수집
            val allActivity = mutableMapOf<String, MutableSet<String>>()
            AnalyticsEventTable
                .select(AnalyticsEventTable.anonymousId, AnalyticsEventTable.occurredAt)
                .where { baseCondition }
                .forEach {
                    val userId = it[AnalyticsEventTable.anonymousId].toString()
                    val dateStr = it[AnalyticsEventTable.occurredAt].toString().substring(0, 10)
                    allActivity.getOrPut(userId) { mutableSetOf() }.add(dateStr)
                }

            // 코호트 결정
            val startDate = start.toString().substring(0, 10)
            val endDate = end.toString().substring(0, 10)

            val cohortUsers: Map<String, Set<String>> =
                if (type == "new") {
                    // 신규: 첫 활동일이 기간 내인 사용자
                    val userFirstDate = allActivity.mapValues { (_, dates) -> dates.min() }
                    userFirstDate
                        .filter { (_, firstDate) -> firstDate in startDate..endDate }
                        .entries
                        .groupBy({ it.value }, { it.key })
                        .mapValues { (_, users) -> users.toSet() }
                } else {
                    // 활성: 해당 날짜에 활동한 모든 사용자
                    val result = mutableMapOf<String, MutableSet<String>>()
                    allActivity.forEach { (userId, dates) ->
                        dates.filter { it in startDate..endDate }.forEach { date ->
                            result.getOrPut(date) { mutableSetOf() }.add(userId)
                        }
                    }
                    result
                }

            // 날짜 목록 생성
            val dates = generateDateRange(startDate, endDate)
            val today =
                kotlinx.datetime.Clock.System
                    .now()
                    .toString()
                    .substring(0, 10)

            val cohorts =
                dates.map { cohortDate ->
                    val users = cohortUsers[cohortDate] ?: emptySet()
                    val retentionMap = mutableMapOf<String, RetentionValue>()

                    for (day in retentionDays) {
                        val targetDate = addDays(cohortDate, day)
                        // 미래 날짜는 생략
                        if (targetDate > today) continue

                        val retainedCount =
                            users.count { userId ->
                                allActivity[userId]?.contains(targetDate) == true
                            }
                        val rate =
                            if (users.isNotEmpty()) {
                                (retainedCount.toDouble() / users.size * 10000).toLong() / 10000.0
                            } else {
                                0.0
                            }
                        retentionMap[day.toString()] = RetentionValue(retainedCount.toLong(), rate)
                    }

                    CohortEntry(
                        date = cohortDate,
                        cohortSize = users.size.toLong(),
                        retention = retentionMap,
                    )
                }

            RetentionResponse(
                type = type,
                retentionDays = retentionDays,
                cohorts = cohorts,
                period = PeriodInfo(start.toString(), end.toString()),
            )
        }
    }

    private fun generateDateRange(
        start: String,
        end: String,
    ): List<String> {
        val dates = mutableListOf<String>()
        var current = java.time.LocalDate.parse(start)
        val endDate = java.time.LocalDate.parse(end)
        while (!current.isAfter(endDate)) {
            dates.add(current.toString())
            current = current.plusDays(1)
        }
        return dates
    }

    private fun addDays(
        dateStr: String,
        days: Int,
    ): String {
        val date = java.time.LocalDate.parse(dateStr)
        return date.plusDays(days.toLong()).toString()
    }

    private fun extractJsonField(
        json: String,
        field: String,
    ): String? {
        // 문자열 값: "field": "value"
        val stringRegex = """"$field"\s*:\s*"([^"]+)"""".toRegex()
        stringRegex
            .find(json)
            ?.groupValues
            ?.get(1)
            ?.let { return it }
        // 숫자/불리언 값: "field": 123 또는 "field": true
        val nonStringRegex = """"$field"\s*:\s*([^",}\s]+)""".toRegex()
        return nonStringRegex.find(json)?.groupValues?.get(1)
    }

    private fun matchesFilter(
        properties: String,
        filter: Map<String, String>?,
    ): Boolean {
        if (filter.isNullOrEmpty()) return true
        return filter.all { (key, value) -> extractJsonField(properties, key) == value }
    }
}

private data class FunnelEvent(
    val sessionId: String,
    val anonymousId: String,
    val eventKey: String,
    val properties: String,
    val occurredAt: kotlinx.datetime.Instant,
)
