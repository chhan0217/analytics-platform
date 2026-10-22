package io.github.chaevi.analytics.backend.credential

import kotlinx.datetime.Clock
import org.jetbrains.exposed.sql.ResultRow
import org.jetbrains.exposed.sql.and
import org.jetbrains.exposed.sql.insert
import org.jetbrains.exposed.sql.selectAll
import org.jetbrains.exposed.sql.update
import java.util.UUID

class AppCredentialRepository {
    fun create(
        applicationId: UUID,
        keyPrefix: String,
        keyHash: String,
    ): String {
        val id = UUID.randomUUID()
        val now = Clock.System.now()

        AppCredentialTable.insert {
            it[AppCredentialTable.id] = id
            it[AppCredentialTable.applicationId] = applicationId
            it[AppCredentialTable.keyPrefix] = keyPrefix
            it[AppCredentialTable.keyHash] = keyHash
            it[createdAt] = now
        }

        return id.toString()
    }

    fun findAllByApplication(applicationId: UUID): List<CredentialResponse> =
        AppCredentialTable
            .selectAll()
            .where { AppCredentialTable.applicationId eq applicationId }
            .orderBy(AppCredentialTable.createdAt)
            .map { it.toResponse() }

    fun findByKeyHash(keyHash: String): CredentialWithAppInfo? =
        AppCredentialTable
            .selectAll()
            .where { AppCredentialTable.keyHash eq keyHash }
            .singleOrNull()
            ?.let {
                CredentialWithAppInfo(
                    credentialId = it[AppCredentialTable.id].value.toString(),
                    applicationId = it[AppCredentialTable.applicationId].value.toString(),
                    status = it[AppCredentialTable.status],
                )
            }

    fun revoke(
        id: UUID,
        applicationId: UUID,
    ): Boolean {
        val now = Clock.System.now()
        val updated =
            AppCredentialTable.update({
                (AppCredentialTable.id eq id) and
                    (AppCredentialTable.applicationId eq applicationId) and
                    (AppCredentialTable.status eq "ACTIVE")
            }) {
                it[status] = "REVOKED"
                it[revokedAt] = now
            }
        return updated > 0
    }

    private fun ResultRow.toResponse(): CredentialResponse =
        CredentialResponse(
            id = this[AppCredentialTable.id].value.toString(),
            applicationId = this[AppCredentialTable.applicationId].value.toString(),
            keyPrefix = this[AppCredentialTable.keyPrefix],
            status = this[AppCredentialTable.status],
            createdAt = this[AppCredentialTable.createdAt].toString(),
            revokedAt = this[AppCredentialTable.revokedAt]?.toString(),
        )
}

data class CredentialWithAppInfo(
    val credentialId: String,
    val applicationId: String,
    val status: String,
)
