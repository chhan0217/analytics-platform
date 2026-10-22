package io.github.chaevi.analytics.backend.credential

import kotlinx.serialization.Serializable

@Serializable
data class CreateCredentialResponse(
    val id: String,
    val applicationId: String,
    val ingestionKey: String,
    val keyPrefix: String,
    val status: String,
    val createdAt: String,
)

@Serializable
data class CredentialResponse(
    val id: String,
    val applicationId: String,
    val keyPrefix: String,
    val status: String,
    val createdAt: String,
    val revokedAt: String?,
)
