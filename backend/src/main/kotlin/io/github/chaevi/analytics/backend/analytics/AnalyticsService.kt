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
            var condition: Op<Boolean> =
                (AnalyticsEventTable.projectId eq projUuid) and
                    (AnalyticsEventTable.eventKey eq "screen_view") and
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
                        val props = it[AnalyticsEventTable.properties]
                        val screenId = extractJsonField(props, "screen_id") ?: "unknown"
                        Pair(screenId, it[AnalyticsEventTable.anonymousId].toString())
                    }

            val grouped = rows.groupBy { it.first }
            val screens =
                grouped
                    .map { (screenId, entries) ->
                        ScreenEntry(
                            screenId = screenId,
                            viewCount = entries.size.toLong(),
                            uniqueUsers =
                                entries
                                    .map { it.second }
                                    .toSet()
                                    .size
                                    .toLong(),
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

    private fun extractJsonField(
        json: String,
        field: String,
    ): String? {
        val regex = """"$field"\s*:\s*"([^"]+)"""".toRegex()
        return regex.find(json)?.groupValues?.get(1)
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
