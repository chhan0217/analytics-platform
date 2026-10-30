package io.github.chaevi.analytics.backend.application

import io.github.chaevi.analytics.backend.common.ApiException
import io.github.chaevi.analytics.backend.project.ProjectRepository
import io.ktor.http.HttpStatusCode
import org.jetbrains.exposed.sql.transactions.transaction
import java.util.UUID

class ApplicationService(
    private val repository: ApplicationRepository = ApplicationRepository(),
    private val projectRepository: ProjectRepository = ProjectRepository(),
) {
    @Suppress("ThrowsCount")
    fun create(
        projectId: String,
        request: CreateApplicationRequest,
    ): ApplicationResponse {
        val projUuid = parseUuid(projectId)

        require(request.name.isNotBlank()) {
            throw ApiException("VALIDATION_ERROR", HttpStatusCode.BadRequest, "앱 이름은 필수입니다")
        }

        val platform =
            Platform.fromString(request.platform)
                ?: throw ApiException(
                    "INVALID_PLATFORM",
                    HttpStatusCode.BadRequest,
                    "유효하지 않은 플랫폼입니다: ${request.platform} (허용: ANDROID, IOS, WEB)",
                )

        return transaction {
            projectRepository.findById(projUuid)
                ?: throw ApiException("NOT_FOUND", HttpStatusCode.NotFound, "프로젝트를 찾을 수 없습니다: $projectId")

            repository.create(projUuid, request.name.trim(), platform, request.appIdentifier?.trim())
        }
    }

    fun findAllByProject(
        projectId: String,
        page: Int,
        pageSize: Int,
    ): Pair<List<ApplicationResponse>, Long> {
        val projUuid = parseUuid(projectId)
        return transaction { repository.findAllByProject(projUuid, page, pageSize) }
    }

    fun findById(
        projectId: String,
        applicationId: String,
    ): ApplicationResponse {
        val projUuid = parseUuid(projectId)
        val appUuid = parseUuid(applicationId)
        return transaction { repository.findByIdAndProject(appUuid, projUuid) }
            ?: throw ApiException("NOT_FOUND", HttpStatusCode.NotFound, "앱을 찾을 수 없습니다: $applicationId")
    }

    fun update(
        projectId: String,
        applicationId: String,
        request: UpdateApplicationRequest,
    ): ApplicationResponse {
        val projUuid = parseUuid(projectId)
        val appUuid = parseUuid(applicationId)

        request.name?.let {
            require(it.isNotBlank()) {
                throw ApiException("VALIDATION_ERROR", HttpStatusCode.BadRequest, "앱 이름은 비어있을 수 없습니다")
            }
        }

        transaction {
            repository.findByIdAndProject(appUuid, projUuid)
                ?: throw ApiException("NOT_FOUND", HttpStatusCode.NotFound, "앱을 찾을 수 없습니다: $applicationId")
            repository.update(appUuid, request.name?.trim(), request.appIdentifier?.trim())
        }

        return findById(projectId, applicationId)
    }

    private fun parseUuid(id: String): UUID =
        try {
            UUID.fromString(id)
        } catch (e: IllegalArgumentException) {
            throw ApiException("VALIDATION_ERROR", HttpStatusCode.BadRequest, "유효하지 않은 ID 형식입니다: $id")
        }
}
