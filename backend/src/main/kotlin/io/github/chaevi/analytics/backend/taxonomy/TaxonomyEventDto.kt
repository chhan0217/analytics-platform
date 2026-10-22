package io.github.chaevi.analytics.backend.taxonomy

import kotlinx.serialization.Serializable

@Serializable
data class CreateTaxonomyEventRequest(
    val eventKey: String,
    val displayName: String,
    val description: String? = null,
)

@Serializable
data class UpdateTaxonomyEventRequest(
    val displayName: String? = null,
    val description: String? = null,
    val status: String? = null,
)

@Serializable
data class TaxonomyEventResponse(
    val id: String,
    val projectId: String,
    val eventKey: String,
    val displayName: String,
    val description: String?,
    val schemaVersion: Int,
    val status: String,
    val isSystem: Boolean,
    val createdAt: String,
)
