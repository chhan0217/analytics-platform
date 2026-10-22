package io.github.chaevi.analytics.backend.credential

import io.github.chaevi.analytics.backend.common.DataResponse
import io.github.chaevi.analytics.backend.common.ListResponse
import io.github.chaevi.analytics.backend.common.PageMeta
import io.ktor.http.HttpStatusCode
import io.ktor.server.response.respond
import io.ktor.server.routing.Route
import io.ktor.server.routing.delete
import io.ktor.server.routing.get
import io.ktor.server.routing.post
import io.ktor.server.routing.route

fun Route.credentialRoutes(service: AppCredentialService = AppCredentialService()) {
    route("/projects/{projectId}/applications/{applicationId}/credentials") {
        post {
            val projectId = call.parameters["projectId"]!!
            val applicationId = call.parameters["applicationId"]!!
            val response = service.create(projectId, applicationId)
            call.respond(HttpStatusCode.Created, DataResponse(response))
        }

        get {
            val projectId = call.parameters["projectId"]!!
            val applicationId = call.parameters["applicationId"]!!
            val credentials = service.findAllByApplication(projectId, applicationId)
            call.respond(
                ListResponse(
                    data = credentials,
                    meta = PageMeta(total = credentials.size.toLong(), page = 1, pageSize = credentials.size),
                ),
            )
        }

        delete("/{credentialId}") {
            val projectId = call.parameters["projectId"]!!
            val applicationId = call.parameters["applicationId"]!!
            val credentialId = call.parameters["credentialId"]!!
            service.revoke(projectId, applicationId, credentialId)
            call.respond(HttpStatusCode.NoContent)
        }
    }
}
