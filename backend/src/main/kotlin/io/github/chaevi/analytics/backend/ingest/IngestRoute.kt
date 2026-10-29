package io.github.chaevi.analytics.backend.ingest

import io.github.chaevi.analytics.backend.common.DataResponse
import io.github.chaevi.analytics.backend.replay.ScreenTemplateRequest
import io.github.chaevi.analytics.backend.replay.ScreenTemplateService
import io.ktor.server.request.receive
import io.ktor.server.response.respond
import io.ktor.server.routing.Route
import io.ktor.server.routing.post
import io.ktor.server.routing.route

fun Route.ingestRoutes(
    service: IngestService = IngestService(),
    templateService: ScreenTemplateService = ScreenTemplateService(),
) {
    route("/ingest") {
        post("/events") {
            val ingestionKey = call.request.headers["X-Ingestion-Key"]
            val request = call.receive<IngestRequest>()
            val response = service.ingest(ingestionKey, request)
            call.respond(DataResponse(response))
        }

        post("/screen-templates") {
            val ingestionKey = call.request.headers["X-Ingestion-Key"]
            val request = call.receive<ScreenTemplateRequest>()
            val response = templateService.ingestTemplates(ingestionKey, request)
            call.respond(DataResponse(response))
        }
    }
}
