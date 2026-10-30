package io.github.chaevi.analytics.backend.application

import kotlinx.serialization.Serializable

@Serializable
data class CreateApplicationRequest(
    val name: String,
    val platform: String,
    val appIdentifier: String? = null,
)

@Serializable
data class UpdateApplicationRequest(
    val name: String? = null,
    val appIdentifier: String? = null,
)

@Serializable
data class ApplicationResponse(
    val id: String,
    val projectId: String,
    val name: String,
    val platform: String,
    val appIdentifier: String?,
    val createdAt: String,
)
