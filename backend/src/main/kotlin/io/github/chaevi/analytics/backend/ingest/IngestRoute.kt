package io.github.chaevi.analytics.backend.ingest

import io.github.chaevi.analytics.backend.common.DataResponse
import io.ktor.server.request.receive
import io.ktor.server.response.respond
import io.ktor.server.routing.Route
import io.ktor.server.routing.post
import io.ktor.server.routing.route

fun Route.ingestRoutes(service: IngestService = IngestService()) {
    route("/ingest") {
        post("/events") {
            val ingestionKey = call.request.headers["X-Ingestion-Key"]
            val request = call.receive<IngestRequest>()
            val response = service.ingest(ingestionKey, request)
            call.respond(DataResponse(response))
        }
    }
}
