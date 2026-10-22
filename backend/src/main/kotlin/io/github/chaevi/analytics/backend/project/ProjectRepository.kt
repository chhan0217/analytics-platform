package io.github.chaevi.analytics.backend.project

import kotlinx.datetime.Clock
import org.jetbrains.exposed.sql.ResultRow
import org.jetbrains.exposed.sql.insert
import org.jetbrains.exposed.sql.selectAll
import org.jetbrains.exposed.sql.update
import java.util.UUID

class ProjectRepository {
    fun create(
        organizationId: UUID,
        name: String,
    ): ProjectResponse {
        val now = Clock.System.now()
        val id = UUID.randomUUID()

        ProjectTable.insert {
            it[ProjectTable.id] = id
            it[ProjectTable.organizationId] = organizationId
            it[ProjectTable.name] = name
            it[createdAt] = now
        }

        return ProjectResponse(
            id = id.toString(),
            organizationId = organizationId.toString(),
            name = name,
            unregisteredEventPolicy = "PERMISSIVE",
            createdAt = now.toString(),
        )
    }

    fun findAllByOrganization(
        organizationId: UUID,
        page: Int,
        pageSize: Int,
    ): Pair<List<ProjectResponse>, Long> {
        val query = ProjectTable.selectAll().where { ProjectTable.organizationId eq organizationId }
        val total = query.count()
        val rows =
            query
                .orderBy(ProjectTable.createdAt)
                .limit(pageSize)
                .offset(((page - 1) * pageSize).toLong())
                .map { it.toResponse() }

        return Pair(rows, total)
    }

    fun findById(id: UUID): ProjectResponse? =
        ProjectTable
            .selectAll()
            .where { ProjectTable.id eq id }
            .singleOrNull()
            ?.toResponse()

    fun update(
        id: UUID,
        name: String?,
        unregisteredEventPolicy: String?,
    ): Boolean {
        val updated =
            ProjectTable.update({ ProjectTable.id eq id }) {
                if (name != null) it[ProjectTable.name] = name
                if (unregisteredEventPolicy != null) it[ProjectTable.unregisteredEventPolicy] = unregisteredEventPolicy
            }
        return updated > 0
    }

    private fun ResultRow.toResponse(): ProjectResponse =
        ProjectResponse(
            id = this[ProjectTable.id].value.toString(),
            organizationId = this[ProjectTable.organizationId].value.toString(),
            name = this[ProjectTable.name],
            unregisteredEventPolicy = this[ProjectTable.unregisteredEventPolicy],
            createdAt = this[ProjectTable.createdAt].toString(),
        )
}
