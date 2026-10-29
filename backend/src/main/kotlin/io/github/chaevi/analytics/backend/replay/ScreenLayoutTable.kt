package io.github.chaevi.analytics.backend.replay

import io.github.chaevi.analytics.backend.project.ProjectTable
import org.jetbrains.exposed.dao.id.UUIDTable
import org.jetbrains.exposed.sql.kotlin.datetime.timestampWithTimeZone

object ScreenLayoutTable : UUIDTable("screen_layouts") {
    val projectId = reference("project_id", ProjectTable)
    val screenId = varchar("screen_id", 256)
    val displayName = varchar("display_name", 256)
    val layoutJson = text("layout_json")
    val createdAt = timestampWithTimeZone("created_at")
    val updatedAt = timestampWithTimeZone("updated_at")
}
