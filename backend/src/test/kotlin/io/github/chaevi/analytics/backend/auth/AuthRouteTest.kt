package io.github.chaevi.analytics.backend.auth

import io.github.chaevi.analytics.backend.common.createTestClient
import io.github.chaevi.analytics.backend.common.setupTestApp
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldNotBeBlank
import io.ktor.client.call.body
import io.ktor.client.request.bearerAuth
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
data class AuthLoginWrap(
    val data: AuthLoginData,
)

@Serializable
data class AuthLoginData(
    val token: String,
    val adminUser: AdminUserResponse,
)

class AuthRouteTest {
    @Test
    fun `로그인 성공`() =
        testApplication {
            setupTestApp()
            val client = createTestClient()

            val response =
                client.post("/admin/auth/login") {
                    contentType(ContentType.Application.Json)
                    setBody(LoginRequest(email = "admin@analytics.local", password = "admin1234"))
                }

            response.status shouldBe HttpStatusCode.OK
            val body = response.body<AuthLoginWrap>()
            body.data.token.shouldNotBeBlank()
            body.data.adminUser.email shouldBe "admin@analytics.local"
            body.data.adminUser.name shouldBe "관리자"
        }

    @Test
    fun `잘못된 비밀번호로 로그인 시 401`() =
        testApplication {
            setupTestApp()
            val client = createTestClient()

            val response =
                client.post("/admin/auth/login") {
                    contentType(ContentType.Application.Json)
                    setBody(LoginRequest(email = "admin@analytics.local", password = "wrong"))
                }

            response.status shouldBe HttpStatusCode.Unauthorized
        }

    @Test
    fun `인증 없이 관리자 API 접근 시 401`() =
        testApplication {
            setupTestApp()
            val client = createTestClient()

            val response = client.get("/admin/organizations")
            response.status shouldBe HttpStatusCode.Unauthorized
        }

    @Test
    fun `유효한 토큰으로 관리자 API 접근 성공`() =
        testApplication {
            setupTestApp()
            val client = createTestClient()

            val loginResponse =
                client
                    .post("/admin/auth/login") {
                        contentType(ContentType.Application.Json)
                        setBody(LoginRequest(email = "admin@analytics.local", password = "admin1234"))
                    }.body<AuthLoginWrap>()

            val response =
                client.get("/admin/organizations") {
                    bearerAuth(loginResponse.data.token)
                }

            response.status shouldBe HttpStatusCode.OK
        }
}
