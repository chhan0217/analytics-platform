package io.github.chaevi.analytics.backend.organization

import kotlinx.serialization.Serializable

@Serializable
data class CreateOrganizationRequest(
    val name: String,
)

@Serializable
data class UpdateOrganizationRequest(
    val name: String? = null,
)

@Serializable
data class OrganizationResponse(
    val id: String,
    val name: String,
    val createdAt: String,
)
