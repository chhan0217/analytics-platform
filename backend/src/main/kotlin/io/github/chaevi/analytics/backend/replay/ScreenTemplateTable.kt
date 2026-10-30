package io.github.chaevi.analytics.backend.replay

import io.github.chaevi.analytics.backend.application.ApplicationTable
import io.github.chaevi.analytics.backend.ingest.jsonbText
import io.github.chaevi.analytics.backend.project.ProjectTable
import org.jetbrains.exposed.dao.id.UUIDTable
import org.jetbrains.exposed.sql.kotlin.datetime.timestampWithTimeZone

object ScreenTemplateTable : UUIDTable("screen_templates") {
    val projectId = reference("project_id", ProjectTable)
    val applicationId = reference("application_id", ApplicationTable)
    val screenId = varchar("screen_id", 256)
    val appVersion = varchar("app_version", 64)
    val viewportWidth = integer("viewport_width")
    val viewportHeight = integer("viewport_height")
    val wireframe = jsonbText("wireframe")
    val capturedAt = timestampWithTimeZone("captured_at")
    val screenshot = text("screenshot").nullable()
}
