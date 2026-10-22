package io.github.chaevi.analytics.backend.taxonomy

import io.github.chaevi.analytics.backend.common.ApiException
import io.ktor.http.HttpStatusCode
import org.jetbrains.exposed.sql.transactions.transaction
import java.util.UUID

class TaxonomyPropertyService(
    private val repository: TaxonomyPropertyRepository = TaxonomyPropertyRepository(),
    private val eventRepository: TaxonomyEventRepository = TaxonomyEventRepository(),
) {
    @Suppress("ThrowsCount")
    fun create(
        projectId: String,
        eventId: String,
        request: CreateTaxonomyPropertyRequest,
    ): TaxonomyPropertyResponse {
        val eventUuid = parseUuid(eventId)

        require(request.propertyKey.isNotBlank()) {
            throw ApiException("VALIDATION_ERROR", HttpStatusCode.BadRequest, "속성 키는 필수입니다")
        }

        PropertyValueType.fromString(request.valueType)
            ?: throw ApiException(
                "VALIDATION_ERROR",
                HttpStatusCode.BadRequest,
                "유효하지 않은 값 타입입니다: ${request.valueType} (허용: STRING, INTEGER, FLOAT, BOOLEAN, ARRAY, OBJECT)",
            )

        return transaction {
            eventRepository.findById(eventUuid)
                ?: throw ApiException("NOT_FOUND", HttpStatusCode.NotFound, "이벤트를 찾을 수 없습니다: $eventId")

            val existing = repository.findByEventAndKey(eventUuid, request.propertyKey)
            if (existing != null) {
                throw ApiException(
                    "DUPLICATE_PROPERTY_KEY",
                    HttpStatusCode.Conflict,
                    "속성 키가 이미 존재합니다: ${request.propertyKey}",
                )
            }

            repository.create(
                eventUuid,
                request.propertyKey,
                request.valueType.uppercase(),
                request.required,
                request.description?.trim(),
            )
        }
    }

    fun findAllByEvent(
        projectId: String,
        eventId: String,
    ): List<TaxonomyPropertyResponse> {
        val eventUuid = parseUuid(eventId)
        return transaction { repository.findAllByEvent(eventUuid) }
    }

    fun update(
        projectId: String,
        eventId: String,
        propertyId: String,
        request: UpdateTaxonomyPropertyRequest,
    ): TaxonomyPropertyResponse {
        val propertyUuid = parseUuid(propertyId)

        transaction {
            repository.update(propertyUuid, request.required, request.description?.trim())
        }

        val properties = findAllByEvent(projectId, eventId)
        return properties.find { it.id == propertyId }
            ?: throw ApiException("NOT_FOUND", HttpStatusCode.NotFound, "속성을 찾을 수 없습니다: $propertyId")
    }

    private fun parseUuid(id: String): UUID =
        try {
            UUID.fromString(id)
        } catch (e: IllegalArgumentException) {
            throw ApiException("VALIDATION_ERROR", HttpStatusCode.BadRequest, "유효하지 않은 ID 형식입니다: $id")
        }
}
