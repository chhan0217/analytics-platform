package io.github.chaevi.analytics.backend.project

import io.github.chaevi.analytics.backend.common.ApiException
import io.github.chaevi.analytics.backend.organization.OrganizationRepository
import io.github.chaevi.analytics.backend.taxonomy.TaxonomyEventService
import io.ktor.http.HttpStatusCode
import org.jetbrains.exposed.sql.transactions.transaction
import java.util.UUID

class ProjectService(
    private val repository: ProjectRepository = ProjectRepository(),
    private val organizationRepository: OrganizationRepository = OrganizationRepository(),
    private val taxonomyEventService: TaxonomyEventService = TaxonomyEventService(),
) {
    companion object {
        private val VALID_POLICIES = setOf("PERMISSIVE", "STRICT", "QUARANTINE")
    }

    fun create(
        organizationId: String,
        request: CreateProjectRequest,
    ): ProjectResponse {
        val orgUuid = parseUuid(organizationId)

        require(request.name.isNotBlank()) {
            throw ApiException("VALIDATION_ERROR", HttpStatusCode.BadRequest, "프로젝트명은 필수입니다")
        }

        val project =
            transaction {
                organizationRepository.findById(orgUuid)
                    ?: throw ApiException("NOT_FOUND", HttpStatusCode.NotFound, "조직을 찾을 수 없습니다: $organizationId")

                repository.create(orgUuid, request.name.trim())
            }

        taxonomyEventService.seedSystemEvents(UUID.fromString(project.id))

        return project
    }

    fun findAllByOrganization(
        organizationId: String,
        page: Int,
        pageSize: Int,
    ): Pair<List<ProjectResponse>, Long> {
        val orgUuid = parseUuid(organizationId)
        return transaction { repository.findAllByOrganization(orgUuid, page, pageSize) }
    }

    fun findById(id: String): ProjectResponse {
        val uuid = parseUuid(id)
        return transaction { repository.findById(uuid) }
            ?: throw ApiException("NOT_FOUND", HttpStatusCode.NotFound, "프로젝트를 찾을 수 없습니다: $id")
    }

    @Suppress("ThrowsCount")
    fun update(
        id: String,
        request: UpdateProjectRequest,
    ): ProjectResponse {
        val uuid = parseUuid(id)

        request.name?.let {
            require(it.isNotBlank()) {
                throw ApiException("VALIDATION_ERROR", HttpStatusCode.BadRequest, "프로젝트명은 비어있을 수 없습니다")
            }
        }

        request.unregisteredEventPolicy?.let {
            if (it !in VALID_POLICIES) {
                throw ApiException(
                    "VALIDATION_ERROR",
                    HttpStatusCode.BadRequest,
                    "유효하지 않은 정책입니다: $it (허용: $VALID_POLICIES)",
                )
            }
        }

        transaction {
            repository.findById(uuid)
                ?: throw ApiException("NOT_FOUND", HttpStatusCode.NotFound, "프로젝트를 찾을 수 없습니다: $id")
            repository.update(uuid, request.name?.trim(), request.unregisteredEventPolicy)
        }

        return findById(id)
    }

    private fun parseUuid(id: String): UUID =
        try {
            UUID.fromString(id)
        } catch (e: IllegalArgumentException) {
            throw ApiException("VALIDATION_ERROR", HttpStatusCode.BadRequest, "유효하지 않은 ID 형식입니다: $id")
        }
}
