package io.github.chaevi.analytics.backend.organization

import io.github.chaevi.analytics.backend.common.createAuthenticatedClient
import io.github.chaevi.analytics.backend.common.setupTestApp
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import io.kotest.matchers.string.shouldNotBeBlank
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
data class OrgDataResponse(
    val data: OrganizationResponse,
)

@Serializable
data class OrgListResponse(
    val data: List<OrganizationResponse>,
    val meta: OrgPageMeta,
)

@Serializable
data class OrgPageMeta(
    val total: Long,
    val page: Int,
    val pageSize: Int,
)

class OrganizationRouteTest {
    @Test
    fun `조직 생성 성공`() =
        testApplication {
            setupTestApp()
            val client = createAuthenticatedClient()

            val response =
                client.post("/admin/organizations") {
                    contentType(ContentType.Application.Json)
                    setBody(CreateOrganizationRequest(name = "테스트 조직"))
                }

            response.status shouldBe HttpStatusCode.Created
            val body = response.body<OrgDataResponse>()
            body.data.name shouldBe "테스트 조직"
            body.data.id.shouldNotBeBlank()
            body.data.createdAt.shouldNotBeBlank()
        }

    @Test
    fun `조직 목록 조회`() =
        testApplication {
            setupTestApp()
            val client = createAuthenticatedClient()

            client.post("/admin/organizations") {
                contentType(ContentType.Application.Json)
                setBody(CreateOrganizationRequest(name = "조직 A"))
            }
            client.post("/admin/organizations") {
                contentType(ContentType.Application.Json)
                setBody(CreateOrganizationRequest(name = "조직 B"))
            }

            val response = client.get("/admin/organizations")
            response.status shouldBe HttpStatusCode.OK

            val body = response.body<OrgListResponse>()
            body.data.size shouldBe 2
            body.meta.total shouldBe 2
        }

    @Test
    fun `조직 상세 조회`() =
        testApplication {
            setupTestApp()
            val client = createAuthenticatedClient()

            val created =
                client
                    .post("/admin/organizations") {
                        contentType(ContentType.Application.Json)
                        setBody(CreateOrganizationRequest(name = "조회용 조직"))
                    }.body<OrgDataResponse>()

            val response = client.get("/admin/organizations/${created.data.id}")
            response.status shouldBe HttpStatusCode.OK

            val body = response.body<OrgDataResponse>()
            body.data.id shouldBe created.data.id
            body.data.name shouldBe "조회용 조직"
        }

    @Test
    fun `존재하지 않는 조직 조회 시 404`() =
        testApplication {
            setupTestApp()
            val client = createAuthenticatedClient()

            val response = client.get("/admin/organizations/00000000-0000-0000-0000-000000000000")
            response.status shouldBe HttpStatusCode.NotFound
        }

    @Test
    fun `조직 수정 성공`() =
        testApplication {
            setupTestApp()
            val client = createAuthenticatedClient()

            val created =
                client
                    .post("/admin/organizations") {
                        contentType(ContentType.Application.Json)
                        setBody(CreateOrganizationRequest(name = "수정 전"))
                    }.body<OrgDataResponse>()

            val response =
                client.patch("/admin/organizations/${created.data.id}") {
                    contentType(ContentType.Application.Json)
                    setBody(UpdateOrganizationRequest(name = "수정 후"))
                }

            response.status shouldBe HttpStatusCode.OK
            val body = response.body<OrgDataResponse>()
            body.data.name shouldBe "수정 후"
        }

    @Test
    fun `빈 이름으로 생성 시 400`() =
        testApplication {
            setupTestApp()
            val client = createAuthenticatedClient()

            val response =
                client.post("/admin/organizations") {
                    contentType(ContentType.Application.Json)
                    setBody(CreateOrganizationRequest(name = ""))
                }

            response.status shouldNotBe HttpStatusCode.Created
        }
}
