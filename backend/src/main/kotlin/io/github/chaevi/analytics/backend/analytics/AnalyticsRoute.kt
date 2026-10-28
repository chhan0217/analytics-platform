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

@Suppress("LongMethod")
fun Route.analyticsRoutes(service: AnalyticsService = AnalyticsService()) {
    route("/projects/{projectId}/analytics") {
        get("/active-users") {
            val projectId = call.requireProjectId()
            val date = call.request.queryParameters["date"] ?: todayKst()
            val applicationId = call.request.queryParameters["application_id"]
            val response = service.activeUsers(projectId, date, applicationId)
            call.respond(DataResponse(response))
        }

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

        get("/trend") {
            val projectId = call.requireProjectId()
            val (start, end) = parseDateRange(call)
            val query =
                TrendQuery(
                    projectId = projectId,
                    start = start,
                    end = end,
                    granularity = call.request.queryParameters["granularity"] ?: "daily",
                    eventKey = call.request.queryParameters["event_key"],
                    propertyName = call.request.queryParameters["property_name"],
                    propertyValue = call.request.queryParameters["property_value"],
                    applicationId = call.request.queryParameters["application_id"],
                )
            call.respond(DataResponse(service.trend(query)))
        }

        get("/retention") {
            val projectId = call.requireProjectId()
            val (start, end) = parseDateRange(call)
            val retentionDaysParam = call.request.queryParameters["retention_days"] ?: "1,7,30"
            val retentionDays = retentionDaysParam.split(",").map { it.trim().toInt() }
            val type = call.request.queryParameters["type"] ?: "new"
            val applicationId = call.request.queryParameters["application_id"]
            val response = service.retention(projectId, start, end, retentionDays, type, applicationId)
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

private val ZONE_KST = java.time.ZoneId.of("Asia/Seoul")

private fun parseDateRange(call: io.ktor.server.routing.RoutingCall): Pair<Instant, Instant> {
    val startDate =
        call.request.queryParameters["start_date"]
            ?: throw ApiException("VALIDATION_ERROR", HttpStatusCode.BadRequest, "start_date는 필수입니다")
    val endDate =
        call.request.queryParameters["end_date"]
            ?: throw ApiException("VALIDATION_ERROR", HttpStatusCode.BadRequest, "end_date는 필수입니다")
    return Pair(parseStartInstant(startDate), parseEndInstant(endDate))
}

/** 날짜만 넘기면 KST 해당일 00:00:00 (= UTC 전날 15:00:00), ISO면 그대로 파싱 */
private fun parseStartInstant(value: String): Instant =
    if (value.contains("T")) {
        Instant.parse(value)
    } else {
        val kstStart =
            java.time.LocalDate
                .parse(value)
                .atStartOfDay(ZONE_KST)
        Instant.parse(kstStart.toInstant().toString())
    }

/** 날짜만 넘기면 KST 해당일 23:59:59 (= UTC 당일 14:59:59), ISO면 그대로 파싱 */
private fun parseEndInstant(value: String): Instant =
    if (value.contains("T")) {
        Instant.parse(value)
    } else {
        val kstEnd =
            java.time.LocalDate
                .parse(value)
                .atTime(23, 59, 59)
                .atZone(ZONE_KST)
        Instant.parse(kstEnd.toInstant().toString())
    }
