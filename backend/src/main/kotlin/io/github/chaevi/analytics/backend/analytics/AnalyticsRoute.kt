package io.github.chaevi.analytics.backend.analytics

import io.github.chaevi.analytics.backend.common.ApiException
import io.github.chaevi.analytics.backend.common.DataResponse
import io.ktor.http.HttpStatusCode
import io.ktor.server.request.receive
import io.ktor.server.response.respond
import io.ktor.server.routing.Route
import io.ktor.server.routing.get
import io.ktor.server.routing.post
import io.ktor.server.routing.route
import kotlinx.datetime.Instant

fun Route.analyticsRoutes(service: AnalyticsService = AnalyticsService()) {
    route("/projects/{projectId}/analytics") {
        get("/overview") {
            val projectId = call.requireProjectId()
            val (start, end) = parseDateRange(call)
            val applicationId = call.request.queryParameters["application_id"]
            val response = service.overview(projectId, start, end, applicationId)
            call.respond(DataResponse(response))
        }

        get("/screens") {
            val projectId = call.requireProjectId()
            val (start, end) = parseDateRange(call)
            val applicationId = call.request.queryParameters["application_id"]
            val response = service.screens(projectId, start, end, applicationId)
            call.respond(DataResponse(response))
        }

        get("/journeys") {
            val projectId = call.requireProjectId()
            val (start, end) = parseDateRange(call)
            val applicationId = call.request.queryParameters["application_id"]
            val response = service.journeys(projectId, start, end, applicationId)
            call.respond(DataResponse(response))
        }

        post("/funnels/query") {
            val projectId = call.requireProjectId()
            val request = call.receive<FunnelRequest>()
            val response = service.funnel(projectId, request)
            call.respond(DataResponse(response))
        }

        get("/events/summary") {
            val projectId = call.requireProjectId()
            val (start, end) = parseDateRange(call)
            val applicationId = call.request.queryParameters["application_id"]
            val response = service.eventSummary(projectId, start, end, applicationId)
            call.respond(DataResponse(response))
        }

        get("/events/breakdown") {
            val projectId = call.requireProjectId()
            val (start, end) = parseDateRange(call)
            val eventKey =
                call.request.queryParameters["event_key"]
                    ?: throw ApiException("VALIDATION_ERROR", HttpStatusCode.BadRequest, "event_key는 필수입니다")
            val propertyKey =
                call.request.queryParameters["property_key"]
                    ?: throw ApiException("VALIDATION_ERROR", HttpStatusCode.BadRequest, "property_key는 필수입니다")
            val applicationId = call.request.queryParameters["application_id"]
            val response = service.eventBreakdown(projectId, start, end, eventKey, propertyKey, applicationId)
            call.respond(DataResponse(response))
        }
    }
}

private fun io.ktor.server.routing.RoutingCall.requireProjectId(): String =
    parameters["projectId"]
        ?: throw ApiException("VALIDATION_ERROR", HttpStatusCode.BadRequest, "projectId는 필수입니다")

private fun parseDateRange(call: io.ktor.server.routing.RoutingCall): Pair<Instant, Instant> {
    val startDate =
        call.request.queryParameters["start_date"]
            ?: throw ApiException("VALIDATION_ERROR", HttpStatusCode.BadRequest, "start_date는 필수입니다")
    val endDate =
        call.request.queryParameters["end_date"]
            ?: throw ApiException("VALIDATION_ERROR", HttpStatusCode.BadRequest, "end_date는 필수입니다")
    return Pair(parseInstant(startDate), parseInstant(endDate))
}

private fun parseInstant(value: String): Instant =
    if (value.contains("T")) {
        Instant.parse(value)
    } else {
        Instant.parse("${value}T00:00:00Z")
    }
