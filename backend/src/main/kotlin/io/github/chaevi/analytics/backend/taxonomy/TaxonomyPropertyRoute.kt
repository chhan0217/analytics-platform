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

fun Route.taxonomyPropertyRoutes(service: TaxonomyPropertyService = TaxonomyPropertyService()) {
    route("/projects/{projectId}/taxonomy/events/{eventId}/properties") {
        post {
            val projectId = call.parameters["projectId"]!!
            val eventId = call.parameters["eventId"]!!
            val request = call.receive<CreateTaxonomyPropertyRequest>()
            val response = service.create(projectId, eventId, request)
            call.respond(HttpStatusCode.Created, DataResponse(response))
        }

        get {
            val projectId = call.parameters["projectId"]!!
            val eventId = call.parameters["eventId"]!!
            val properties = service.findAllByEvent(projectId, eventId)
            call.respond(
                ListResponse(
                    data = properties,
                    meta = PageMeta(total = properties.size.toLong(), page = 1, pageSize = properties.size),
                ),
            )
        }

        patch("/{propertyId}") {
            val projectId = call.parameters["projectId"]!!
            val eventId = call.parameters["eventId"]!!
            val propertyId = call.parameters["propertyId"]!!
            val request = call.receive<UpdateTaxonomyPropertyRequest>()
            val response = service.update(projectId, eventId, propertyId, request)
            call.respond(DataResponse(response))
        }
    }
}
