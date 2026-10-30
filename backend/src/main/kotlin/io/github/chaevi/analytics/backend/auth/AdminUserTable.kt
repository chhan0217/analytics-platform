package io.github.chaevi.analytics.backend.auth

import io.github.chaevi.analytics.backend.project.ProjectTable
import org.jetbrains.exposed.dao.id.UUIDTable
import org.jetbrains.exposed.sql.kotlin.datetime.timestamp

object AdminUserTable : UUIDTable("admin_users") {
    val email = varchar("email", 256)
    val passwordHash = varchar("password_hash", 256)
    val name = varchar("name", 128)
    val createdAt = timestamp("created_at")
}

object ProjectMembershipTable : UUIDTable("project_memberships") {
    val adminUserId = reference("admin_user_id", AdminUserTable)
    val projectId = reference("project_id", ProjectTable)
    val role = varchar("role", 20).default("MEMBER")
    val createdAt = timestamp("created_at")
}
