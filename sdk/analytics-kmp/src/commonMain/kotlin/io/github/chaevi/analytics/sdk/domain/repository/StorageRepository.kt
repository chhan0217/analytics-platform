package io.github.chaevi.analytics.sdk.domain.repository

interface StorageRepository {
    fun getString(key: String): String?

    fun putString(
        key: String,
        value: String,
    )

    fun remove(key: String)
}
