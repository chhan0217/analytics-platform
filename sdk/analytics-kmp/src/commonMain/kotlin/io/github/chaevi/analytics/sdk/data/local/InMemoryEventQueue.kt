package io.github.chaevi.analytics.sdk.data.local

import io.github.chaevi.analytics.sdk.domain.model.AnalyticsEvent
import io.github.chaevi.analytics.sdk.domain.repository.EventQueueRepository

class InMemoryEventQueue(
    private val maxSize: Int = 1000,
) : EventQueueRepository {
    private val queue = mutableListOf<AnalyticsEvent>()

    override fun enqueue(event: AnalyticsEvent) {
        if (queue.size >= maxSize) {
            queue.removeFirst()
        }
        queue.add(event)
    }

    override fun peek(count: Int): List<AnalyticsEvent> = queue.take(count).toList()

    override fun remove(eventIds: List<String>) {
        val idSet = eventIds.toSet()
        queue.removeAll { it.eventId in idSet }
    }

    override fun size(): Int = queue.size

    override fun clear() {
        queue.clear()
    }
}
