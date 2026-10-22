package io.github.chaevi.analytics.backend.taxonomy

import io.github.chaevi.analytics.backend.common.createAuthenticatedClient
import io.github.chaevi.analytics.backend.common.setupTestApp
import io.github.chaevi.analytics.backend.organization.CreateOrganizationRequest
import io.github.chaevi.analytics.backend.organization.OrganizationResponse
import io.github.chaevi.analytics.backend.project.CreateProjectRequest
import io.github.chaevi.analytics.backend.project.ProjectResponse
import io.kotest.matchers.shouldBe
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.get
import io.ktor.client.request.patch
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.http.ContentType
import io.ktor.http.HttpStatusCode
import io.ktor.http.contentType
import io.ktor.server.testing.testApplication
import kotlinx.serialization.Serializable
import kotlin.test.Test

@Serializable
data class OrgR(
    val data: OrganizationResponse,
)

@Serializable
data class ProjR(
    val data: ProjectResponse,
)

@Serializable
data class TaxEventR(
    val data: TaxonomyEventResponse,
)

@Serializable
data class TaxEventListR(
    val data: List<TaxonomyEventResponse>,
    val meta: TaxPageMeta,
)

@Serializable
data class TaxPageMeta(
    val total: Long,
    val page: Int,
    val pageSize: Int,
)

class TaxonomyEventRouteTest {
    private suspend fun createProject(client: HttpClient): String {
        val org =
            client
                .post("/admin/organizations") {
                    contentType(ContentType.Application.Json)
                    setBody(CreateOrganizationRequest(name = "테스트 조직"))
                }.body<OrgR>()
                .data

        val proj =
            client
                .post("/admin/organizations/${org.id}/projects") {
                    contentType(ContentType.Application.Json)
                    setBody(CreateProjectRequest(name = "테스트 프로젝트"))
                }.body<ProjR>()
                .data

        return proj.id
    }

    @Test
    fun `프로젝트 생성 시 시스템 이벤트 6개 자동 시딩`() =
        testApplication {
            setupTestApp()
            val client = createAuthenticatedClient()
            val projectId = createProject(client)

            val response = client.get("/admin/projects/$projectId/taxonomy/events?page_size=50")
            val body = response.body<TaxEventListR>()
            body.meta.total shouldBe 6
            body.data.all { it.isSystem } shouldBe true
            body.data.map { it.eventKey }.toSet() shouldBe
                setOf("session_start", "session_end", "screen_view", "app_open", "button_click", "identify")
        }

    @Test
    fun `커스텀 이벤트 등록 성공`() =
        testApplication {
            setupTestApp()
            val client = createAuthenticatedClient()
            val projectId = createProject(client)

            val response =
                client.post("/admin/projects/$projectId/taxonomy/events") {
                    contentType(ContentType.Application.Json)
                    setBody(
                        CreateTaxonomyEventRequest(
                            eventKey = "product_view",
                            displayName = "상품 조회",
                            description = "사용자가 상품 상세 페이지를 조회할 때",
                        ),
                    )
                }

            response.status shouldBe HttpStatusCode.Created
            val body = response.body<TaxEventR>()
            body.data.eventKey shouldBe "product_view"
            body.data.displayName shouldBe "상품 조회"
            body.data.isSystem shouldBe false
            body.data.status shouldBe "ACTIVE"
        }

    @Test
    fun `중복 이벤트 키 등록 시 409`() =
        testApplication {
            setupTestApp()
            val client = createAuthenticatedClient()
            val projectId = createProject(client)

            client.post("/admin/projects/$projectId/taxonomy/events") {
                contentType(ContentType.Application.Json)
                setBody(CreateTaxonomyEventRequest(eventKey = "custom_event", displayName = "커스텀"))
            }

            val response =
                client.post("/admin/projects/$projectId/taxonomy/events") {
                    contentType(ContentType.Application.Json)
                    setBody(CreateTaxonomyEventRequest(eventKey = "custom_event", displayName = "중복"))
                }

            response.status shouldBe HttpStatusCode.Conflict
        }

    @Test
    fun `잘못된 이벤트 키 형식 시 400`() =
        testApplication {
            setupTestApp()
            val client = createAuthenticatedClient()
            val projectId = createProject(client)

            val response =
                client.post("/admin/projects/$projectId/taxonomy/events") {
                    contentType(ContentType.Application.Json)
                    setBody(CreateTaxonomyEventRequest(eventKey = "Invalid-Key", displayName = "잘못된"))
                }

            response.status shouldBe HttpStatusCode.BadRequest
        }

    @Test
    fun `이벤트 상태 변경 성공`() =
        testApplication {
            setupTestApp()
            val client = createAuthenticatedClient()
            val projectId = createProject(client)

            val created =
                client
                    .post("/admin/projects/$projectId/taxonomy/events") {
                        contentType(ContentType.Application.Json)
                        setBody(CreateTaxonomyEventRequest(eventKey = "temp_event", displayName = "임시"))
                    }.body<TaxEventR>()

            val response =
                client.patch("/admin/projects/$projectId/taxonomy/events/${created.data.id}") {
                    contentType(ContentType.Application.Json)
                    setBody(UpdateTaxonomyEventRequest(status = "DEPRECATED"))
                }

            response.status shouldBe HttpStatusCode.OK
            response.body<TaxEventR>().data.status shouldBe "DEPRECATED"
        }

    @Test
    fun `시스템 이벤트 ARCHIVED 변경 불가`() =
        testApplication {
            setupTestApp()
            val client = createAuthenticatedClient()
            val projectId = createProject(client)

            val events = client.get("/admin/projects/$projectId/taxonomy/events?page_size=50").body<TaxEventListR>()
            val systemEvent = events.data.first { it.isSystem }

            val response =
                client.patch("/admin/projects/$projectId/taxonomy/events/${systemEvent.id}") {
                    contentType(ContentType.Application.Json)
                    setBody(UpdateTaxonomyEventRequest(status = "ARCHIVED"))
                }

            response.status shouldBe HttpStatusCode.BadRequest
        }

    @Test
    fun `이벤트 키는 변경 불가 (displayName만 변경)`() =
        testApplication {
            setupTestApp()
            val client = createAuthenticatedClient()
            val projectId = createProject(client)

            val created =
                client
                    .post("/admin/projects/$projectId/taxonomy/events") {
                        contentType(ContentType.Application.Json)
                        setBody(CreateTaxonomyEventRequest(eventKey = "original_key", displayName = "원래"))
                    }.body<TaxEventR>()

            val response =
                client.patch("/admin/projects/$projectId/taxonomy/events/${created.data.id}") {
                    contentType(ContentType.Application.Json)
                    setBody(UpdateTaxonomyEventRequest(displayName = "변경된 이름"))
                }

            response.status shouldBe HttpStatusCode.OK
            val body = response.body<TaxEventR>()
            body.data.eventKey shouldBe "original_key"
            body.data.displayName shouldBe "변경된 이름"
        }
}
