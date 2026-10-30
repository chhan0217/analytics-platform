package io.github.chaevi.analytics.backend.ingest

import io.github.chaevi.analytics.backend.common.ApiException
import io.github.chaevi.analytics.backend.common.parseEndInstant
import io.github.chaevi.analytics.backend.common.parseStartInstant
import io.ktor.http.HttpStatusCode
import kotlinx.datetime.Instant
import org.jetbrains.exposed.sql.Op
import org.jetbrains.exposed.sql.ResultRow
import org.jetbrains.exposed.sql.SortOrder
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.SqlExpressionBuilder.greaterEq
import org.jetbrains.exposed.sql.SqlExpressionBuilder.less
import org.jetbrains.exposed.sql.SqlExpressionBuilder.lessEq
import org.jetbrains.exposed.sql.and
import org.jetbrains.exposed.sql.or
import org.jetbrains.exposed.sql.selectAll
import org.jetbrains.exposed.sql.transactions.transaction
import java.util.Base64
import java.util.UUID

class EventQueryService {
    @Suppress("LongParameterList", "UnusedParameter")
    fun findEvents(
        projectId: String,
        startDate: String,
        endDate: String,
        platform: String?,
        applicationId: String?,
        eventKey: String?,
        userId: String?,
        sessionId: String?,
        cursor: String?,
        pageSize: Int,
    ): CursorListResponse<EventResponse> {
        val projUuid = parseUuid(projectId)
        val start = parseStartInstant(startDate)
        val end = parseEndInstant(endDate)
        val limit = pageSize.coerceIn(1, 200)

        return transaction {
            var condition: Op<Boolean> =
                (AnalyticsEventTable.projectId eq projUuid) and
                    (AnalyticsEventTable.occurredAt greaterEq start) and
                    (AnalyticsEventTable.occurredAt lessEq end)

            applicationId?.let {
                condition = condition and (AnalyticsEventTable.applicationId eq parseUuid(it))
            }
            eventKey?.let {
                condition = condition and (AnalyticsEventTable.eventKey eq it)
            }
            userId?.let {
                condition = condition and (AnalyticsEventTable.analyticsUserId eq parseUuid(it))
            }
            sessionId?.let {
                condition = condition and (AnalyticsEventTable.sessionId eq parseUuid(it))
            }
            cursor?.let {
                val (cursorTime, cursorId) = decodeCursor(it)
                condition =
                    condition and (
                        (AnalyticsEventTable.occurredAt less cursorTime) or
                            ((AnalyticsEventTable.occurredAt eq cursorTime) and (AnalyticsEventTable.id less cursorId))
                    )
            }

            val rows =
                AnalyticsEventTable
                    .selectAll()
                    .where { condition }
                    .orderBy(AnalyticsEventTable.occurredAt, SortOrder.DESC)
                    .orderBy(AnalyticsEventTable.id, SortOrder.DESC)
                    .limit(limit + 1)
                    .map { it.toEventResponse() }

            val hasMore = rows.size > limit
            val data = if (hasMore) rows.take(limit) else rows
            val nextCursor =
                if (hasMore) {
                    val last = data.last()
                    encodeCursor(last.occurredAt, last.id)
                } else {
                    null
                }

            CursorListResponse(
                data = data,
                meta = CursorMeta(nextCursor = nextCursor, hasMore = hasMore),
            )
        }
    }

    fun findEventById(
        projectId: String,
        eventId: String,
    ): EventResponse {
        val projUuid = parseUuid(projectId)
        val eventUuid = parseUuid(eventId)

        return transaction {
            AnalyticsEventTable
                .selectAll()
                .where {
                    (AnalyticsEventTable.id eq eventUuid) and
                        (AnalyticsEventTable.projectId eq projUuid)
                }.singleOrNull()
                ?.toEventResponse()
        } ?: throw ApiException("NOT_FOUND", HttpStatusCode.NotFound, "이벤트를 찾을 수 없습니다: $eventId")
    }

    private fun encodeCursor(
        occurredAt: String,
        id: String,
    ): String {
        val raw = "$occurredAt|$id"
        return Base64.getUrlEncoder().withoutPadding().encodeToString(raw.toByteArray())
    }

    private fun decodeCursor(cursor: String): Pair<Instant, UUID> =
        try {
            val decoded = String(Base64.getUrlDecoder().decode(cursor))
            val parts = decoded.split("|")
            Pair(Instant.parse(parts[0]), UUID.fromString(parts[1]))
        } catch (e: Exception) {
            throw ApiException("VALIDATION_ERROR", HttpStatusCode.BadRequest, "유효하지 않은 커서입니다")
        }

    private fun parseUuid(id: String): UUID =
        try {
            UUID.fromString(id)
        } catch (e: IllegalArgumentException) {
            throw ApiException("VALIDATION_ERROR", HttpStatusCode.BadRequest, "유효하지 않은 ID 형식입니다: $id")
        }

    private fun ResultRow.toEventResponse(): EventResponse =
        EventResponse(
            id = this[AnalyticsEventTable.id].value.toString(),
            projectId = this[AnalyticsEventTable.projectId].value.toString(),
            applicationId = this[AnalyticsEventTable.applicationId].value.toString(),
            sessionId = this[AnalyticsEventTable.sessionId].toString(),
            analyticsUserId = this[AnalyticsEventTable.analyticsUserId]?.value?.toString(),
            anonymousId = this[AnalyticsEventTable.anonymousId].toString(),
            eventKey = this[AnalyticsEventTable.eventKey],
            eventSchemaVersion = this[AnalyticsEventTable.eventSchemaVersion],
            properties = this[AnalyticsEventTable.properties],
            context = this[AnalyticsEventTable.context],
            occurredAt = this[AnalyticsEventTable.occurredAt].toString(),
            receivedAt = this[AnalyticsEventTable.receivedAt].toString(),
            sequenceNumber = this[AnalyticsEventTable.sequenceNumber],
        )
}
