package io.github.chaevi.analytics.sdk.data.local

import io.github.chaevi.analytics.sdk.domain.repository.StorageRepository
import platform.Foundation.NSUserDefaults

actual fun createPlatformStorage(): StorageRepository = NSUserDefaultsStorage()

class NSUserDefaultsStorage : StorageRepository {
    private val defaults = NSUserDefaults.standardUserDefaults

    override fun getString(key: String): String? = defaults.stringForKey(key)

    override fun putString(
        key: String,
        value: String,
    ) {
        defaults.setObject(value, forKey = key)
    }

    override fun remove(key: String) {
        defaults.removeObjectForKey(key)
    }
}
