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
import io.kotest.matchers.shouldNotBe
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.get
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
data class OrgQ(
    val data: OrganizationResponse,
)

@Serializable
data class ProjQ(
    val data: ProjectResponse,
)

@Serializable
data class AppQ(
    val data: ApplicationResponse,
)

@Serializable
data class CredQ(
    val data: CreateCredentialResponse,
)

@Serializable
data class EventDetailW(
    val data: EventResponse,
)

class EventQueryRouteTest {
    private suspend fun setupAndIngest(
        adminClient: HttpClient,
        plainClient: HttpClient,
        eventCount: Int = 5,
    ): Pair<String, String> {
        val org =
            adminClient
                .post("/admin/organizations") {
                    contentType(ContentType.Application.Json)
                    setBody(CreateOrganizationRequest(name = "쿼리 테스트 조직"))
                }.body<OrgQ>()
                .data

        val proj =
            adminClient
                .post("/admin/organizations/${org.id}/projects") {
                    contentType(ContentType.Application.Json)
                    setBody(CreateProjectRequest(name = "쿼리 테스트 프로젝트"))
                }.body<ProjQ>()
                .data

        val app =
            adminClient
                .post("/admin/projects/${proj.id}/applications") {
                    contentType(ContentType.Application.Json)
                    setBody(CreateApplicationRequest(name = "Android", platform = "ANDROID"))
                }.body<AppQ>()
                .data

        val key =
            adminClient
                .post("/admin/projects/${proj.id}/applications/${app.id}/credentials")
                .body<CredQ>()
                .data
                .ingestionKey

        val events =
            (1..eventCount).map { i ->
                IngestEvent(
                    eventId =
                        java.util.UUID
                            .randomUUID()
                            .toString(),
                    sessionId = "10000000-0000-0000-0000-000000000001",
                    anonymousId = "20000000-0000-0000-0000-000000000001",
                    eventKey = if (i % 2 == 0) "screen_view" else "button_click",
                    occurredAt = "2026-10-22T10:00:%02dZ".format(i),
                    properties = JsonObject(mapOf("screen_id" to JsonPrimitive("page_$i"))),
                )
            }

        plainClient.post("/ingest/events") {
            header("X-Ingestion-Key", key)
            contentType(ContentType.Application.Json)
            setBody(IngestRequest(events = events))
        }

        return Pair(proj.id, events.first().eventId)
    }

    @Test
    fun `이벤트 목록 조회`() =
        testApplication {
            setupTestApp()
            val adminClient = createAuthenticatedClient()
            val plainClient = createTestClient()
            val (projId, _) = setupAndIngest(adminClient, plainClient, 5)

            val response =
                adminClient.get(
                    "/admin/projects/$projId/events?start_date=2026-10-22T00:00:00Z&end_date=2026-10-22T23:59:59Z",
                )

            response.status shouldBe HttpStatusCode.OK
            val body = response.body<CursorListResponse<EventResponse>>()
            body.data.size shouldBe 5
            body.meta.hasMore shouldBe false
        }

    @Test
    fun `이벤트 커서 페이지네이션`() =
        testApplication {
            setupTestApp()
            val adminClient = createAuthenticatedClient()
            val plainClient = createTestClient()
            val (projId, _) = setupAndIngest(adminClient, plainClient, 5)

            val page1 =
                adminClient
                    .get(
                        "/admin/projects/$projId/events?start_date=2026-10-22T00:00:00Z&end_date=2026-10-22T23:59:59Z&page_size=3",
                    ).body<CursorListResponse<EventResponse>>()

            page1.data.size shouldBe 3
            page1.meta.hasMore shouldBe true
            page1.meta.nextCursor shouldNotBe null

            // 커서로 다음 페이지 요청 — 이전 페이지 ID와 중복 없어야 함
            val page2 =
                adminClient
                    .get(
                        "/admin/projects/$projId/events?start_date=2026-10-22T00:00:00Z&end_date=2026-10-22T23:59:59Z&page_size=3&cursor=${page1.meta.nextCursor}",
                    ).body<CursorListResponse<EventResponse>>()

            val allIds = (page1.data.map { it.id } + page2.data.map { it.id }).toSet()
            allIds.size shouldBe page1.data.size + page2.data.size
            (page1.data.size + page2.data.size) shouldBe 5
        }

    @Test
    fun `이벤트 키 필터`() =
        testApplication {
            setupTestApp()
            val adminClient = createAuthenticatedClient()
            val plainClient = createTestClient()
            val (projId, _) = setupAndIngest(adminClient, plainClient, 6)

            val response =
                adminClient.get(
                    "/admin/projects/$projId/events?start_date=2026-10-22T00:00:00Z&end_date=2026-10-22T23:59:59Z&event_key=screen_view",
                )

            val body = response.body<CursorListResponse<EventResponse>>()
            body.data.all { it.eventKey == "screen_view" } shouldBe true
        }

    @Test
    fun `이벤트 상세 조회`() =
        testApplication {
            setupTestApp()
            val adminClient = createAuthenticatedClient()
            val plainClient = createTestClient()
            val (projId, eventId) = setupAndIngest(adminClient, plainClient, 1)

            val response = adminClient.get("/admin/projects/$projId/events/$eventId")
            response.status shouldBe HttpStatusCode.OK

            val body = response.body<EventDetailW>()
            body.data.id shouldBe eventId
        }

    @Test
    fun `start_date 없이 요청 시 400`() =
        testApplication {
            setupTestApp()
            val adminClient = createAuthenticatedClient()

            val response =
                adminClient.get(
                    "/admin/projects/00000000-0000-0000-0000-000000000000/events?end_date=2026-10-22T23:59:59Z",
                )

            response.status shouldBe HttpStatusCode.BadRequest
        }
}
