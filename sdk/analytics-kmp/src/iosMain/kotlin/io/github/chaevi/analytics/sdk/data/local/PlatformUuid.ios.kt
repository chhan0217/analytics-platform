package io.github.chaevi.analytics.sdk.data.local

import platform.Foundation.NSUUID

actual fun randomUuid(): String = NSUUID().UUIDString().lowercase()
