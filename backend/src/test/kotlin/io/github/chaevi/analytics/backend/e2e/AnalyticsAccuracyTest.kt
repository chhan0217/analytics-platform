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
data class OverviewResp(
    val totalEvents: Long,
    val totalSessions: Long,
    val totalUsers: Long,
)

@Serializable
data class ScreenStatsResp(
    val screens: List<ScreenItem>,
)

@Serializable
data class ScreenItem(
    val screenId: String,
    val viewCount: Long,
    val uniqueUsers: Long,
)

@Serializable
data class FunnelResp(
    val steps: List<FunnelStepItem>,
    val overallConversionRate: Double,
)

@Serializable
data class FunnelStepItem(
    val step: Int,
    val eventKey: String,
    val uniqueUsers: Long,
)

class AnalyticsAccuracyTest {
    private val dateParams = "start_date=2026-10-22T00:00:00Z&end_date=2026-10-22T23:59:59Z"

    @Test
    fun `Overview 수치 정확성`() =
        testApplication {
            setupTestApp()
            val admin = createAuthenticatedClient()
            val plain = createTestClient()

            val org =
                admin
                    .post("/admin/organizations") {
                        contentType(ContentType.Application.Json)
                        setBody("""{"name":"분석 정확성 조직"}""")
                    }.body<Wrap<OrgResp>>()
                    .data

            val proj = setupProject(admin, org.id, "분석 프로젝트")

            // 사용자 2명, 세션 3개, 이벤트 5개
            val events =
                listOf(
                    event(
                        "app_open",
                        sessionId = "c1000000-0000-0000-0000-000000000001",
                        anonymousId = "b1000000-0000-0000-0000-000000000001",
                        occurredAt = "2026-10-22T10:00:01Z",
                    ),
                    event(
                        "screen_view",
                        sessionId = "c1000000-0000-0000-0000-000000000001",
                        anonymousId = "b1000000-0000-0000-0000-000000000001",
                        occurredAt = "2026-10-22T10:00:02Z",
                        properties = mapOf("screen_id" to "home"),
                    ),
                    event(
                        "screen_view",
                        sessionId = "c1000000-0000-0000-0000-000000000002",
                        anonymousId = "b1000000-0000-0000-0000-000000000001",
                        occurredAt = "2026-10-22T11:00:01Z",
                        properties = mapOf("screen_id" to "detail"),
                    ),
                    event(
                        "app_open",
                        sessionId = "c2000000-0000-0000-0000-000000000001",
                        anonymousId = "b2000000-0000-0000-0000-000000000001",
                        occurredAt = "2026-10-22T10:00:01Z",
                    ),
                    event(
                        "screen_view",
                        sessionId = "c2000000-0000-0000-0000-000000000001",
                        anonymousId = "b2000000-0000-0000-0000-000000000001",
                        occurredAt = "2026-10-22T10:00:02Z",
                        properties = mapOf("screen_id" to "home"),
                    ),
                )

            plain.ingestEvents(proj.androidKey, events)

            val overview =
                admin
                    .get(
                        "/admin/projects/${proj.projectId}/analytics/overview?$dateParams",
                    ).body<Wrap<OverviewResp>>()
                    .data

            overview.totalEvents shouldBe 5
            overview.totalSessions shouldBe 3
            overview.totalUsers shouldBe 2
        }

