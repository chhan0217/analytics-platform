package io.github.chaevi.analytics.backend.project

import io.github.chaevi.analytics.backend.common.createAuthenticatedClient
import io.github.chaevi.analytics.backend.common.setupTestApp
import io.github.chaevi.analytics.backend.organization.CreateOrganizationRequest
import io.github.chaevi.analytics.backend.organization.OrganizationResponse
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldNotBeBlank
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
data class OrgData(
    val data: OrganizationResponse,
)

@Serializable
data class ProjData(
    val data: ProjectResponse,
)

@Serializable
data class ProjList(
    val data: List<ProjectResponse>,
    val meta: ProjPageMeta,
)

@Serializable
data class ProjPageMeta(
    val total: Long,
    val page: Int,
    val pageSize: Int,
)

class ProjectRouteTest {
    private suspend fun createOrg(client: HttpClient): OrganizationResponse =
        client
            .post("/admin/organizations") {
                contentType(ContentType.Application.Json)
                setBody(CreateOrganizationRequest(name = "테스트 조직"))
            }.body<OrgData>()
            .data

    @Test
    fun `프로젝트 생성 성공`() =
        testApplication {
            setupTestApp()
            val client = createAuthenticatedClient()
            val org = createOrg(client)

            val response =
                client.post("/admin/organizations/${org.id}/projects") {
                    contentType(ContentType.Application.Json)
                    setBody(CreateProjectRequest(name = "테스트 프로젝트"))
                }

            response.status shouldBe HttpStatusCode.Created
            val body = response.body<ProjData>()
            body.data.name shouldBe "테스트 프로젝트"
            body.data.organizationId shouldBe org.id
            body.data.unregisteredEventPolicy shouldBe "PERMISSIVE"
            body.data.id.shouldNotBeBlank()
        }

    @Test
    fun `존재하지 않는 조직에 프로젝트 생성 시 404`() =
        testApplication {
            setupTestApp()
            val client = createAuthenticatedClient()

            val response =
                client.post("/admin/organizations/00000000-0000-0000-0000-000000000000/projects") {
                    contentType(ContentType.Application.Json)
                    setBody(CreateProjectRequest(name = "실패 프로젝트"))
                }

            response.status shouldBe HttpStatusCode.NotFound
        }

    @Test
    fun `조직별 프로젝트 목록 조회`() =
        testApplication {
            setupTestApp()
            val client = createAuthenticatedClient()
            val org = createOrg(client)

            client.post("/admin/organizations/${org.id}/projects") {
                contentType(ContentType.Application.Json)
                setBody(CreateProjectRequest(name = "프로젝트 A"))
            }
            client.post("/admin/organizations/${org.id}/projects") {
                contentType(ContentType.Application.Json)
                setBody(CreateProjectRequest(name = "프로젝트 B"))
            }

            val response = client.get("/admin/organizations/${org.id}/projects")
            response.status shouldBe HttpStatusCode.OK

            val body = response.body<ProjList>()
            body.data.size shouldBe 2
            body.meta.total shouldBe 2
        }

    @Test
    fun `프로젝트 상세 조회`() =
        testApplication {
            setupTestApp()
            val client = createAuthenticatedClient()
            val org = createOrg(client)

            val created =
                client
                    .post("/admin/organizations/${org.id}/projects") {
                        contentType(ContentType.Application.Json)
                        setBody(CreateProjectRequest(name = "조회용"))
                    }.body<ProjData>()

            val response = client.get("/admin/projects/${created.data.id}")
            response.status shouldBe HttpStatusCode.OK
            response.body<ProjData>().data.name shouldBe "조회용"
        }

    @Test
    fun `프로젝트 수정 성공`() =
        testApplication {
            setupTestApp()
            val client = createAuthenticatedClient()
            val org = createOrg(client)

            val created =
                client
                    .post("/admin/organizations/${org.id}/projects") {
                        contentType(ContentType.Application.Json)
                        setBody(CreateProjectRequest(name = "수정 전"))
                    }.body<ProjData>()

            val response =
                client.patch("/admin/projects/${created.data.id}") {
                    contentType(ContentType.Application.Json)
                    setBody(UpdateProjectRequest(name = "수정 후", unregisteredEventPolicy = "STRICT"))
                }

            response.status shouldBe HttpStatusCode.OK
            val body = response.body<ProjData>()
            body.data.name shouldBe "수정 후"
            body.data.unregisteredEventPolicy shouldBe "STRICT"
        }

    @Test
    fun `잘못된 이벤트 정책으로 수정 시 400`() =
        testApplication {
            setupTestApp()
            val client = createAuthenticatedClient()
            val org = createOrg(client)

            val created =
                client
                    .post("/admin/organizations/${org.id}/projects") {
                        contentType(ContentType.Application.Json)
                        setBody(CreateProjectRequest(name = "정책 테스트"))
                    }.body<ProjData>()

            val response =
                client.patch("/admin/projects/${created.data.id}") {
                    contentType(ContentType.Application.Json)
                    setBody(UpdateProjectRequest(unregisteredEventPolicy = "INVALID"))
                }

            response.status shouldBe HttpStatusCode.BadRequest
        }
}
