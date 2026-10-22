package io.github.chaevi.analytics.backend.e2e

import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.header
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.http.ContentType
import io.ktor.http.contentType
import io.ktor.server.testing.ApplicationTestBuilder
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive

@Serializable
data class Wrap<T>(
    val data: T,
)

@Serializable
data class OrgResp(
    val id: String,
    val name: String,
)

@Serializable
data class ProjResp(
    val id: String,
    val name: String,
)

@Serializable
data class AppResp(
    val id: String,
    val platform: String,
)

@Serializable
data class CredResp(
    val ingestionKey: String,
)

@Serializable
data class IngestResp(
    val accepted: Int,
    val duplicated: Int,
)

@Serializable
data class IngestEvent(
    val eventId: String,
    val sessionId: String,
    val anonymousId: String,
    val eventKey: String,
    val schemaVersion: Int = 1,
    val occurredAt: String,
    val properties: JsonObject = JsonObject(emptyMap()),
    val context: JsonObject = JsonObject(emptyMap()),
    val externalUserId: String? = null,
)

@Serializable
data class IngestPayload(
    val events: List<IngestEvent>,
)

data class ProjectSetup(
    val projectId: String,
    val androidAppId: String,
    val webAppId: String,
    val androidKey: String,
    val webKey: String,
)

suspend fun ApplicationTestBuilder.setupProject(
    adminClient: HttpClient,
    orgId: String,
    projectName: String,
): ProjectSetup {
    val proj =
        adminClient
            .post("/admin/organizations/$orgId/projects") {
                contentType(ContentType.Application.Json)
                setBody("""{"name":"$projectName"}""")
            }.body<Wrap<ProjResp>>()
            .data

    val androidApp =
        adminClient
            .post("/admin/projects/${proj.id}/applications") {
                contentType(ContentType.Application.Json)
                setBody("""{"name":"Android","platform":"ANDROID"}""")
            }.body<Wrap<AppResp>>()
            .data

    val webApp =
        adminClient
            .post("/admin/projects/${proj.id}/applications") {
                contentType(ContentType.Application.Json)
                setBody("""{"name":"Web","platform":"WEB"}""")
            }.body<Wrap<AppResp>>()
            .data

    val androidKey =
        adminClient
            .post(
                "/admin/projects/${proj.id}/applications/${androidApp.id}/credentials",
            ).body<Wrap<CredResp>>()
            .data.ingestionKey

    val webKey =
        adminClient
            .post(
                "/admin/projects/${proj.id}/applications/${webApp.id}/credentials",
            ).body<Wrap<CredResp>>()
            .data.ingestionKey

    return ProjectSetup(proj.id, androidApp.id, webApp.id, androidKey, webKey)
}

suspend fun HttpClient.ingestEvents(
    key: String,
    events: List<IngestEvent>,
): IngestResp =
    this
        .post("/ingest/events") {
            header("X-Ingestion-Key", key)
            contentType(ContentType.Application.Json)
            setBody(IngestPayload(events = events))
        }.body<Wrap<IngestResp>>()
        .data

fun event(
    eventKey: String,
    sessionId: String = "c0000000-0000-0000-0000-000000000001",
    anonymousId: String = "a0000000-0000-0000-0000-000000000001",
    occurredAt: String = "2026-10-22T10:00:00Z",
    properties: Map<String, String> = emptyMap(),
    externalUserId: String? = null,
): IngestEvent =
    IngestEvent(
        eventId =
            java.util.UUID
                .randomUUID()
                .toString(),
        sessionId = sessionId,
        anonymousId = anonymousId,
        eventKey = eventKey,
        occurredAt = occurredAt,
        properties = JsonObject(properties.mapValues { JsonPrimitive(it.value) }),
        externalUserId = externalUserId,
    )
