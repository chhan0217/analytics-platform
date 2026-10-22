package io.github.chaevi.analytics.backend.e2e

import io.github.chaevi.analytics.backend.common.createAuthenticatedClient
import io.github.chaevi.analytics.backend.common.createTestClient
import io.github.chaevi.analytics.backend.common.setupTestApp
import io.kotest.matchers.shouldBe
import io.ktor.client.call.body
import io.ktor.client.request.header
import io.ktor.client.request.patch
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.http.ContentType
import io.ktor.http.HttpStatusCode
import io.ktor.http.contentType
import io.ktor.server.testing.testApplication
import kotlin.test.Test

class IngestValidationTest {
    @Test
    fun `동일 event_id 재전송 시 중복 저장 안 됨`() =
        testApplication {
            setupTestApp()
            val admin = createAuthenticatedClient()
            val plain = createTestClient()

            val org =
                admin
                    .post("/admin/organizations") {
                        contentType(ContentType.Application.Json)
                        setBody("""{"name":"중복 테스트"}""")
                    }.body<Wrap<OrgResp>>()
                    .data

            val proj = setupProject(admin, org.id, "중복 프로젝트")
            val evt = event("screen_view", occurredAt = "2026-10-22T11:00:00Z")

            val r1 = plain.ingestEvents(proj.androidKey, listOf(evt))
            r1.accepted shouldBe 1
            r1.duplicated shouldBe 0

            val r2 = plain.ingestEvents(proj.androidKey, listOf(evt))
            r2.accepted shouldBe 0
            r2.duplicated shouldBe 1
        }

    @Test
    fun `ARCHIVED 이벤트 수집 거부`() =
        testApplication {
            setupTestApp()
            val admin = createAuthenticatedClient()
            val plain = createTestClient()

            val org =
                admin
                    .post("/admin/organizations") {
                        contentType(ContentType.Application.Json)
                        setBody("""{"name":"ARCHIVED 테스트"}""")
                    }.body<Wrap<OrgResp>>()
                    .data

            val proj = setupProject(admin, org.id, "ARCHIVED 프로젝트")

            // 커스텀 이벤트 등록 후 ARCHIVED로 변경
            val taxEvent =
                admin
                    .post("/admin/projects/${proj.projectId}/taxonomy/events") {
                        contentType(ContentType.Application.Json)
                        setBody("""{"event_key":"archived_event","display_name":"보관됨"}""")
                    }.body<Wrap<TaxResp>>()
                    .data

            admin.patch("/admin/projects/${proj.projectId}/taxonomy/events/${taxEvent.id}") {
                contentType(ContentType.Application.Json)
                setBody("""{"status":"ARCHIVED"}""")
            }

            // ARCHIVED 이벤트 수집 시도
            val archivedEvent =
                event(
                    "archived_event",
                    sessionId = "s0000000-0000-0000-0000-000000000001",
                    anonymousId = "a0000000-0000-0000-0000-000000000001",
                    occurredAt = "2026-10-22T12:00:00Z",
                )
            val response =
                plain.post("/ingest/events") {
                    header("X-Ingestion-Key", proj.androidKey)
                    contentType(ContentType.Application.Json)
                    setBody(IngestPayload(events = listOf(archivedEvent)))
                }

            response.status shouldBe HttpStatusCode.BadRequest
        }

    @Test
    fun `잘못된 수집 키로 요청 시 401`() =
        testApplication {
            setupTestApp()
            val plain = createTestClient()

            val testEvent =
                event(
                    "test",
                    sessionId = "s0000000-0000-0000-0000-000000000001",
                    anonymousId = "a0000000-0000-0000-0000-000000000001",
                    occurredAt = "2026-10-22T12:00:00Z",
                )
            val result =
                plain.post("/ingest/events") {
                    header("X-Ingestion-Key", "ink_invalid_key_0000000000000000000000000000")
                    contentType(ContentType.Application.Json)
                    setBody(IngestPayload(events = listOf(testEvent)))
                }

            result.status shouldBe HttpStatusCode.Unauthorized
        }

    @Test
    fun `빈 배치 400, 초과 배치 413`() =
        testApplication {
            setupTestApp()
            val admin = createAuthenticatedClient()
            val plain = createTestClient()

            val org =
                admin
                    .post("/admin/organizations") {
                        contentType(ContentType.Application.Json)
                        setBody("""{"name":"배치 테스트"}""")
                    }.body<Wrap<OrgResp>>()
                    .data

            val proj = setupProject(admin, org.id, "배치 프로젝트")

            // 빈 배치
            val emptyResult =
                plain.post("/ingest/events") {
                    header("X-Ingestion-Key", proj.androidKey)
                    contentType(ContentType.Application.Json)
                    setBody("""{"events":[]}""")
                }
            emptyResult.status shouldBe HttpStatusCode.BadRequest

            // 101개 초과 배치
            val bigEvents = (1..101).map { event("test", occurredAt = "2026-10-22T12:00:0${it % 10}Z") }
            val overResult =
                plain.post("/ingest/events") {
                    header("X-Ingestion-Key", proj.androidKey)
                    contentType(ContentType.Application.Json)
                    setBody(IngestPayload(events = bigEvents))
                }
            overResult.status shouldBe HttpStatusCode.PayloadTooLarge
        }
}

@kotlinx.serialization.Serializable
data class TaxResp(
    val id: String,
    val eventKey: String,
)
