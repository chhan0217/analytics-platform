package io.github.chaevi.analytics.backend.taxonomy

import io.github.chaevi.analytics.backend.common.DataResponse
import io.github.chaevi.analytics.backend.common.ListResponse
import io.github.chaevi.analytics.backend.common.PageMeta
import io.ktor.http.HttpStatusCode
import io.ktor.server.request.receive
import io.ktor.server.response.respond
import io.ktor.server.routing.Route
import io.ktor.server.routing.get
import io.ktor.server.routing.patch
import io.ktor.server.routing.post
import io.ktor.server.routing.route

fun Route.taxonomyEventRoutes(service: TaxonomyEventService = TaxonomyEventService()) {
    route("/projects/{projectId}/taxonomy/events") {
        post {
            val projectId = call.parameters["projectId"]!!
            val request = call.receive<CreateTaxonomyEventRequest>()
            val response = service.create(projectId, request)
            call.respond(HttpStatusCode.Created, DataResponse(response))
        }

        get {
            val projectId = call.parameters["projectId"]!!
            val page = call.parameters["page"]?.toIntOrNull() ?: 1
            val pageSize = call.parameters["page_size"]?.toIntOrNull() ?: 20

            val (data, total) = service.findAll(projectId, page, pageSize)
            call.respond(
                ListResponse(
                    data = data,
                    meta = PageMeta(total = total, page = page, pageSize = pageSize),
                ),
            )
        }

        get("/{eventId}") {
            val projectId = call.parameters["projectId"]!!
            val eventId = call.parameters["eventId"]!!
            val response = service.findById(projectId, eventId)
            call.respond(DataResponse(response))
        }

        patch("/{eventId}") {
            val projectId = call.parameters["projectId"]!!
            val eventId = call.parameters["eventId"]!!
            val request = call.receive<UpdateTaxonomyEventRequest>()
            val response = service.update(projectId, eventId, request)
            call.respond(DataResponse(response))
        }
    }
}
