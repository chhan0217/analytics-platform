package io.github.chaevi.analytics.sdk.domain.repository

import io.github.chaevi.analytics.sdk.domain.model.AnalyticsEvent

interface EventQueueRepository {
    fun enqueue(event: AnalyticsEvent)

    fun peek(count: Int): List<AnalyticsEvent>

    fun remove(eventIds: List<String>)

    fun size(): Int

    fun clear()
}
