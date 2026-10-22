package io.github.chaevi.analytics.sdk.domain.usecase

internal object SdkLogger {
    var enabled = false

    fun d(
        tag: String,
        message: String,
    ) {
        if (enabled) println("[Analytics/$tag] $message")
    }

    fun e(
        tag: String,
        message: String,
    ) {
        if (enabled) println("[Analytics/$tag] ERROR: $message")
    }
}
