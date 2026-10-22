package io.github.chaevi.analytics.backend.taxonomy

import io.github.chaevi.analytics.backend.project.ProjectTable
import org.jetbrains.exposed.dao.id.UUIDTable
import org.jetbrains.exposed.sql.kotlin.datetime.timestamp

object TaxonomyEventTable : UUIDTable("taxonomy_events") {
    val projectId = reference("project_id", ProjectTable)
    val eventKey = varchar("event_key", 128)
    val displayName = varchar("display_name", 256)
    val description = text("description").nullable()
    val schemaVersion = integer("schema_version").default(1)
    val status = varchar("status", 20).default("ACTIVE")
    val isSystem = bool("is_system").default(false)
    val createdAt = timestamp("created_at")
}

enum class TaxonomyEventStatus {
    ACTIVE,
    DEPRECATED,
    ARCHIVED,
    ;

    companion object {
        fun fromString(value: String): TaxonomyEventStatus? = entries.find { it.name == value.uppercase() }
    }
}
