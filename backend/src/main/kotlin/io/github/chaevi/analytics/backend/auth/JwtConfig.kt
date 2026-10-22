package io.github.chaevi.analytics.backend.auth

import com.auth0.jwt.JWT
import com.auth0.jwt.algorithms.Algorithm
import io.ktor.server.application.Application
import io.ktor.server.auth.authentication
import io.ktor.server.auth.jwt.JWTPrincipal
import io.ktor.server.auth.jwt.jwt
import java.util.Date

object JwtConfig {
    private val secret = System.getenv("JWT_SECRET") ?: "analytics-local-dev-secret"
    private val issuer = "analytics-platform"
    private val audience = "analytics-admin"
    private val algorithm = Algorithm.HMAC256(secret)
    private val expirationMs = 24 * 60 * 60 * 1000L // 24시간

    fun generateToken(adminUserId: String): String =
        JWT
            .create()
            .withIssuer(issuer)
            .withAudience(audience)
            .withClaim("admin_user_id", adminUserId)
            .withExpiresAt(Date(System.currentTimeMillis() + expirationMs))
            .sign(algorithm)

    fun Application.configureAuth() {
        authentication {
            jwt("admin-jwt") {
                verifier(
                    JWT
                        .require(algorithm)
                        .withIssuer(issuer)
                        .withAudience(audience)
                        .build(),
                )
                validate { credential ->
                    val adminUserId = credential.payload.getClaim("admin_user_id").asString()
                    if (adminUserId != null) JWTPrincipal(credential.payload) else null
                }
            }
        }
    }
}
