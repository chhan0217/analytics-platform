package io.github.chaevi.analytics.backend.taxonomy

import kotlinx.serialization.Serializable

@Serializable
data class CreateTaxonomyPropertyRequest(
    val propertyKey: String,
    val valueType: String,
    val required: Boolean = false,
    val description: String? = null,
)

@Serializable
data class UpdateTaxonomyPropertyRequest(
    val required: Boolean? = null,
    val description: String? = null,
)

@Serializable
data class TaxonomyPropertyResponse(
    val id: String,
    val taxonomyEventId: String,
    val propertyKey: String,
    val valueType: String,
    val required: Boolean,
    val description: String?,
)
