package io.github.chaevi.analytics.backend.common

import io.ktor.server.application.Application
import io.ktor.server.response.respond
import io.ktor.server.routing.get
import io.ktor.server.routing.routing
import kotlinx.datetime.Clock
import kotlinx.serialization.Serializable

@Serializable
data class HealthResponse(
    val status: String = "ok",
    val timestamp: String = Clock.System.now().toString(),
)

@Serializable
data class DataResponse<T>(
    val data: T,
)

fun Application.configureRouting() {
    routing {
        get("/health") {
            call.respond(DataResponse(HealthResponse()))
        }
    }
}
