package io.github.chaevi.analytics.sdk.data.local

actual fun randomUuid(): String =
    java.util.UUID
        .randomUUID()
        .toString()
