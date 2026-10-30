package io.github.chaevi.analytics.backend.project

import kotlinx.serialization.Serializable

@Serializable
data class CreateProjectRequest(
    val name: String,
)

@Serializable
data class UpdateProjectRequest(
    val name: String? = null,
    val unregisteredEventPolicy: String? = null,
)

@Serializable
data class ProjectResponse(
    val id: String,
    val organizationId: String,
    val name: String,
    val unregisteredEventPolicy: String,
    val createdAt: String,
)
