package io.github.chaevi.analytics.backend.organization

import org.jetbrains.exposed.dao.id.UUIDTable
import org.jetbrains.exposed.sql.kotlin.datetime.timestamp

object OrganizationTable : UUIDTable("organizations") {
    val name = varchar("name", 256)
    val createdAt = timestamp("created_at")
}
