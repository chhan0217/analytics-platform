package io.github.chaevi.analytics.backend.common

import io.github.oshai.kotlinlogging.KotlinLogging
import io.ktor.http.HttpStatusCode
import io.ktor.server.application.Application
import io.ktor.server.application.install
import io.ktor.server.plugins.statuspages.StatusPages
import io.ktor.server.response.respond
import kotlinx.serialization.Serializable

private val logger = KotlinLogging.logger {}

class ApiException(
    val code: String,
    val status: HttpStatusCode,
    override val message: String,
) : RuntimeException(message)

@Serializable
data class ErrorDetail(
    val code: String,
    val message: String,
)

@Serializable
data class ErrorResponse(
    val success: Boolean = false,
    val error: ErrorDetail,
)

fun Application.configureStatusPages() {
    install(StatusPages) {
        exception<ApiException> { call, cause ->
            call.respond(
                cause.status,
                ErrorResponse(error = ErrorDetail(cause.code, cause.message)),
            )
        }
        exception<Exception> { call, cause ->
            logger.error(cause) { "처리되지 않은 예외 발생" }
            call.respond(
                HttpStatusCode.InternalServerError,
                ErrorResponse(error = ErrorDetail("INTERNAL_ERROR", "서버 내부 오류가 발생했습니다")),
            )
        }
    }
}
