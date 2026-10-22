package io.github.chaevi.analytics.backend.ingest

import io.github.chaevi.analytics.backend.application.ApplicationTable
import io.github.chaevi.analytics.backend.project.ProjectTable
import org.jetbrains.exposed.dao.id.UUIDTable
import org.jetbrains.exposed.sql.Column
import org.jetbrains.exposed.sql.ColumnType
import org.jetbrains.exposed.sql.Table
import org.jetbrains.exposed.sql.kotlin.datetime.timestamp
import org.postgresql.util.PGobject

object AnalyticsUserTable : UUIDTable("analytics_users") {
    val projectId = reference("project_id", ProjectTable)
    val externalUserId = varchar("external_user_id", 256).nullable()
    val createdAt = timestamp("created_at")
    val updatedAt = timestamp("updated_at")
}

object AnonymousIdentityTable : UUIDTable("anonymous_identities") {
    val projectId = reference("project_id", ProjectTable)
    val anonymousId = uuid("anonymous_id")
    val createdAt = timestamp("created_at")
}

object AnalyticsSessionTable : UUIDTable("analytics_sessions") {
    val projectId = reference("project_id", ProjectTable)
    val applicationId = reference("application_id", ApplicationTable)
    val anonymousId = uuid("anonymous_id")
    val analyticsUserId = optReference("analytics_user_id", AnalyticsUserTable)
    val startedAt = timestamp("started_at")
    val lastEventAt = timestamp("last_event_at")
    val endedAt = timestamp("ended_at").nullable()
}

object AnalyticsEventTable : UUIDTable("analytics_events") {
    val projectId = reference("project_id", ProjectTable)
    val applicationId = reference("application_id", ApplicationTable)
    val sessionId = uuid("session_id")
    val analyticsUserId = optReference("analytics_user_id", AnalyticsUserTable)
    val anonymousId = uuid("anonymous_id")
    val eventKey = varchar("event_key", 128)
    val eventSchemaVersion = integer("event_schema_version").default(1)
    val properties = jsonbText("properties")
    val context = jsonbText("context")
    val occurredAt = timestamp("occurred_at")
    val receivedAt = timestamp("received_at")
    val sequenceNumber = integer("sequence_number").nullable()
}

class JsonbColumnType : ColumnType<String>() {
    override fun sqlType(): String = "JSONB"

    override fun valueFromDB(value: Any): String =
        when (value) {
            is PGobject -> value.value ?: "{}"
            is String -> value
            else -> value.toString()
        }

    override fun notNullValueToDB(value: String): Any =
        PGobject().apply {
            type = "jsonb"
            this.value = value
        }
}

fun Table.jsonbText(name: String): Column<String> = registerColumn(name, JsonbColumnType())
