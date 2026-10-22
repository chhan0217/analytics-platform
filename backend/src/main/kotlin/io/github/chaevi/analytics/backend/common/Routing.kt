package io.github.chaevi.analytics.backend.common

import io.github.chaevi.analytics.backend.analytics.analyticsRoutes
import io.github.chaevi.analytics.backend.application.applicationRoutes
import io.github.chaevi.analytics.backend.auth.authRoutes
import io.github.chaevi.analytics.backend.credential.credentialRoutes
import io.github.chaevi.analytics.backend.ingest.eventQueryRoutes
import io.github.chaevi.analytics.backend.ingest.ingestRoutes
import io.github.chaevi.analytics.backend.organization.organizationRoutes
import io.github.chaevi.analytics.backend.project.projectRoutes
import io.github.chaevi.analytics.backend.taxonomy.taxonomyEventRoutes
import io.github.chaevi.analytics.backend.taxonomy.taxonomyPropertyRoutes
import io.github.chaevi.analytics.backend.user.userQueryRoutes
import io.ktor.server.application.Application
import io.ktor.server.auth.authenticate
import io.ktor.server.response.respond
import io.ktor.server.routing.get
import io.ktor.server.routing.route
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

@Serializable
data class PageMeta(
    val total: Long,
    val page: Int,
    val pageSize: Int,
)

@Serializable
data class ListResponse<T>(
    val data: List<T>,
    val meta: PageMeta,
)

fun Application.configureRouting() {
    routing {
        get("/health") {
            call.respond(DataResponse(HealthResponse()))
        }

        ingestRoutes()

        route("/admin") {
            authRoutes()

            authenticate("admin-jwt") {
                organizationRoutes()
                projectRoutes()
                applicationRoutes()
                credentialRoutes()
                taxonomyEventRoutes()
                taxonomyPropertyRoutes()
                eventQueryRoutes()
                userQueryRoutes()
                analyticsRoutes()
            }
        }
    }
}