    @Test
    fun `Screen 통계 정확성`() =
        testApplication {
            setupTestApp()
            val admin = createAuthenticatedClient()
            val plain = createTestClient()

            val org =
                admin
                    .post("/admin/organizations") {
                        contentType(ContentType.Application.Json)
                        setBody("""{"name":"화면 통계 조직"}""")
                    }.body<Wrap<OrgResp>>()
                    .data

            val proj = setupProject(admin, org.id, "화면 통계 프로젝트")

            val events =
                listOf(
                    event(
                        "screen_view",
                        anonymousId = "b1000000-0000-0000-0000-000000000001",
                        occurredAt = "2026-10-22T10:00:01Z",
                        properties = mapOf("screen_id" to "home"),
                    ),
                    event(
                        "screen_view",
                        anonymousId = "b1000000-0000-0000-0000-000000000001",
                        occurredAt = "2026-10-22T10:00:02Z",
                        properties = mapOf("screen_id" to "home"),
                    ),
                    event(
                        "screen_view",
                        anonymousId = "b2000000-0000-0000-0000-000000000001",
                        occurredAt = "2026-10-22T10:00:03Z",
                        properties = mapOf("screen_id" to "home"),
                    ),
                    event(
                        "screen_view",
                        anonymousId = "b1000000-0000-0000-0000-000000000001",
                        occurredAt = "2026-10-22T10:00:04Z",
                        properties = mapOf("screen_id" to "detail"),
                    ),
                )

            plain.ingestEvents(proj.androidKey, events)

            val screens =
                admin
                    .get(
                        "/admin/projects/${proj.projectId}/analytics/screens?$dateParams",
                    ).body<Wrap<ScreenStatsResp>>()
                    .data

            val home = screens.screens.find { it.screenId == "home" }!!
            home.viewCount shouldBe 3
            home.uniqueUsers shouldBe 2

            val detail = screens.screens.find { it.screenId == "detail" }!!
            detail.viewCount shouldBe 1
            detail.uniqueUsers shouldBe 1
        }

    @Suppress("LongMethod")
    @Test
    fun `Funnel 전환율 계산 정확성`() =
        testApplication {
            setupTestApp()
            val admin = createAuthenticatedClient()
            val plain = createTestClient()

            val org =
                admin
                    .post("/admin/organizations") {
                        contentType(ContentType.Application.Json)
                        setBody("""{"name":"퍼널 조직"}""")
                    }.body<Wrap<OrgResp>>()
                    .data

            val proj = setupProject(admin, org.id, "퍼널 프로젝트")

            // 3명: 전원 screen_view → 2명 product_view → 1명 purchase
            val events =
                listOf(
                    // 사용자 1: 전체 퍼널
                    event(
                        "screen_view",
                        sessionId = "cf000001-0000-0000-0000-000000000001",
                        anonymousId = "bf000001-0000-0000-0000-000000000001",
                        occurredAt = "2026-10-22T10:00:01Z",
                    ),
                    event(
                        "product_view",
                        sessionId = "cf000001-0000-0000-0000-000000000001",
                        anonymousId = "bf000001-0000-0000-0000-000000000001",
                        occurredAt = "2026-10-22T10:00:02Z",
                    ),
                    event(
                        "purchase",
                        sessionId = "cf000001-0000-0000-0000-000000000001",
                        anonymousId = "bf000001-0000-0000-0000-000000000001",
                        occurredAt = "2026-10-22T10:00:03Z",
                    ),
                    // 사용자 2: screen_view + product_view
                    event(
                        "screen_view",
                        sessionId = "cf000002-0000-0000-0000-000000000001",
                        anonymousId = "bf000002-0000-0000-0000-000000000001",
                        occurredAt = "2026-10-22T10:00:01Z",
                    ),
                    event(
                        "product_view",
                        sessionId = "cf000002-0000-0000-0000-000000000001",
                        anonymousId = "bf000002-0000-0000-0000-000000000001",
                        occurredAt = "2026-10-22T10:00:02Z",
                    ),
                    // 사용자 3: screen_view만
                    event(
                        "screen_view",
                        sessionId = "cf000003-0000-0000-0000-000000000001",
                        anonymousId = "bf000003-0000-0000-0000-000000000001",
                        occurredAt = "2026-10-22T10:00:01Z",
                    ),
                )

            plain.ingestEvents(proj.androidKey, events)

            val funnel =
                admin
                    .post("/admin/projects/${proj.projectId}/analytics/funnels/query") {
                        contentType(ContentType.Application.Json)
                        setBody(
                            """{"start_date":"2026-10-22T00:00:00Z",""" +
                                """"end_date":"2026-10-22T23:59:59Z",""" +
                                """"steps":[{"event_key":"screen_view"},""" +
                                """{"event_key":"product_view"},""" +
                                """{"event_key":"purchase"}]}""",
                        )
                    }.body<Wrap<FunnelResp>>()
                    .data

            funnel.steps[0].uniqueUsers shouldBe 3 // screen_view: 3명
            funnel.steps[1].uniqueUsers shouldBe 2 // product_view: 2명
            funnel.steps[2].uniqueUsers shouldBe 1 // purchase: 1명

            // 전체 전환율: 1/3
            (funnel.overallConversionRate * 100).toInt() shouldBe 33
        }
}
