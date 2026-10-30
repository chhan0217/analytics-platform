package io.github.chaevi.analytics.backend.taxonomy

import org.jetbrains.exposed.dao.id.UUIDTable

object TaxonomyPropertyTable : UUIDTable("taxonomy_properties") {
    val taxonomyEventId = reference("taxonomy_event_id", TaxonomyEventTable)
    val propertyKey = varchar("property_key", 128)
    val valueType = varchar("value_type", 20)
    val required = bool("required").default(false)
    val description = text("description").nullable()
}

enum class PropertyValueType {
    STRING,
    INTEGER,
    FLOAT,
    BOOLEAN,
    ARRAY,
    OBJECT,
    ;

    companion object {
        fun fromString(value: String): PropertyValueType? = entries.find { it.name == value.uppercase() }
    }
}
