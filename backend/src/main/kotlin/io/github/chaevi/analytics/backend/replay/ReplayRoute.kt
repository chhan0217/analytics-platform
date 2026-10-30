package io.github.chaevi.analytics.backend.replay

import io.github.chaevi.analytics.backend.common.ApiException
import io.github.chaevi.analytics.backend.common.DataResponse
import io.github.chaevi.analytics.backend.common.ListResponse
import io.github.chaevi.analytics.backend.common.PageMeta
import io.github.chaevi.analytics.backend.common.parseEndInstant
import io.github.chaevi.analytics.backend.common.parseStartInstant
import io.ktor.http.HttpStatusCode
import io.ktor.server.request.receive
import io.ktor.server.response.respond
import io.ktor.server.routing.Route
import io.ktor.server.routing.delete
import io.ktor.server.routing.get
import io.ktor.server.routing.patch
import io.ktor.server.routing.post
import io.ktor.server.routing.route
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonArray
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.deleteWhere
import org.jetbrains.exposed.sql.insertAndGetId
import org.jetbrains.exposed.sql.selectAll
import org.jetbrains.exposed.sql.transactions.transaction
import org.jetbrains.exposed.sql.update
import java.util.UUID

@Suppress("ThrowsCount", "LongMethod")
fun Route.replayRoutes(service: ReplayService = ReplayService()) {
    route("/projects/{projectId}/sessions") {
        get {
            val projectId = call.requireProjectId()
            val startDate =
                call.request.queryParameters["start_date"]
                    ?: throw ApiException(
                        "VALIDATION_ERROR",
                        HttpStatusCode.BadRequest,
                        "start_date는 필수입니다",
                    )
            val endDate =
                call.request.queryParameters["end_date"]
                    ?: throw ApiException(
                        "VALIDATION_ERROR",
                        HttpStatusCode.BadRequest,
                        "end_date는 필수입니다",
                    )
            val applicationId = call.request.queryParameters["application_id"]
            val page = call.request.queryParameters["page"]?.toIntOrNull() ?: 1
            val pageSize = call.request.queryParameters["size"]?.toIntOrNull() ?: 20

            val start = parseStartInstant(startDate)
            val end = parseEndInstant(endDate)

            val (sessions, total) =
                service.sessionList(
                    projectId,
                    start,
                    end,
                    applicationId,
                    page,
                    pageSize,
                )

            call.respond(
                ListResponse(
                    data = sessions,
                    meta = PageMeta(total = total, page = page, pageSize = pageSize),
                ),
            )
        }

        get("/{sessionId}/replay") {
            val projectId = call.requireProjectId()
            val sessionId =
                call.parameters["sessionId"]
                    ?: throw ApiException(
                        "VALIDATION_ERROR",
                        HttpStatusCode.BadRequest,
                        "sessionId는 필수입니다",
                    )

            val response = service.sessionReplay(projectId, sessionId)
            call.respond(DataResponse(response))
        }
    }

    route("/projects/{projectId}/screen-templates") {
        get {
            val projectId = call.requireProjectId()
            val projUuid = UUID.fromString(projectId)

            val screens =
                transaction {
                    ScreenTemplateTable
                        .selectAll()
                        .where { ScreenTemplateTable.projectId eq projUuid }
                        .orderBy(ScreenTemplateTable.screenId)
                        .map { row ->
                            val wireframe = row[ScreenTemplateTable.wireframe]
                            val nodeCount =
                                try {
                                    Json.parseToJsonElement(wireframe).jsonArray.size
                                } catch (_: Exception) {
                                    0
                                }

                            AppScreenEntry(
                                id = row[ScreenTemplateTable.id].value.toString(),
                                screenId = row[ScreenTemplateTable.screenId],
                                appVersion = row[ScreenTemplateTable.appVersion],
                                viewportWidth = row[ScreenTemplateTable.viewportWidth],
                                viewportHeight = row[ScreenTemplateTable.viewportHeight],
                                nodeCount = nodeCount,
                                hasScreenshot = row[ScreenTemplateTable.screenshot] != null,
                            )
                        }
                }

            call.respond(DataResponse(screens))
        }

        patch("/{templateId}/screenshot") {
            val templateId =
                call.parameters["templateId"]
                    ?: throw ApiException(
                        "VALIDATION_ERROR",
                        HttpStatusCode.BadRequest,
                        "templateId는 필수입니다",
                    )

            val request = call.receive<ScreenshotUploadRequest>()

            transaction {
                ScreenTemplateTable.update(
                    { ScreenTemplateTable.id eq UUID.fromString(templateId) },
                ) {
                    it[screenshot] = request.screenshot
                }
            }

            call.respond(DataResponse(mapOf("success" to true)))
        }

        get("/{templateId}/screenshot") {
            val templateId =
                call.parameters["templateId"]
                    ?: throw ApiException(
                        "VALIDATION_ERROR",
                        HttpStatusCode.BadRequest,
                        "templateId는 필수입니다",
                    )

            val screenshot =
                transaction {
                    ScreenTemplateTable
                        .selectAll()
                        .where { ScreenTemplateTable.id eq UUID.fromString(templateId) }
                        .firstOrNull()
                        ?.get(ScreenTemplateTable.screenshot)
                } ?: throw ApiException("NOT_FOUND", HttpStatusCode.NotFound, "스크린샷이 없습니다")

            call.respond(DataResponse(mapOf("screenshot" to screenshot)))
        }
    }

    route("/projects/{projectId}/screen-layouts") {
        get {
            val projectId = call.requireProjectId()
            val projUuid = UUID.fromString(projectId)

            val layouts =
                transaction {
                    ScreenLayoutTable
                        .selectAll()
                        .where { ScreenLayoutTable.projectId eq projUuid }
                        .orderBy(ScreenLayoutTable.screenId)
                        .map { row ->
                            ScreenLayoutEntry(
                                id = row[ScreenLayoutTable.id].value.toString(),
                                screenId = row[ScreenLayoutTable.screenId],
                                displayName = row[ScreenLayoutTable.displayName],
                                createdAt = row[ScreenLayoutTable.createdAt].toString(),
                                updatedAt = row[ScreenLayoutTable.updatedAt].toString(),
                            )
                        }
                }

            call.respond(DataResponse(layouts))
        }

        post {
            val projectId = call.requireProjectId()
            val projUuid = UUID.fromString(projectId)
            val request = call.receive<CreateScreenLayoutRequest>()

            val now = java.time.OffsetDateTime.now()
            val created =
                transaction {
                    ScreenLayoutTable.insertAndGetId {
                        it[ScreenLayoutTable.projectId] = projUuid
                        it[screenId] = request.screenId
                        it[displayName] = request.displayName
                        it[layoutJson] = request.layoutJson
                        it[createdAt] = now
                        it[updatedAt] = now
                    }
                }

            call.respond(
                io.ktor.http.HttpStatusCode.Created,
                DataResponse(mapOf("id" to created.value.toString())),
            )
        }

        get("/{layoutId}") {
            val layoutId =
                call.parameters["layoutId"]
                    ?: throw ApiException("VALIDATION_ERROR", HttpStatusCode.BadRequest, "layoutId는 필수입니다")

            val layout =
                transaction {
                    ScreenLayoutTable
                        .selectAll()
                        .where { ScreenLayoutTable.id eq UUID.fromString(layoutId) }
                        .firstOrNull()
                } ?: throw ApiException("NOT_FOUND", HttpStatusCode.NotFound, "레이아웃을 찾을 수 없습니다")

            call.respond(
                DataResponse(
                    ScreenLayoutDetail(
                        id = layout[ScreenLayoutTable.id].value.toString(),
                        screenId = layout[ScreenLayoutTable.screenId],
                        displayName = layout[ScreenLayoutTable.displayName],
                        layoutJson = layout[ScreenLayoutTable.layoutJson],
                        createdAt = layout[ScreenLayoutTable.createdAt].toString(),
                        updatedAt = layout[ScreenLayoutTable.updatedAt].toString(),
                    ),
                ),
            )
        }

        patch("/{layoutId}") {
            val layoutId =
                call.parameters["layoutId"]
                    ?: throw ApiException("VALIDATION_ERROR", HttpStatusCode.BadRequest, "layoutId는 필수입니다")

            val request = call.receive<UpdateScreenLayoutRequest>()
            val now = java.time.OffsetDateTime.now()

            transaction {
                ScreenLayoutTable.update(
                    { ScreenLayoutTable.id eq UUID.fromString(layoutId) },
                ) {
                    request.displayName?.let { name -> it[displayName] = name }
                    request.layoutJson?.let { json -> it[layoutJson] = json }
                    it[updatedAt] = now
                }
            }

            call.respond(DataResponse(mapOf("success" to true)))
        }

        delete("/{layoutId}") {
            val layoutId =
                call.parameters["layoutId"]
                    ?: throw ApiException("VALIDATION_ERROR", HttpStatusCode.BadRequest, "layoutId는 필수입니다")

            transaction {
                ScreenLayoutTable.deleteWhere {
                    ScreenLayoutTable.id eq UUID.fromString(layoutId)
                }
            }

            call.respond(DataResponse(mapOf("success" to true)))
        }
    }
}

private fun io.ktor.server.routing.RoutingCall.requireProjectId(): String =
    parameters["projectId"]
        ?: throw ApiException("VALIDATION_ERROR", HttpStatusCode.BadRequest, "projectId는 필수입니다")
