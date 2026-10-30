package io.github.chaevi.analytics.backend.organization

import kotlinx.datetime.Clock
import org.jetbrains.exposed.sql.ResultRow
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.deleteWhere
import org.jetbrains.exposed.sql.insert
import org.jetbrains.exposed.sql.selectAll
import org.jetbrains.exposed.sql.update
import java.util.UUID

class OrganizationRepository {
    fun create(name: String): OrganizationResponse {
        val now = Clock.System.now()
        val id = UUID.randomUUID()

        OrganizationTable.insert {
            it[OrganizationTable.id] = id
            it[OrganizationTable.name] = name
            it[createdAt] = now
        }

        return OrganizationResponse(
            id = id.toString(),
            name = name,
            createdAt = now.toString(),
        )
    }

    fun findAll(
        page: Int,
        pageSize: Int,
    ): Pair<List<OrganizationResponse>, Long> {
        val total = OrganizationTable.selectAll().count()
        val rows =
            OrganizationTable
                .selectAll()
                .orderBy(OrganizationTable.createdAt)
                .limit(pageSize)
                .offset(((page - 1) * pageSize).toLong())
                .map { it.toResponse() }

        return Pair(rows, total)
    }

    fun findById(id: UUID): OrganizationResponse? =
        OrganizationTable
            .selectAll()
            .where { OrganizationTable.id eq id }
            .singleOrNull()
            ?.toResponse()

    fun update(
        id: UUID,
        name: String?,
    ): Boolean {
        if (name == null) return false
        val updated =
            OrganizationTable.update({ OrganizationTable.id eq id }) {
                it[OrganizationTable.name] = name
            }
        return updated > 0
    }

    fun delete(id: UUID): Boolean = OrganizationTable.deleteWhere { OrganizationTable.id eq id } > 0

    private fun ResultRow.toResponse(): OrganizationResponse =
        OrganizationResponse(
            id = this[OrganizationTable.id].value.toString(),
            name = this[OrganizationTable.name],
            createdAt = this[OrganizationTable.createdAt].toString(),
        )
}
