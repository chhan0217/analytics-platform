package io.github.chaevi.analytics.backend.organization

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

fun Route.organizationRoutes(service: OrganizationService = OrganizationService()) {
    route("/organizations") {
        post {
            val request = call.receive<CreateOrganizationRequest>()
            val response = service.create(request)
            call.respond(HttpStatusCode.Created, DataResponse(response))
        }

        get {
            val page = call.parameters["page"]?.toIntOrNull() ?: 1
            val pageSize = call.parameters["page_size"]?.toIntOrNull() ?: 20

            val (data, total) = service.findAll(page, pageSize)
            call.respond(
                ListResponse(
                    data = data,
                    meta = PageMeta(total = total, page = page, pageSize = pageSize),
                ),
            )
        }

        get("/{organizationId}") {
            val id = call.parameters["organizationId"]!!
            val response = service.findById(id)
            call.respond(DataResponse(response))
        }

        patch("/{organizationId}") {
            val id = call.parameters["organizationId"]!!
            val request = call.receive<UpdateOrganizationRequest>()
            val response = service.update(id, request)
            call.respond(DataResponse(response))
        }
    }
}
