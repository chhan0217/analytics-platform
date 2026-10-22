package io.github.chaevi.analytics.backend.auth

import io.github.chaevi.analytics.backend.common.ApiException
import io.github.oshai.kotlinlogging.KotlinLogging
import io.ktor.http.HttpStatusCode
import kotlinx.datetime.Clock
import org.jetbrains.exposed.sql.insert
import org.jetbrains.exposed.sql.selectAll
import org.jetbrains.exposed.sql.transactions.transaction
import java.util.UUID

private val logger = KotlinLogging.logger {}

class AuthService {
    fun login(request: LoginRequest): LoginResponse {
        val user =
            transaction {
                AdminUserTable
                    .selectAll()
                    .where { AdminUserTable.email eq request.email }
                    .singleOrNull()
            } ?: throw ApiException("UNAUTHORIZED", HttpStatusCode.Unauthorized, "이메일 또는 비밀번호가 올바르지 않습니다")

        val passwordHash = user[AdminUserTable.passwordHash]
        if (!PasswordUtil.verify(request.password, passwordHash)) {
            throw ApiException("UNAUTHORIZED", HttpStatusCode.Unauthorized, "이메일 또는 비밀번호가 올바르지 않습니다")
        }

        val adminUserId = user[AdminUserTable.id].value.toString()
        val token = JwtConfig.generateToken(adminUserId)

        return LoginResponse(
            token = token,
            adminUser =
                AdminUserResponse(
                    id = adminUserId,
                    email = user[AdminUserTable.email],
                    name = user[AdminUserTable.name],
                ),
        )
    }

    fun seedDefaultAdmin() {
        transaction {
            val exists =
                AdminUserTable
                    .selectAll()
                    .where { AdminUserTable.email eq "admin@analytics.local" }
                    .count() > 0

            if (!exists) {
                AdminUserTable.insert {
                    it[id] = UUID.randomUUID()
                    it[email] = "admin@analytics.local"
                    it[passwordHash] = PasswordUtil.hash("admin1234")
                    it[name] = "관리자"
                    it[createdAt] = Clock.System.now()
                }
                logger.info { "기본 관리자 계정 생성 완료: admin@analytics.local / admin1234" }
            }
        }
    }
}
