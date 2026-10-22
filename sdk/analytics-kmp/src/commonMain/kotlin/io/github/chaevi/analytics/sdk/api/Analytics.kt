package io.github.chaevi.analytics.sdk.api

object Analytics {
    private var initialized = false

    fun initialize(config: AnalyticsConfig) {
        if (initialized) return
        initialized = true
    }

    fun track(
        eventKey: String,
        properties: Map<String, Any?> = emptyMap(),
    ) {
        checkInitialized()
    }

    fun trackScreen(
        screenId: String,
        properties: Map<String, Any?> = emptyMap(),
    ) {
        checkInitialized()
    }

    fun identify(externalUserId: String) {
        checkInitialized()
    }

    fun setUserProperties(properties: Map<String, Any?>) {
        checkInitialized()
    }

    fun resetIdentity() {
        checkInitialized()
    }

    fun flush() {
        checkInitialized()
    }

    private fun checkInitialized() {
        require(initialized) {
            "Analytics SDK가 초기화되지 않았습니다. Analytics.initialize()를 먼저 호출하세요."
        }
    }
}
