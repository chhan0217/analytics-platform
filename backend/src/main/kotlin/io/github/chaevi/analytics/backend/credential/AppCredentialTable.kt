package io.github.chaevi.analytics.backend.credential

import io.github.chaevi.analytics.backend.application.ApplicationTable
import org.jetbrains.exposed.dao.id.UUIDTable
import org.jetbrains.exposed.sql.kotlin.datetime.timestamp

object AppCredentialTable : UUIDTable("app_credentials") {
    val applicationId = reference("application_id", ApplicationTable)
    val keyPrefix = varchar("key_prefix", 12)
    val keyHash = varchar("key_hash", 64)
    val status = varchar("status", 20).default("ACTIVE")
    val createdAt = timestamp("created_at")
    val revokedAt = timestamp("revoked_at").nullable()
}
