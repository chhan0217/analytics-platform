package io.github.chaevi.analytics.backend.user

import io.github.chaevi.analytics.backend.common.ListResponse
import io.github.chaevi.analytics.backend.common.PageMeta
import io.github.chaevi.analytics.backend.ingest.AnalyticsEventTable
import io.github.chaevi.analytics.backend.ingest.AnalyticsSessionTable
import io.github.chaevi.analytics.backend.ingest.AnalyticsUserTable
import io.github.chaevi.analytics.backend.ingest.AnonymousIdentityTable
import io.ktor.server.response.respond
import io.ktor.server.routing.Route
import io.ktor.server.routing.get
import io.ktor.server.routing.route
import kotlinx.serialization.Serializable
import org.jetbrains.exposed.sql.SortOrder
import org.jetbrains.exposed.sql.and
import org.jetbrains.exposed.sql.or
import org.jetbrains.exposed.sql.selectAll
import org.jetbrains.exposed.sql.transactions.transaction
import java.util.UUID

@Serializable
data class UserResponse(
    val id: String,
    val projectId: String,
    val externalUserId: String?,
    val createdAt: String,
)

@Serializable
data class SessionResponse(
    val id: String,
    val applicationId: String,
    val anonymousId: String,
    val startedAt: String,
    val lastEventAt: String,
    val endedAt: String?,
)

@Serializable
data class SessionEventResponse(
    val id: String,
    val eventKey: String,
    val properties: String,
    val occurredAt: String,
    val sequenceNumber: Int?,
)

@Suppress("LongMethod")
fun Route.userQueryRoutes() {
    route("/projects/{projectId}") {
        get("/users") {
            val projectId = call.parameters["projectId"]!!
            val externalUserId = call.request.queryParameters["external_user_id"]
            val anonymousId = call.request.queryParameters["anonymous_id"]
            val projUuid = UUID.fromString(projectId)

            val users =
                transaction {
                    if (externalUserId != null) {
                        AnalyticsUserTable
                            .selectAll()
                            .where {
                                (AnalyticsUserTable.projectId eq projUuid) and
                                    (AnalyticsUserTable.externalUserId eq externalUserId)
                            }.map {
                                UserResponse(
                                    id = it[AnalyticsUserTable.id].value.toString(),
                                    projectId = it[AnalyticsUserTable.projectId].value.toString(),
                                    externalUserId = it[AnalyticsUserTable.externalUserId],
                                    createdAt = it[AnalyticsUserTable.createdAt].toString(),
                                )
                            }
                    } else if (anonymousId != null) {
                        val anonUuid = UUID.fromString(anonymousId)
                        AnonymousIdentityTable
                            .selectAll()
                            .where {
                                (AnonymousIdentityTable.projectId eq projUuid) and
                                    (AnonymousIdentityTable.anonymousId eq anonUuid)
                            }.map {
                                UserResponse(
                                    id = it[AnonymousIdentityTable.id].value.toString(),
                                    projectId = it[AnonymousIdentityTable.projectId].value.toString(),
                                    externalUserId = null,
                                    createdAt = it[AnonymousIdentityTable.createdAt].toString(),
                                )
                            }
                    } else {
                        emptyList()
                    }
                }

            call.respond(
                ListResponse(
                    data = users,
                    meta = PageMeta(total = users.size.toLong(), page = 1, pageSize = users.size),
                ),
            )
        }

        get("/users/{userId}/sessions") {
            val userId = call.parameters["userId"]!!
            val userUuid = UUID.fromString(userId)

            val sessions =
                transaction {
                    AnalyticsSessionTable
                        .selectAll()
                        .where {
                            (AnalyticsSessionTable.analyticsUserId eq userUuid) or
                                (AnalyticsSessionTable.anonymousId eq userUuid)
                        }.orderBy(AnalyticsSessionTable.startedAt, SortOrder.DESC)
                        .map {
                            SessionResponse(
                                id = it[AnalyticsSessionTable.id].value.toString(),
                                applicationId = it[AnalyticsSessionTable.applicationId].value.toString(),
                                anonymousId = it[AnalyticsSessionTable.anonymousId].toString(),
                                startedAt = it[AnalyticsSessionTable.startedAt].toString(),
                                lastEventAt = it[AnalyticsSessionTable.lastEventAt].toString(),
                                endedAt = it[AnalyticsSessionTable.endedAt]?.toString(),
                            )
                        }
                }

            call.respond(
                ListResponse(
                    data = sessions,
                    meta = PageMeta(total = sessions.size.toLong(), page = 1, pageSize = sessions.size),
                ),
            )
        }

        get("/sessions/{sessionId}/events") {
            val sessionId = call.parameters["sessionId"]!!
            val sessionUuid = UUID.fromString(sessionId)

            val events =
                transaction {
                    AnalyticsEventTable
                        .selectAll()
                        .where { AnalyticsEventTable.sessionId eq sessionUuid }
                        .orderBy(AnalyticsEventTable.occurredAt, SortOrder.ASC)
                        .map {
                            SessionEventResponse(
                                id = it[AnalyticsEventTable.id].value.toString(),
                                eventKey = it[AnalyticsEventTable.eventKey],
                                properties = it[AnalyticsEventTable.properties],
                                occurredAt = it[AnalyticsEventTable.occurredAt].toString(),
                                sequenceNumber = it[AnalyticsEventTable.sequenceNumber],
                            )
                        }
                }

            call.respond(
                ListResponse(
                    data = events,
                    meta = PageMeta(total = events.size.toLong(), page = 1, pageSize = events.size),
                ),
            )
        }
    }
}
