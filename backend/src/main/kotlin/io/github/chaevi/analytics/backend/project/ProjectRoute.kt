package io.github.chaevi.analytics.backend.project

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

fun Route.projectRoutes(service: ProjectService = ProjectService()) {
    route("/organizations/{organizationId}/projects") {
        post {
            val orgId = call.parameters["organizationId"]!!
            val request = call.receive<CreateProjectRequest>()
            val response = service.create(orgId, request)
            call.respond(HttpStatusCode.Created, DataResponse(response))
        }

        get {
            val orgId = call.parameters["organizationId"]!!
            val page = call.parameters["page"]?.toIntOrNull() ?: 1
            val pageSize = call.parameters["page_size"]?.toIntOrNull() ?: 20

            val (data, total) = service.findAllByOrganization(orgId, page, pageSize)
            call.respond(
                ListResponse(
                    data = data,
                    meta = PageMeta(total = total, page = page, pageSize = pageSize),
                ),
            )
        }
    }

    route("/projects/{projectId}") {
        get {
            val id = call.parameters["projectId"]!!
            val response = service.findById(id)
            call.respond(DataResponse(response))
        }

        patch {
            val id = call.parameters["projectId"]!!
            val request = call.receive<UpdateProjectRequest>()
            val response = service.update(id, request)
            call.respond(DataResponse(response))
        }
    }
}
