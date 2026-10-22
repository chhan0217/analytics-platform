package io.github.chaevi.analytics.backend.application

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

fun Route.applicationRoutes(service: ApplicationService = ApplicationService()) {
    route("/projects/{projectId}/applications") {
        post {
            val projectId = call.parameters["projectId"]!!
            val request = call.receive<CreateApplicationRequest>()
            val response = service.create(projectId, request)
            call.respond(HttpStatusCode.Created, DataResponse(response))
        }

        get {
            val projectId = call.parameters["projectId"]!!
            val page = call.parameters["page"]?.toIntOrNull() ?: 1
            val pageSize = call.parameters["page_size"]?.toIntOrNull() ?: 20

            val (data, total) = service.findAllByProject(projectId, page, pageSize)
            call.respond(
                ListResponse(
                    data = data,
                    meta = PageMeta(total = total, page = page, pageSize = pageSize),
                ),
            )
        }

        get("/{applicationId}") {
            val projectId = call.parameters["projectId"]!!
            val applicationId = call.parameters["applicationId"]!!
            val response = service.findById(projectId, applicationId)
            call.respond(DataResponse(response))
        }

        patch("/{applicationId}") {
            val projectId = call.parameters["projectId"]!!
            val applicationId = call.parameters["applicationId"]!!
            val request = call.receive<UpdateApplicationRequest>()
            val response = service.update(projectId, applicationId, request)
            call.respond(DataResponse(response))
        }
    }
}
