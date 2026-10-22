package io.github.chaevi.analytics.backend.ingest

import io.github.chaevi.analytics.backend.common.DataResponse
import io.ktor.server.response.respond
import io.ktor.server.routing.Route
import io.ktor.server.routing.get
import io.ktor.server.routing.route

fun Route.eventQueryRoutes(service: EventQueryService = EventQueryService()) {
    route("/projects/{projectId}/events") {
        get {
            val projectId = call.parameters["projectId"]!!
            val startDate =
                call.request.queryParameters["start_date"]
                    ?: return@get call.respond(
                        io.ktor.http.HttpStatusCode.BadRequest,
                        mapOf("error" to "start_date는 필수입니다"),
                    )
            val endDate =
                call.request.queryParameters["end_date"]
                    ?: return@get call.respond(
                        io.ktor.http.HttpStatusCode.BadRequest,
                        mapOf("error" to "end_date는 필수입니다"),
                    )

            val platform = call.request.queryParameters["platform"]
            val applicationId = call.request.queryParameters["application_id"]
            val eventKey = call.request.queryParameters["event_key"]
            val userId = call.request.queryParameters["user_id"]
            val sessionId = call.request.queryParameters["session_id"]
            val cursor = call.request.queryParameters["cursor"]
            val pageSize = call.request.queryParameters["page_size"]?.toIntOrNull() ?: 50

            val response =
                service.findEvents(
                    projectId = projectId,
                    startDate = startDate,
                    endDate = endDate,
                    platform = platform,
                    applicationId = applicationId,
                    eventKey = eventKey,
                    userId = userId,
                    sessionId = sessionId,
                    cursor = cursor,
                    pageSize = pageSize,
                )

            call.respond(response)
        }

        get("/{eventId}") {
            val projectId = call.parameters["projectId"]!!
            val eventId = call.parameters["eventId"]!!
            val response = service.findEventById(projectId, eventId)
            call.respond(DataResponse(response))
        }
    }
}
