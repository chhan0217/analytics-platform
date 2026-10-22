package io.github.chaevi.analytics.backend.credential

import io.github.chaevi.analytics.backend.application.ApplicationRepository
import io.github.chaevi.analytics.backend.common.ApiException
import io.ktor.http.HttpStatusCode
import org.jetbrains.exposed.sql.transactions.transaction
import java.util.UUID

class AppCredentialService(
    private val repository: AppCredentialRepository = AppCredentialRepository(),
    private val applicationRepository: ApplicationRepository = ApplicationRepository(),
) {
    fun create(
        projectId: String,
        applicationId: String,
    ): CreateCredentialResponse {
        val projUuid = parseUuid(projectId)
        val appUuid = parseUuid(applicationId)

        val rawKey = IngestionKeyUtil.generate()
        val keyHash = IngestionKeyUtil.hash(rawKey)
        val keyPrefix = IngestionKeyUtil.prefix(rawKey)

        val credentialId =
            transaction {
                applicationRepository.findByIdAndProject(appUuid, projUuid)
                    ?: throw ApiException("NOT_FOUND", HttpStatusCode.NotFound, "앱을 찾을 수 없습니다: $applicationId")

                repository.create(appUuid, keyPrefix, keyHash)
            }

        return CreateCredentialResponse(
            id = credentialId,
            applicationId = applicationId,
            ingestionKey = rawKey,
            keyPrefix = keyPrefix,
            status = "ACTIVE",
            createdAt =
                kotlinx.datetime.Clock.System
                    .now()
                    .toString(),
        )
    }

    fun findAllByApplication(
        projectId: String,
        applicationId: String,
    ): List<CredentialResponse> {
        val projUuid = parseUuid(projectId)
        val appUuid = parseUuid(applicationId)

        return transaction {
            applicationRepository.findByIdAndProject(appUuid, projUuid)
                ?: throw ApiException("NOT_FOUND", HttpStatusCode.NotFound, "앱을 찾을 수 없습니다: $applicationId")

            repository.findAllByApplication(appUuid)
        }
    }

    fun revoke(
        projectId: String,
        applicationId: String,
        credentialId: String,
    ) {
        val projUuid = parseUuid(projectId)
        val appUuid = parseUuid(applicationId)
        val credUuid = parseUuid(credentialId)

        transaction {
            applicationRepository.findByIdAndProject(appUuid, projUuid)
                ?: throw ApiException("NOT_FOUND", HttpStatusCode.NotFound, "앱을 찾을 수 없습니다: $applicationId")

            val revoked = repository.revoke(credUuid, appUuid)
            if (!revoked) {
                throw ApiException("NOT_FOUND", HttpStatusCode.NotFound, "활성 수집 키를 찾을 수 없습니다: $credentialId")
            }
        }
    }

    private fun parseUuid(id: String): UUID =
        try {
            UUID.fromString(id)
        } catch (e: IllegalArgumentException) {
            throw ApiException("VALIDATION_ERROR", HttpStatusCode.BadRequest, "유효하지 않은 ID 형식입니다: $id")
        }
}
