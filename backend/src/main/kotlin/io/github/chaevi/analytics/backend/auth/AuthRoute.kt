package io.github.chaevi.analytics.backend.auth

import io.github.chaevi.analytics.backend.common.DataResponse
import io.ktor.server.request.receive
import io.ktor.server.response.respond
import io.ktor.server.routing.Route
import io.ktor.server.routing.post
import io.ktor.server.routing.route

fun Route.authRoutes(service: AuthService = AuthService()) {
    route("/auth") {
        post("/login") {
            val request = call.receive<LoginRequest>()
            val response = service.login(request)
            call.respond(DataResponse(response))
        }
    }
}
