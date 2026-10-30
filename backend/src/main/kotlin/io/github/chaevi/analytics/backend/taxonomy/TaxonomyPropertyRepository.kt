package io.github.chaevi.analytics.backend.taxonomy

import org.jetbrains.exposed.sql.ResultRow
import org.jetbrains.exposed.sql.and
import org.jetbrains.exposed.sql.insert
import org.jetbrains.exposed.sql.selectAll
import org.jetbrains.exposed.sql.update
import java.util.UUID

class TaxonomyPropertyRepository {
    fun create(
        taxonomyEventId: UUID,
        propertyKey: String,
        valueType: String,
        required: Boolean,
        description: String?,
    ): TaxonomyPropertyResponse {
        val id = UUID.randomUUID()

        TaxonomyPropertyTable.insert {
            it[TaxonomyPropertyTable.id] = id
            it[TaxonomyPropertyTable.taxonomyEventId] = taxonomyEventId
            it[TaxonomyPropertyTable.propertyKey] = propertyKey
            it[TaxonomyPropertyTable.valueType] = valueType
            it[TaxonomyPropertyTable.required] = required
            it[TaxonomyPropertyTable.description] = description
        }

        return TaxonomyPropertyResponse(
            id = id.toString(),
            taxonomyEventId = taxonomyEventId.toString(),
            propertyKey = propertyKey,
            valueType = valueType,
            required = required,
            description = description,
        )
    }

    fun findAllByEvent(taxonomyEventId: UUID): List<TaxonomyPropertyResponse> =
        TaxonomyPropertyTable
            .selectAll()
            .where { TaxonomyPropertyTable.taxonomyEventId eq taxonomyEventId }
            .map { it.toResponse() }

    fun findByEventAndKey(
        taxonomyEventId: UUID,
        propertyKey: String,
    ): TaxonomyPropertyResponse? =
        TaxonomyPropertyTable
            .selectAll()
            .where {
                (TaxonomyPropertyTable.taxonomyEventId eq taxonomyEventId) and
                    (TaxonomyPropertyTable.propertyKey eq propertyKey)
            }.singleOrNull()
            ?.toResponse()

    fun update(
        id: UUID,
        required: Boolean?,
        description: String?,
    ): Boolean {
        if (required == null && description == null) return false
        val updated =
            TaxonomyPropertyTable.update({ TaxonomyPropertyTable.id eq id }) {
                if (required != null) it[TaxonomyPropertyTable.required] = required
                if (description != null) it[TaxonomyPropertyTable.description] = description
            }
        return updated > 0
    }

    private fun ResultRow.toResponse(): TaxonomyPropertyResponse =
        TaxonomyPropertyResponse(
            id = this[TaxonomyPropertyTable.id].value.toString(),
            taxonomyEventId = this[TaxonomyPropertyTable.taxonomyEventId].value.toString(),
            propertyKey = this[TaxonomyPropertyTable.propertyKey],
            valueType = this[TaxonomyPropertyTable.valueType],
            required = this[TaxonomyPropertyTable.required],
            description = this[TaxonomyPropertyTable.description],
        )
}
