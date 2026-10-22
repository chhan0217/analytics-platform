package io.github.chaevi.analytics.sdk.data.local

import io.github.chaevi.analytics.sdk.domain.repository.StorageRepository

expect fun createPlatformStorage(): StorageRepository
