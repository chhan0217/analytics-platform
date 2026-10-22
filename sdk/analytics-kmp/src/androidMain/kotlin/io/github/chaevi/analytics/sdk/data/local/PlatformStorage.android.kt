package io.github.chaevi.analytics.sdk.data.local

import android.content.Context
import android.content.SharedPreferences
import io.github.chaevi.analytics.sdk.domain.repository.StorageRepository

private var appContext: Context? = null

fun initAndroidStorage(context: Context) {
    appContext = context.applicationContext
}

actual fun createPlatformStorage(): StorageRepository {
    val ctx = appContext
    return if (ctx != null) {
        SharedPreferencesStorage(
            ctx.getSharedPreferences("analytics_sdk", Context.MODE_PRIVATE),
        )
    } else {
        InMemoryStorage()
    }
}

class SharedPreferencesStorage(
    private val prefs: SharedPreferences,
) : StorageRepository {
    override fun getString(key: String): String? = prefs.getString(key, null)

    override fun putString(
        key: String,
        value: String,
    ) {
        prefs.edit().putString(key, value).apply()
    }

    override fun remove(key: String) {
        prefs.edit().remove(key).apply()
    }
}
