package io.github.chaevi.analytics.backend

import io.github.chaevi.analytics.backend.auth.AuthService
import io.github.chaevi.analytics.backend.auth.JwtConfig.configureAuth
import io.github.chaevi.analytics.backend.common.configureDatabase
import io.github.chaevi.analytics.backend.common.configureRouting
import io.github.chaevi.analytics.backend.common.configureSerialization
import io.github.chaevi.analytics.backend.common.configureStatusPages
import io.ktor.server.application.Application
import io.ktor.server.engine.embeddedServer
import io.ktor.server.netty.Netty

fun main() {
    val port = System.getenv("BACKEND_PORT")?.toIntOrNull() ?: 8080

    embeddedServer(Netty, port = port, module = Application::module)
        .start(wait = true)
}

fun Application.module() {
    configureSerialization()
    configureStatusPages()
    configureDatabase()
    configureAuth()
    configureRouting()
    AuthService().seedDefaultAdmin()
}
