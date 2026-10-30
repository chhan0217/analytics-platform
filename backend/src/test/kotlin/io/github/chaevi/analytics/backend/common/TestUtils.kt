package io.github.chaevi.analytics.backend.common

import io.github.chaevi.analytics.backend.application.ApplicationTable
import io.github.chaevi.analytics.backend.auth.LoginRequest
import io.github.chaevi.analytics.backend.credential.AppCredentialTable
import io.github.chaevi.analytics.backend.module
import io.github.chaevi.analytics.backend.organization.OrganizationTable
import io.github.chaevi.analytics.backend.project.ProjectTable
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.plugins.defaultRequest
import io.ktor.client.request.bearerAuth
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.http.ContentType
import io.ktor.http.contentType
import io.ktor.serialization.kotlinx.json.json
import io.ktor.server.testing.ApplicationTestBuilder
import kotlinx.serialization.Serializable
import org.jetbrains.exposed.sql.deleteAll
import org.jetbrains.exposed.sql.transactions.transaction

@Serializable
data class LoginWrap(
    val data: LoginData,
)

@Serializable
data class LoginData(
    val token: String,
)

fun ApplicationTestBuilder.createTestClient(): HttpClient =
    createClient {
        install(ContentNegotiation) {
            json(appJson)
        }
    }

suspend fun ApplicationTestBuilder.createAuthenticatedClient(): HttpClient {
    val plainClient = createTestClient()
    val loginResponse =
        plainClient
            .post("/admin/auth/login") {
                contentType(ContentType.Application.Json)
                setBody(LoginRequest(email = "admin@analytics.local", password = "admin1234"))
            }.body<LoginWrap>()

    val token = loginResponse.data.token

    return createClient {
        install(ContentNegotiation) {
            json(appJson)
        }
        defaultRequest {
            bearerAuth(token)
        }
    }
}

fun ApplicationTestBuilder.setupTestApp() {
    application {
        module()
        cleanDatabase()
    }
}

fun cleanDatabase() {
    transaction {
        exec("DELETE FROM analytics_events")
        exec("DELETE FROM analytics_sessions")
        exec("DELETE FROM identity_links")
        exec("DELETE FROM anonymous_identities")
        exec("DELETE FROM analytics_users")
        exec("DELETE FROM taxonomy_properties")
        exec("DELETE FROM taxonomy_events")
        AppCredentialTable.deleteAll()
        ApplicationTable.deleteAll()
        ProjectTable.deleteAll()
        OrganizationTable.deleteAll()
    }
}
