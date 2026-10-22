package io.github.chaevi.analytics.backend.organization

import io.github.chaevi.analytics.backend.common.ApiException
import io.ktor.http.HttpStatusCode
import org.jetbrains.exposed.sql.transactions.transaction
import java.util.UUID

class OrganizationService(
    private val repository: OrganizationRepository = OrganizationRepository(),
) {
    fun create(request: CreateOrganizationRequest): OrganizationResponse {
        require(request.name.isNotBlank()) {
            throw ApiException("VALIDATION_ERROR", HttpStatusCode.BadRequest, "조직명은 필수입니다")
        }

        return transaction { repository.create(request.name.trim()) }
    }

    fun findAll(
        page: Int,
        pageSize: Int,
    ): Pair<List<OrganizationResponse>, Long> = transaction { repository.findAll(page, pageSize) }

    fun findById(id: String): OrganizationResponse {
        val uuid = parseUuid(id)
        return transaction { repository.findById(uuid) }
            ?: throw ApiException("NOT_FOUND", HttpStatusCode.NotFound, "조직을 찾을 수 없습니다: $id")
    }

    fun update(
        id: String,
        request: UpdateOrganizationRequest,
    ): OrganizationResponse {
        val uuid = parseUuid(id)

        request.name?.let {
            require(it.isNotBlank()) {
                throw ApiException("VALIDATION_ERROR", HttpStatusCode.BadRequest, "조직명은 비어있을 수 없습니다")
            }
        }

        transaction {
            val exists =
                repository.findById(uuid)
                    ?: throw ApiException("NOT_FOUND", HttpStatusCode.NotFound, "조직을 찾을 수 없습니다: $id")
            repository.update(uuid, request.name?.trim())
            exists
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
