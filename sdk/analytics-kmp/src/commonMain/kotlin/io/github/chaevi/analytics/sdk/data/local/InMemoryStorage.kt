package io.github.chaevi.analytics.sdk.data.local

import io.github.chaevi.analytics.sdk.domain.repository.StorageRepository

class InMemoryStorage : StorageRepository {
    private val store = mutableMapOf<String, String>()

    override fun getString(key: String): String? = store[key]

    override fun putString(
        key: String,
        value: String,
    ) {
        store[key] = value
    }

    override fun remove(key: String) {
        store.remove(key)
    }
}
