package io.github.chaevi.analytics.backend.application

import kotlinx.datetime.Clock
import org.jetbrains.exposed.sql.ResultRow
import org.jetbrains.exposed.sql.and
import org.jetbrains.exposed.sql.insert
import org.jetbrains.exposed.sql.selectAll
import org.jetbrains.exposed.sql.update
import java.util.UUID

class ApplicationRepository {
    fun create(
        projectId: UUID,
        name: String,
        platform: Platform,
        appIdentifier: String?,
    ): ApplicationResponse {
        val now = Clock.System.now()
        val id = UUID.randomUUID()

        ApplicationTable.insert {
            it[ApplicationTable.id] = id
            it[ApplicationTable.projectId] = projectId
            it[ApplicationTable.name] = name
            it[ApplicationTable.platform] = platform.name
            it[ApplicationTable.appIdentifier] = appIdentifier
            it[createdAt] = now
        }

        return ApplicationResponse(
            id = id.toString(),
            projectId = projectId.toString(),
            name = name,
            platform = platform.name,
            appIdentifier = appIdentifier,
            createdAt = now.toString(),
        )
    }

    fun findAllByProject(
        projectId: UUID,
        page: Int,
        pageSize: Int,
    ): Pair<List<ApplicationResponse>, Long> {
        val query = ApplicationTable.selectAll().where { ApplicationTable.projectId eq projectId }
        val total = query.count()
        val rows =
            query
                .orderBy(ApplicationTable.createdAt)
                .limit(pageSize)
                .offset(((page - 1) * pageSize).toLong())
                .map { it.toResponse() }

        return Pair(rows, total)
    }

    fun findById(id: UUID): ApplicationResponse? =
        ApplicationTable
            .selectAll()
            .where { ApplicationTable.id eq id }
            .singleOrNull()
            ?.toResponse()

    fun findByIdAndProject(
        id: UUID,
        projectId: UUID,
    ): ApplicationResponse? =
        ApplicationTable
            .selectAll()
            .where { (ApplicationTable.id eq id) and (ApplicationTable.projectId eq projectId) }
            .singleOrNull()
            ?.toResponse()

    fun update(
        id: UUID,
        name: String?,
        appIdentifier: String?,
    ): Boolean {
        if (name == null && appIdentifier == null) return false
        val updated =
            ApplicationTable.update({ ApplicationTable.id eq id }) {
                if (name != null) it[ApplicationTable.name] = name
                if (appIdentifier != null) it[ApplicationTable.appIdentifier] = appIdentifier
            }
        return updated > 0
    }

    private fun ResultRow.toResponse(): ApplicationResponse =
        ApplicationResponse(
            id = this[ApplicationTable.id].value.toString(),
            projectId = this[ApplicationTable.projectId].value.toString(),
            name = this[ApplicationTable.name],
            platform = this[ApplicationTable.platform],
            appIdentifier = this[ApplicationTable.appIdentifier],
            createdAt = this[ApplicationTable.createdAt].toString(),
        )
}
