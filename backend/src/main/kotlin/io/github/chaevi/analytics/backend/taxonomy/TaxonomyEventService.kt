package io.github.chaevi.analytics.backend.taxonomy

import io.github.chaevi.analytics.backend.common.ApiException
import io.github.chaevi.analytics.backend.project.ProjectRepository
import io.ktor.http.HttpStatusCode
import org.jetbrains.exposed.sql.transactions.transaction
import java.util.UUID

class TaxonomyEventService(
    private val repository: TaxonomyEventRepository = TaxonomyEventRepository(),
    private val projectRepository: ProjectRepository = ProjectRepository(),
    private val propertyRepository: TaxonomyPropertyRepository = TaxonomyPropertyRepository(),
) {
    private val eventKeyPattern = Regex("^[a-z][a-z0-9_]{0,127}$")

    @Suppress("ThrowsCount")
    fun create(
        projectId: String,
        request: CreateTaxonomyEventRequest,
    ): TaxonomyEventResponse {
        val projUuid = parseUuid(projectId)

        require(request.eventKey.isNotBlank()) {
            throw ApiException("VALIDATION_ERROR", HttpStatusCode.BadRequest, "이벤트 키는 필수입니다")
        }

        if (!eventKeyPattern.matches(request.eventKey)) {
            throw ApiException(
                "VALIDATION_ERROR",
                HttpStatusCode.BadRequest,
                "이벤트 키는 영문 소문자 snake_case여야 합니다: ${request.eventKey}",
            )
        }

        require(request.displayName.isNotBlank()) {
            throw ApiException("VALIDATION_ERROR", HttpStatusCode.BadRequest, "표시명은 필수입니다")
        }

        return transaction {
            projectRepository.findById(projUuid)
                ?: throw ApiException("NOT_FOUND", HttpStatusCode.NotFound, "프로젝트를 찾을 수 없습니다: $projectId")

            val existing = repository.findByProjectAndEventKey(projUuid, request.eventKey)
            if (existing != null) {
                throw ApiException("DUPLICATE_EVENT_KEY", HttpStatusCode.Conflict, "이벤트 키가 이미 존재합니다: ${request.eventKey}")
            }

            repository.create(projUuid, request.eventKey, request.displayName.trim(), request.description?.trim())
        }
    }

    fun findAll(
        projectId: String,
        page: Int,
        pageSize: Int,
    ): Pair<List<TaxonomyEventResponse>, Long> {
        val projUuid = parseUuid(projectId)
        return transaction { repository.findAllByProject(projUuid, page, pageSize) }
    }

    @Suppress("UnusedParameter")
    fun findById(
        projectId: String,
        eventId: String,
    ): TaxonomyEventResponse {
        val eventUuid = parseUuid(eventId)
        return transaction {
            val event =
                repository.findById(eventUuid)
                    ?: throw ApiException("NOT_FOUND", HttpStatusCode.NotFound, "이벤트를 찾을 수 없습니다: $eventId")
            val properties = propertyRepository.findAllByEvent(eventUuid)
            event.copy(properties = properties)
        }
    }

    @Suppress("ThrowsCount")
    fun update(
        projectId: String,
        eventId: String,
        request: UpdateTaxonomyEventRequest,
    ): TaxonomyEventResponse {
        val eventUuid = parseUuid(eventId)

        request.status?.let {
            TaxonomyEventStatus.fromString(it)
                ?: throw ApiException(
                    "VALIDATION_ERROR",
                    HttpStatusCode.BadRequest,
                    "유효하지 않은 상태입니다: $it (허용: ACTIVE, DEPRECATED, ARCHIVED)",
                )
        }

        transaction {
            val existing =
                repository.findById(eventUuid)
                    ?: throw ApiException("NOT_FOUND", HttpStatusCode.NotFound, "이벤트를 찾을 수 없습니다: $eventId")

            if (existing.isSystem && request.status == "ARCHIVED") {
                throw ApiException("VALIDATION_ERROR", HttpStatusCode.BadRequest, "시스템 이벤트는 ARCHIVED로 변경할 수 없습니다")
            }

            repository.update(eventUuid, request.displayName?.trim(), request.description?.trim(), request.status)
        }

        return findById(projectId, eventId)
    }

    fun seedSystemEvents(projectId: UUID) {
        transaction {
            SYSTEM_EVENTS.forEach { def ->
                val existing = repository.findByProjectAndEventKey(projectId, def.eventKey)
                if (existing == null) {
                    repository.create(projectId, def.eventKey, def.displayName, def.description, isSystem = true)
                }
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
