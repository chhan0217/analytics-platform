package io.github.chaevi.analytics.backend.auth

import io.github.chaevi.analytics.backend.common.ApiException
import io.ktor.http.HttpStatusCode
import io.ktor.server.auth.jwt.JWTPrincipal
import io.ktor.server.auth.principal
import io.ktor.server.routing.RoutingCall

fun RoutingCall.adminUserId(): String =
    principal<JWTPrincipal>()
        ?.payload
        ?.getClaim("admin_user_id")
        ?.asString()
        ?: throw ApiException("UNAUTHORIZED", HttpStatusCode.Unauthorized, "인증이 필요합니다")
