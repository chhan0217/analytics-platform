package io.github.chaevi.analytics.backend.taxonomy

import kotlinx.datetime.Clock
import org.jetbrains.exposed.sql.ResultRow
import org.jetbrains.exposed.sql.and
import org.jetbrains.exposed.sql.insert
import org.jetbrains.exposed.sql.selectAll
import org.jetbrains.exposed.sql.update
import java.util.UUID

class TaxonomyEventRepository {
    fun create(
        projectId: UUID,
        eventKey: String,
        displayName: String,
        description: String?,
        isSystem: Boolean = false,
    ): TaxonomyEventResponse {
        val now = Clock.System.now()
        val id = UUID.randomUUID()

        TaxonomyEventTable.insert {
            it[TaxonomyEventTable.id] = id
            it[TaxonomyEventTable.projectId] = projectId
            it[TaxonomyEventTable.eventKey] = eventKey
            it[TaxonomyEventTable.displayName] = displayName
            it[TaxonomyEventTable.description] = description
            it[TaxonomyEventTable.isSystem] = isSystem
            it[createdAt] = now
        }

        return TaxonomyEventResponse(
            id = id.toString(),
            projectId = projectId.toString(),
            eventKey = eventKey,
            displayName = displayName,
            description = description,
            schemaVersion = 1,
            status = "ACTIVE",
            isSystem = isSystem,
            createdAt = now.toString(),
        )
    }

    fun findAllByProject(
        projectId: UUID,
        page: Int,
        pageSize: Int,
    ): Pair<List<TaxonomyEventResponse>, Long> {
        val query = TaxonomyEventTable.selectAll().where { TaxonomyEventTable.projectId eq projectId }
        val total = query.count()
        val rows =
            query
                .orderBy(TaxonomyEventTable.createdAt)
                .limit(pageSize)
                .offset(((page - 1) * pageSize).toLong())
                .map { it.toResponse() }

        return Pair(rows, total)
    }

    fun findById(id: UUID): TaxonomyEventResponse? =
        TaxonomyEventTable
            .selectAll()
            .where { TaxonomyEventTable.id eq id }
            .singleOrNull()
            ?.toResponse()

    fun findByProjectAndEventKey(
        projectId: UUID,
        eventKey: String,
    ): TaxonomyEventResponse? =
        TaxonomyEventTable
            .selectAll()
            .where {
                (TaxonomyEventTable.projectId eq projectId) and
                    (TaxonomyEventTable.eventKey eq eventKey)
            }.singleOrNull()
            ?.toResponse()

    fun update(
        id: UUID,
        displayName: String?,
        description: String?,
        status: String?,
    ): Boolean {
        if (displayName == null && description == null && status == null) return false
        val updated =
            TaxonomyEventTable.update({ TaxonomyEventTable.id eq id }) {
                if (displayName != null) it[TaxonomyEventTable.displayName] = displayName
                if (description != null) it[TaxonomyEventTable.description] = description
                if (status != null) it[TaxonomyEventTable.status] = status
            }
        return updated > 0
    }

    private fun ResultRow.toResponse(): TaxonomyEventResponse =
        TaxonomyEventResponse(
            id = this[TaxonomyEventTable.id].value.toString(),
            projectId = this[TaxonomyEventTable.projectId].value.toString(),
            eventKey = this[TaxonomyEventTable.eventKey],
            displayName = this[TaxonomyEventTable.displayName],
            description = this[TaxonomyEventTable.description],
            schemaVersion = this[TaxonomyEventTable.schemaVersion],
            status = this[TaxonomyEventTable.status],
            isSystem = this[TaxonomyEventTable.isSystem],
            createdAt = this[TaxonomyEventTable.createdAt].toString(),
        )
}
