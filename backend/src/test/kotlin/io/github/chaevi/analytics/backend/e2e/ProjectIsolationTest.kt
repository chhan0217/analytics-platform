package io.github.chaevi.analytics.backend.e2e

import io.github.chaevi.analytics.backend.common.createAuthenticatedClient
import io.github.chaevi.analytics.backend.common.createTestClient
import io.github.chaevi.analytics.backend.common.setupTestApp
import io.kotest.matchers.shouldBe
import io.ktor.client.call.body
import io.ktor.client.request.get
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.http.ContentType
import io.ktor.http.contentType
import io.ktor.server.testing.testApplication
import kotlinx.serialization.Serializable
import kotlin.test.Test

@Serializable
data class EventListResp(
    val data: List<EventItem>,
)

@Serializable
data class EventItem(
    val id: String,
    val eventKey: String,
    val projectId: String,
)

@Serializable
data class UserListResp(
    val data: List<UserItem>,
)

@Serializable
data class UserItem(
    val id: String,
    val externalUserId: String?,
)

@Serializable
data class SessionListResp(
    val data: List<SessionItem>,
)

@Serializable
data class SessionItem(
    val id: String,
    val applicationId: String,
)

class ProjectIsolationTest {
    @Test
    fun `동일 external_user_id가 프로젝트 간 격리됨`() =
        testApplication {
            setupTestApp()
            val admin = createAuthenticatedClient()
            val plain = createTestClient()

            val org =
                admin
                    .post("/admin/organizations") {
                        contentType(io.ktor.http.ContentType.Application.Json)
                        setBody("""{"name":"격리 테스트 조직"}""")
                    }.body<Wrap<OrgResp>>()
                    .data

            val projA = setupProject(admin, org.id, "프로젝트 A")
            val projB = setupProject(admin, org.id, "프로젝트 B")

            // 같은 external_user_id로 두 프로젝트에 이벤트 전송
            plain.ingestEvents(
                projA.androidKey,
                listOf(
                    event(
                        "screen_view",
                        occurredAt = "2026-10-22T10:00:01Z",
                        externalUserId = "user_100",
                        properties = mapOf("screen_id" to "home_a"),
                    ),
                ),
            )

            plain.ingestEvents(
                projB.androidKey,
                listOf(
                    event(
                        "screen_view",
                        occurredAt = "2026-10-22T10:00:02Z",
                        externalUserId = "user_100",
                        properties = mapOf("screen_id" to "home_b"),
                    ),
                ),
            )

            // 프로젝트 A 이벤트 조회 — 프로젝트 B 이벤트가 안 보여야 함
            val eventsA =
                admin
                    .get(
                        "/admin/projects/${projA.projectId}/events?start_date=2026-10-22T00:00:00Z&end_date=2026-10-22T23:59:59Z",
                    ).body<EventListResp>()

            eventsA.data.all { it.projectId == projA.projectId } shouldBe true
            eventsA.data.none { it.projectId == projB.projectId } shouldBe true

            // 프로젝트 B도 마찬가지
            val eventsB =
                admin
                    .get(
                        "/admin/projects/${projB.projectId}/events?start_date=2026-10-22T00:00:00Z&end_date=2026-10-22T23:59:59Z",
                    ).body<EventListResp>()

            eventsB.data.all { it.projectId == projB.projectId } shouldBe true
        }

    @Test
    fun `크로스 플랫폼 동일 프로젝트 이벤트 통합 조회`() =
        testApplication {
            setupTestApp()
            val admin = createAuthenticatedClient()
            val plain = createTestClient()

            val org =
                admin
                    .post("/admin/organizations") {
                        contentType(io.ktor.http.ContentType.Application.Json)
                        setBody("""{"name":"크로스 플랫폼 조직"}""")
                    }.body<Wrap<OrgResp>>()
                    .data

            val proj = setupProject(admin, org.id, "통합 프로젝트")

            // Android에서 이벤트
            plain.ingestEvents(
                proj.androidKey,
                listOf(
                    event(
                        "screen_view",
                        sessionId = "c1111111-0000-0000-0000-000000000001",
                        anonymousId = "a1111111-0000-0000-0000-000000000001",
                        occurredAt = "2026-10-22T10:00:01Z",
                        properties = mapOf("screen_id" to "android_home"),
                    ),
                ),
            )

            // Web에서 이벤트
            plain.ingestEvents(
                proj.webKey,
                listOf(
                    event(
                        "screen_view",
                        sessionId = "c2222222-0000-0000-0000-000000000001",
                        anonymousId = "a2222222-0000-0000-0000-000000000001",
                        occurredAt = "2026-10-22T10:00:02Z",
                        properties = mapOf("screen_id" to "web_home"),
                    ),
                ),
            )

            // 프로젝트 전체 이벤트에 두 플랫폼 모두 포함
            val events =
                admin
                    .get(
                        "/admin/projects/${proj.projectId}/events?start_date=2026-10-22T00:00:00Z&end_date=2026-10-22T23:59:59Z",
                    ).body<EventListResp>()

            events.data.size shouldBe 2
        }

    @Test
    fun `크로스 플랫폼 동일 사용자 세션 통합 조회`() =
        testApplication {
            setupTestApp()
            val admin = createAuthenticatedClient()
            val plain = createTestClient()

            val org =
                admin
                    .post("/admin/organizations") {
                        contentType(ContentType.Application.Json)
                        setBody("""{"name":"세션 통합 조직"}""")
                    }.body<Wrap<OrgResp>>()
                    .data

            val proj = setupProject(admin, org.id, "세션 통합 프로젝트")

            // Android에서 identify + 이벤트
            plain.ingestEvents(
                proj.androidKey,
                listOf(
                    event(
                        "screen_view",
                        sessionId = "ca000001-0000-0000-0000-000000000001",
                        anonymousId = "aa000001-0000-0000-0000-000000000001",
                        occurredAt = "2026-10-22T10:00:01Z",
                        externalUserId = "cross_user_1",
                    ),
                ),
            )

            // Web에서 identify + 이벤트 (동일 external_user_id)
            plain.ingestEvents(
                proj.webKey,
                listOf(
                    event(
                        "screen_view",
                        sessionId = "ca000002-0000-0000-0000-000000000001",
                        anonymousId = "aa000002-0000-0000-0000-000000000001",
                        occurredAt = "2026-10-22T10:00:02Z",
                        externalUserId = "cross_user_1",
                    ),
                ),
            )

            // 사용자 조회 — 동일 external_user_id로 1명만 존재해야 함
            val users =
                admin
                    .get(
                        "/admin/projects/${proj.projectId}/users?external_user_id=cross_user_1",
                    ).body<UserListResp>()

            users.data.size shouldBe 1

            // 해당 사용자의 세션 조회 — Android, Web 두 세션 모두 보여야 함
            val userId = users.data.first().id
            val sessions =
                admin
                    .get(
                        "/admin/projects/${proj.projectId}/users/$userId/sessions",
                    ).body<SessionListResp>()

            sessions.data.size shouldBe 2

            // 두 세션이 서로 다른 앱(Android, Web)에 속하는지 확인
            val appIds = sessions.data.map { it.applicationId }.toSet()
            appIds.size shouldBe 2
            appIds shouldBe setOf(proj.androidAppId, proj.webAppId)
        }
}
