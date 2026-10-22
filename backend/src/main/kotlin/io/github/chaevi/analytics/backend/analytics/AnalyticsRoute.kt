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
            val projectId = call.parameters["projectId"]!!
            val (start, end) = parseDateRange(call)
            val applicationId = call.request.queryParameters["application_id"]
            val response = service.overview(projectId, start, end, applicationId)
            call.respond(DataResponse(response))
        }

        get("/screens") {
            val projectId = call.parameters["projectId"]!!
            val (start, end) = parseDateRange(call)
            val applicationId = call.request.queryParameters["application_id"]
            val response = service.screens(projectId, start, end, applicationId)
            call.respond(DataResponse(response))
        }

        get("/journeys") {
            val projectId = call.parameters["projectId"]!!
            val (start, end) = parseDateRange(call)
            val applicationId = call.request.queryParameters["application_id"]
            val response = service.journeys(projectId, start, end, applicationId)
            call.respond(DataResponse(response))
        }

        post("/funnels/query") {
            val projectId = call.parameters["projectId"]!!
            val request = call.receive<FunnelRequest>()
            val response = service.funnel(projectId, request)
            call.respond(DataResponse(response))
        }
    }
}

private fun parseDateRange(call: io.ktor.server.routing.RoutingCall): Pair<Instant, Instant> {
    val startDate =
        call.request.queryParameters["start_date"]
            ?: throw ApiException("VALIDATION_ERROR", HttpStatusCode.BadRequest, "start_date는 필수입니다")
    val endDate =
        call.request.queryParameters["end_date"]
            ?: throw ApiException("VALIDATION_ERROR", HttpStatusCode.BadRequest, "end_date는 필수입니다")
    return Pair(Instant.parse(startDate), Instant.parse(endDate))
}
