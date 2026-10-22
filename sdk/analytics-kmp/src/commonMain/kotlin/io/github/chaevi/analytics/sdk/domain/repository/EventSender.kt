package io.github.chaevi.analytics.sdk.domain.repository

import io.github.chaevi.analytics.sdk.domain.model.AnalyticsEvent

enum class SendResult {
    SUCCESS,
    REJECTED,
    SERVER_ERROR,
    NETWORK_ERROR,
}

interface EventSender {
    suspend fun send(events: List<AnalyticsEvent>): SendResult
}
