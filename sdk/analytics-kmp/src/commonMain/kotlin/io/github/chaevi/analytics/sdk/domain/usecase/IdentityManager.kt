package io.github.chaevi.analytics.sdk.domain.usecase

import io.github.chaevi.analytics.sdk.domain.repository.StorageRepository
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid

class IdentityManager(
    private val storage: StorageRepository,
) {
    companion object {
        private const val KEY_ANONYMOUS_ID = "analytics_anonymous_id"
        private const val KEY_EXTERNAL_USER_ID = "analytics_external_user_id"
    }

    @OptIn(ExperimentalUuidApi::class)
    fun getAnonymousId(): String {
        val existing = storage.getString(KEY_ANONYMOUS_ID)
        if (existing != null) return existing

        val newId = Uuid.random().toString()
        storage.putString(KEY_ANONYMOUS_ID, newId)
        return newId
    }

    fun getExternalUserId(): String? = storage.getString(KEY_EXTERNAL_USER_ID)

    fun identify(externalUserId: String) {
        storage.putString(KEY_EXTERNAL_USER_ID, externalUserId)
    }

    fun resetIdentity() {
        storage.remove(KEY_EXTERNAL_USER_ID)
    }
}
