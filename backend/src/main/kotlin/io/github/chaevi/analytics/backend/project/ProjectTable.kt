package io.github.chaevi.analytics.backend.project

import io.github.chaevi.analytics.backend.organization.OrganizationTable
import org.jetbrains.exposed.dao.id.UUIDTable
import org.jetbrains.exposed.sql.kotlin.datetime.timestamp

object ProjectTable : UUIDTable("projects") {
    val organizationId = reference("organization_id", OrganizationTable)
    val name = varchar("name", 256)
    val unregisteredEventPolicy = varchar("unregistered_event_policy", 20).default("PERMISSIVE")
    val createdAt = timestamp("created_at")
}
