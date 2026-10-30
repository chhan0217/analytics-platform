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
data class OrgP(
    val data: OrganizationResponse,
)

@Serializable
data class ProjP(
    val data: ProjectResponse,
)

@Serializable
data class PropR(
    val data: TaxonomyPropertyResponse,
)

@Serializable
data class PropListR(
    val data: List<TaxonomyPropertyResponse>,
)

class TaxonomyPropertyRouteTest {
    private suspend fun createProjectAndEvent(client: HttpClient): Pair<String, String> {
        val org =
            client
                .post("/admin/organizations") {
                    contentType(ContentType.Application.Json)
                    setBody(CreateOrganizationRequest(name = "테스트 조직"))
                }.body<OrgP>()
                .data

        val proj =
            client
                .post("/admin/organizations/${org.id}/projects") {
                    contentType(ContentType.Application.Json)
                    setBody(CreateProjectRequest(name = "테스트 프로젝트"))
                }.body<ProjP>()
                .data

        val event =
            client
                .post("/admin/projects/${proj.id}/taxonomy/events") {
                    contentType(ContentType.Application.Json)
                    setBody(CreateTaxonomyEventRequest(eventKey = "test_event", displayName = "테스트"))
                }.body<TaxEventR>()
                .data

        return Pair(proj.id, event.id)
    }

    @Test
    fun `속성 추가 성공`() =
        testApplication {
            setupTestApp()
            val client = createAuthenticatedClient()
            val (projId, eventId) = createProjectAndEvent(client)

            val response =
                client.post("/admin/projects/$projId/taxonomy/events/$eventId/properties") {
                    contentType(ContentType.Application.Json)
                    setBody(
                        CreateTaxonomyPropertyRequest(
                            propertyKey = "product_id",
                            valueType = "STRING",
                            required = true,
                            description = "상품 ID",
                        ),
                    )
                }

            response.status shouldBe HttpStatusCode.Created
            val body = response.body<PropR>()
            body.data.propertyKey shouldBe "product_id"
            body.data.valueType shouldBe "STRING"
            body.data.required shouldBe true
            body.data.description shouldBe "상품 ID"
        }

    @Test
    fun `잘못된 값 타입 시 400`() =
        testApplication {
            setupTestApp()
            val client = createAuthenticatedClient()
            val (projId, eventId) = createProjectAndEvent(client)

            val response =
                client.post("/admin/projects/$projId/taxonomy/events/$eventId/properties") {
                    contentType(ContentType.Application.Json)
                    setBody(CreateTaxonomyPropertyRequest(propertyKey = "field", valueType = "DATE"))
                }

            response.status shouldBe HttpStatusCode.BadRequest
        }

    @Test
    fun `중복 속성 키 시 409`() =
        testApplication {
            setupTestApp()
            val client = createAuthenticatedClient()
            val (projId, eventId) = createProjectAndEvent(client)

            client.post("/admin/projects/$projId/taxonomy/events/$eventId/properties") {
                contentType(ContentType.Application.Json)
                setBody(CreateTaxonomyPropertyRequest(propertyKey = "dup_key", valueType = "STRING"))
            }

            val response =
                client.post("/admin/projects/$projId/taxonomy/events/$eventId/properties") {
                    contentType(ContentType.Application.Json)
                    setBody(CreateTaxonomyPropertyRequest(propertyKey = "dup_key", valueType = "INTEGER"))
                }

            response.status shouldBe HttpStatusCode.Conflict
        }

    @Test
    fun `속성 목록 조회`() =
        testApplication {
            setupTestApp()
            val client = createAuthenticatedClient()
            val (projId, eventId) = createProjectAndEvent(client)

            client.post("/admin/projects/$projId/taxonomy/events/$eventId/properties") {
                contentType(ContentType.Application.Json)
                setBody(CreateTaxonomyPropertyRequest(propertyKey = "field_a", valueType = "STRING"))
            }
            client.post("/admin/projects/$projId/taxonomy/events/$eventId/properties") {
                contentType(ContentType.Application.Json)
                setBody(CreateTaxonomyPropertyRequest(propertyKey = "field_b", valueType = "INTEGER", required = true))
            }

            val response = client.get("/admin/projects/$projId/taxonomy/events/$eventId/properties")
            response.status shouldBe HttpStatusCode.OK

            val body = response.body<PropListR>()
            body.data.size shouldBe 2
        }

    @Test
    fun `속성 수정 성공`() =
        testApplication {
            setupTestApp()
            val client = createAuthenticatedClient()
            val (projId, eventId) = createProjectAndEvent(client)

            val created =
                client
                    .post("/admin/projects/$projId/taxonomy/events/$eventId/properties") {
                        contentType(ContentType.Application.Json)
                        setBody(CreateTaxonomyPropertyRequest(propertyKey = "editable", valueType = "BOOLEAN"))
                    }.body<PropR>()

            val response =
                client.patch("/admin/projects/$projId/taxonomy/events/$eventId/properties/${created.data.id}") {
                    contentType(ContentType.Application.Json)
                    setBody(UpdateTaxonomyPropertyRequest(required = true, description = "수정된 설명"))
                }

            response.status shouldBe HttpStatusCode.OK
            val body = response.body<PropR>()
            body.data.required shouldBe true
            body.data.description shouldBe "수정된 설명"
            body.data.valueType shouldBe "BOOLEAN"
        }
}
