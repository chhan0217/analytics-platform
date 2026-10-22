package io.github.chaevi.analytics.backend.ingest

import io.github.chaevi.analytics.backend.application.ApplicationResponse
import io.github.chaevi.analytics.backend.application.CreateApplicationRequest
import io.github.chaevi.analytics.backend.common.createAuthenticatedClient
import io.github.chaevi.analytics.backend.common.createTestClient
import io.github.chaevi.analytics.backend.common.setupTestApp
import io.github.chaevi.analytics.backend.credential.CreateCredentialResponse
import io.github.chaevi.analytics.backend.organization.CreateOrganizationRequest
import io.github.chaevi.analytics.backend.organization.OrganizationResponse
import io.github.chaevi.analytics.backend.project.CreateProjectRequest
import io.github.chaevi.analytics.backend.project.ProjectResponse
import io.kotest.matchers.shouldBe
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.header
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.http.ContentType
import io.ktor.http.HttpStatusCode
import io.ktor.http.contentType
import io.ktor.server.testing.testApplication
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlin.test.Test

@Serializable
data class OrgI(
    val data: OrganizationResponse,
)

@Serializable
data class ProjI(
    val data: ProjectResponse,
)

@Serializable
data class AppI(
    val data: ApplicationResponse,
)

@Serializable
data class CredI(
    val data: CreateCredentialResponse,
)

@Serializable
data class IngestW(
    val data: IngestResponse,
)

class IngestRouteTest {
    private suspend fun setupAppWithKey(adminClient: HttpClient): String {
        val org =
            adminClient
                .post("/admin/organizations") {
                    contentType(ContentType.Application.Json)
                    setBody(CreateOrganizationRequest(name = "테스트 조직"))
                }.body<OrgI>()
                .data

        val proj =
            adminClient
                .post("/admin/organizations/${org.id}/projects") {
                    contentType(ContentType.Application.Json)
                    setBody(CreateProjectRequest(name = "테스트 프로젝트"))
                }.body<ProjI>()
                .data

        val app =
            adminClient
                .post("/admin/projects/${proj.id}/applications") {
                    contentType(ContentType.Application.Json)
                    setBody(CreateApplicationRequest(name = "Android", platform = "ANDROID"))
                }.body<AppI>()
                .data

        val cred =
            adminClient
                .post("/admin/projects/${proj.id}/applications/${app.id}/credentials")
                .body<CredI>()
                .data

        return cred.ingestionKey
    }

    private fun createEvents(count: Int): List<IngestEvent> =
        (1..count).map { i ->
            IngestEvent(
                eventId =
                    java.util.UUID
                        .randomUUID()
                        .toString(),
                sessionId = "10000000-0000-0000-0000-000000000001",
                anonymousId = "20000000-0000-0000-0000-000000000001",
                eventKey = "screen_view",
                occurredAt = "2026-10-22T10:00:0${i % 10}Z",
                properties = JsonObject(mapOf("screen_id" to JsonPrimitive("home"))),
            )
        }

    @Test
    fun `이벤트 수집 성공`() =
        testApplication {
            setupTestApp()
            val adminClient = createAuthenticatedClient()
            val key = setupAppWithKey(adminClient)
            val client = createTestClient()

            val response =
                client.post("/ingest/events") {
                    header("X-Ingestion-Key", key)
                    contentType(ContentType.Application.Json)
                    setBody(IngestRequest(events = createEvents(3)))
                }

            response.status shouldBe HttpStatusCode.OK
            val body = response.body<IngestW>()
            body.data.accepted shouldBe 3
            body.data.duplicated shouldBe 0
        }

    @Test
    fun `중복 이벤트 idempotency`() =
        testApplication {
            setupTestApp()
            val adminClient = createAuthenticatedClient()
            val key = setupAppWithKey(adminClient)
            val client = createTestClient()

            val events = createEvents(2)

            client.post("/ingest/events") {
                header("X-Ingestion-Key", key)
                contentType(ContentType.Application.Json)
                setBody(IngestRequest(events = events))
            }

            val response =
                client.post("/ingest/events") {
                    header("X-Ingestion-Key", key)
                    contentType(ContentType.Application.Json)
                    setBody(IngestRequest(events = events))
                }

            response.status shouldBe HttpStatusCode.OK
            val body = response.body<IngestW>()
            body.data.accepted shouldBe 0
            body.data.duplicated shouldBe 2
        }

    @Test
    fun `수집 키 없이 요청 시 401`() =
        testApplication {
            setupTestApp()
            val client = createTestClient()

            val response =
                client.post("/ingest/events") {
                    contentType(ContentType.Application.Json)
                    setBody(IngestRequest(events = createEvents(1)))
                }

            response.status shouldBe HttpStatusCode.Unauthorized
        }

    @Test
    fun `잘못된 수집 키로 요청 시 401`() =
        testApplication {
            setupTestApp()
            val client = createTestClient()

            val response =
                client.post("/ingest/events") {
                    header("X-Ingestion-Key", "ink_invalid_key_00000000000000000000")
                    contentType(ContentType.Application.Json)
                    setBody(IngestRequest(events = createEvents(1)))
                }

            response.status shouldBe HttpStatusCode.Unauthorized
        }

    @Test
    fun `빈 이벤트 배치 시 400`() =
        testApplication {
            setupTestApp()
            val adminClient = createAuthenticatedClient()
            val key = setupAppWithKey(adminClient)
            val client = createTestClient()

            val response =
                client.post("/ingest/events") {
                    header("X-Ingestion-Key", key)
                    contentType(ContentType.Application.Json)
                    setBody(IngestRequest(events = emptyList()))
                }

            response.status shouldBe HttpStatusCode.BadRequest
        }

    @Test
    fun `배치 최대 크기 초과 시 413`() =
        testApplication {
            setupTestApp()
            val adminClient = createAuthenticatedClient()
            val key = setupAppWithKey(adminClient)
            val client = createTestClient()

            val response =
                client.post("/ingest/events") {
                    header("X-Ingestion-Key", key)
                    contentType(ContentType.Application.Json)
                    setBody(IngestRequest(events = createEvents(101)))
                }

            response.status shouldBe HttpStatusCode.PayloadTooLarge
        }

    @Test
    fun `정상 + 중복 혼합 배치`() =
        testApplication {
            setupTestApp()
            val adminClient = createAuthenticatedClient()
            val key = setupAppWithKey(adminClient)
            val client = createTestClient()

            val firstBatch = createEvents(3)
            client.post("/ingest/events") {
                header("X-Ingestion-Key", key)
                contentType(ContentType.Application.Json)
                setBody(IngestRequest(events = firstBatch))
            }

            val mixedBatch = firstBatch.take(2) + createEvents(2)
            val response =
                client.post("/ingest/events") {
                    header("X-Ingestion-Key", key)
                    contentType(ContentType.Application.Json)
                    setBody(IngestRequest(events = mixedBatch))
                }

            response.status shouldBe HttpStatusCode.OK
            val body = response.body<IngestW>()
            body.data.accepted shouldBe 2
            body.data.duplicated shouldBe 2
        }
}
