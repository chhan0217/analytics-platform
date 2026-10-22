package io.github.chaevi.analytics.backend.application

import io.github.chaevi.analytics.backend.project.ProjectTable
import org.jetbrains.exposed.dao.id.UUIDTable
import org.jetbrains.exposed.sql.kotlin.datetime.timestamp

object ApplicationTable : UUIDTable("applications") {
    val projectId = reference("project_id", ProjectTable)
    val name = varchar("name", 256)
    val platform = varchar("platform", 20)
    val appIdentifier = varchar("app_identifier", 256).nullable()
    val createdAt = timestamp("created_at")
}

enum class Platform {
    ANDROID,
    IOS,
    WEB,
    ;

    companion object {
        fun fromString(value: String): Platform? = entries.find { it.name == value.uppercase() }
    }
}
