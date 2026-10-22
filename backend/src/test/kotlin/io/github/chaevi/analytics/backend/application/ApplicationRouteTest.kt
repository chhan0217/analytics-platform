package io.github.chaevi.analytics.backend.application

import io.github.chaevi.analytics.backend.common.createAuthenticatedClient
import io.github.chaevi.analytics.backend.common.setupTestApp
import io.github.chaevi.analytics.backend.organization.CreateOrganizationRequest
import io.github.chaevi.analytics.backend.organization.OrganizationResponse
import io.github.chaevi.analytics.backend.project.CreateProjectRequest
import io.github.chaevi.analytics.backend.project.ProjectResponse
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
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
data class OrgWrap(
    val data: OrganizationResponse,
)

@Serializable
data class ProjWrap(
    val data: ProjectResponse,
)

@Serializable
data class AppWrap(
    val data: ApplicationResponse,
)

@Serializable
data class AppList(
    val data: List<ApplicationResponse>,
    val meta: AppPageMeta,
)

@Serializable
data class AppPageMeta(
    val total: Long,
    val page: Int,
    val pageSize: Int,
)

class ApplicationRouteTest {
    private suspend fun createOrgAndProject(client: HttpClient): ProjectResponse {
        val org =
            client
                .post("/admin/organizations") {
                    contentType(ContentType.Application.Json)
                    setBody(CreateOrganizationRequest(name = "테스트 조직"))
                }.body<OrgWrap>()
                .data

        return client
            .post("/admin/organizations/${org.id}/projects") {
                contentType(ContentType.Application.Json)
                setBody(CreateProjectRequest(name = "테스트 프로젝트"))
            }.body<ProjWrap>()
            .data
    }

    @Test
    fun `앱 등록 성공`() =
        testApplication {
            setupTestApp()
            val client = createAuthenticatedClient()
            val project = createOrgAndProject(client)

            val response =
                client.post("/admin/projects/${project.id}/applications") {
                    contentType(ContentType.Application.Json)
                    setBody(
                        CreateApplicationRequest(
                            name = "Android 앱",
                            platform = "ANDROID",
                            appIdentifier = "com.test.app",
                        ),
                    )
                }

            response.status shouldBe HttpStatusCode.Created
            val body = response.body<AppWrap>()
            body.data.name shouldBe "Android 앱"
            body.data.platform shouldBe "ANDROID"
            body.data.appIdentifier shouldBe "com.test.app"
            body.data.projectId shouldBe project.id
            body.data.id.shouldNotBeBlank()
        }

    @Test
    fun `잘못된 플랫폼으로 등록 시 400`() =
        testApplication {
            setupTestApp()
            val client = createAuthenticatedClient()
            val project = createOrgAndProject(client)

            val response =
                client.post("/admin/projects/${project.id}/applications") {
                    contentType(ContentType.Application.Json)
                    setBody(CreateApplicationRequest(name = "잘못된 앱", platform = "WINDOWS"))
                }

            response.status shouldBe HttpStatusCode.BadRequest
        }

    @Test
    fun `존재하지 않는 프로젝트에 앱 등록 시 404`() =
        testApplication {
            setupTestApp()
            val client = createAuthenticatedClient()

            val response =
                client.post("/admin/projects/00000000-0000-0000-0000-000000000000/applications") {
                    contentType(ContentType.Application.Json)
                    setBody(CreateApplicationRequest(name = "실패 앱", platform = "WEB"))
                }

            response.status shouldBe HttpStatusCode.NotFound
        }

    @Test
    fun `프로젝트별 앱 목록 조회`() =
        testApplication {
            setupTestApp()
            val client = createAuthenticatedClient()
            val project = createOrgAndProject(client)

            client.post("/admin/projects/${project.id}/applications") {
                contentType(ContentType.Application.Json)
                setBody(CreateApplicationRequest(name = "Android", platform = "ANDROID"))
            }
            client.post("/admin/projects/${project.id}/applications") {
                contentType(ContentType.Application.Json)
                setBody(CreateApplicationRequest(name = "iOS", platform = "IOS"))
            }
            client.post("/admin/projects/${project.id}/applications") {
                contentType(ContentType.Application.Json)
                setBody(CreateApplicationRequest(name = "Web", platform = "WEB"))
            }

            val response = client.get("/admin/projects/${project.id}/applications")
            response.status shouldBe HttpStatusCode.OK

            val body = response.body<AppList>()
            body.data.size shouldBe 3
            body.meta.total shouldBe 3
        }

    @Test
    fun `앱 상세 조회`() =
        testApplication {
            setupTestApp()
            val client = createAuthenticatedClient()
            val project = createOrgAndProject(client)

            val created =
                client
                    .post("/admin/projects/${project.id}/applications") {
                        contentType(ContentType.Application.Json)
                        setBody(CreateApplicationRequest(name = "조회용", platform = "WEB"))
                    }.body<AppWrap>()

            val response = client.get("/admin/projects/${project.id}/applications/${created.data.id}")
            response.status shouldBe HttpStatusCode.OK
            response.body<AppWrap>().data.name shouldBe "조회용"
        }

    @Test
    fun `앱 수정 성공 (platform 변경 불가 확인)`() =
        testApplication {
            setupTestApp()
            val client = createAuthenticatedClient()
            val project = createOrgAndProject(client)

            val created =
                client
                    .post("/admin/projects/${project.id}/applications") {
                        contentType(ContentType.Application.Json)
                        setBody(CreateApplicationRequest(name = "수정 전", platform = "ANDROID"))
                    }.body<AppWrap>()

            val response =
                client.patch("/admin/projects/${project.id}/applications/${created.data.id}") {
                    contentType(ContentType.Application.Json)
                    setBody(UpdateApplicationRequest(name = "수정 후"))
                }

            response.status shouldBe HttpStatusCode.OK
            val body = response.body<AppWrap>()
            body.data.name shouldBe "수정 후"
            body.data.platform shouldBe "ANDROID"
        }

    @Test
    fun `같은 플랫폼 앱 여러 개 등록 가능`() =
        testApplication {
            setupTestApp()
            val client = createAuthenticatedClient()
            val project = createOrgAndProject(client)

            val r1 =
                client.post("/admin/projects/${project.id}/applications") {
                    contentType(ContentType.Application.Json)
                    setBody(CreateApplicationRequest(name = "Web 1", platform = "WEB", appIdentifier = "a.com"))
                }
            val r2 =
                client.post("/admin/projects/${project.id}/applications") {
                    contentType(ContentType.Application.Json)
                    setBody(CreateApplicationRequest(name = "Web 2", platform = "WEB", appIdentifier = "b.com"))
                }

            r1.status shouldBe HttpStatusCode.Created
            r2.status shouldBe HttpStatusCode.Created
            r1.body<AppWrap>().data.id shouldNotBe r2.body<AppWrap>().data.id
        }
}
