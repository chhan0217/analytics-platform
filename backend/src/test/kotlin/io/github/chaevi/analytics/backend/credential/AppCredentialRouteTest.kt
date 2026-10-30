package io.github.chaevi.analytics.backend.credential

import io.github.chaevi.analytics.backend.application.ApplicationResponse
import io.github.chaevi.analytics.backend.application.CreateApplicationRequest
import io.github.chaevi.analytics.backend.common.createAuthenticatedClient
import io.github.chaevi.analytics.backend.common.setupTestApp
import io.github.chaevi.analytics.backend.organization.CreateOrganizationRequest
import io.github.chaevi.analytics.backend.organization.OrganizationResponse
import io.github.chaevi.analytics.backend.project.CreateProjectRequest
import io.github.chaevi.analytics.backend.project.ProjectResponse
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldStartWith
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.delete
import io.ktor.client.request.get
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.http.ContentType
import io.ktor.http.HttpStatusCode
import io.ktor.http.contentType
import io.ktor.server.testing.testApplication
import kotlinx.serialization.Serializable
import kotlin.test.Test

@Serializable
data class OrgW(
    val data: OrganizationResponse,
)

@Serializable
data class ProjW(
    val data: ProjectResponse,
)

@Serializable
data class AppW(
    val data: ApplicationResponse,
)

@Serializable
data class CredCreateW(
    val data: CreateCredentialResponse,
)

@Serializable
data class CredListW(
    val data: List<CredentialResponse>,
)

class AppCredentialRouteTest {
    private suspend fun createApp(client: HttpClient): Pair<String, String> {
        val org =
            client
                .post("/admin/organizations") {
                    contentType(ContentType.Application.Json)
                    setBody(CreateOrganizationRequest(name = "테스트 조직"))
                }.body<OrgW>()
                .data

        val proj =
            client
                .post("/admin/organizations/${org.id}/projects") {
                    contentType(ContentType.Application.Json)
                    setBody(CreateProjectRequest(name = "테스트 프로젝트"))
                }.body<ProjW>()
                .data

        val app =
            client
                .post("/admin/projects/${proj.id}/applications") {
                    contentType(ContentType.Application.Json)
                    setBody(CreateApplicationRequest(name = "Android", platform = "ANDROID"))
                }.body<AppW>()
                .data

        return Pair(proj.id, app.id)
    }

    @Test
    fun `수집 키 발급 성공`() =
        testApplication {
            setupTestApp()
            val client = createAuthenticatedClient()
            val (projId, appId) = createApp(client)

            val response =
                client.post("/admin/projects/$projId/applications/$appId/credentials")

            response.status shouldBe HttpStatusCode.Created
            val body = response.body<CredCreateW>()
            body.data.ingestionKey shouldStartWith "ink_"
            body.data.ingestionKey.length shouldBe 68
            body.data.keyPrefix shouldStartWith "ink_"
            body.data.status shouldBe "ACTIVE"
            body.data.applicationId shouldBe appId
        }

    @Test
    fun `수집 키 목록 조회 (원본 키 미노출)`() =
        testApplication {
            setupTestApp()
            val client = createAuthenticatedClient()
            val (projId, appId) = createApp(client)

            client.post("/admin/projects/$projId/applications/$appId/credentials")
            client.post("/admin/projects/$projId/applications/$appId/credentials")

            val response = client.get("/admin/projects/$projId/applications/$appId/credentials")
            response.status shouldBe HttpStatusCode.OK

            val body = response.body<CredListW>()
            body.data.size shouldBe 2
            body.data.forEach {
                it.keyPrefix shouldStartWith "ink_"
                it.status shouldBe "ACTIVE"
            }
        }

    @Test
    fun `수집 키 폐기 성공`() =
        testApplication {
            setupTestApp()
            val client = createAuthenticatedClient()
            val (projId, appId) = createApp(client)

            val created =
                client
                    .post("/admin/projects/$projId/applications/$appId/credentials")
                    .body<CredCreateW>()

            val response =
                client.delete("/admin/projects/$projId/applications/$appId/credentials/${created.data.id}")
            response.status shouldBe HttpStatusCode.NoContent

            val list = client.get("/admin/projects/$projId/applications/$appId/credentials").body<CredListW>()
            val revoked = list.data.find { it.id == created.data.id }
            revoked?.status shouldBe "REVOKED"
        }

    @Test
    fun `이미 폐기된 키 재폐기 시 404`() =
        testApplication {
            setupTestApp()
            val client = createAuthenticatedClient()
            val (projId, appId) = createApp(client)

            val created =
                client
                    .post("/admin/projects/$projId/applications/$appId/credentials")
                    .body<CredCreateW>()

            client.delete("/admin/projects/$projId/applications/$appId/credentials/${created.data.id}")

            val response =
                client.delete("/admin/projects/$projId/applications/$appId/credentials/${created.data.id}")
            response.status shouldBe HttpStatusCode.NotFound
        }
}
