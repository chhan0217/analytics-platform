package io.github.chaevi.analytics.sdk.domain.usecase

import io.github.chaevi.analytics.sdk.domain.repository.EventQueueRepository
import io.github.chaevi.analytics.sdk.domain.repository.EventSender
import io.github.chaevi.analytics.sdk.domain.repository.SendResult
import kotlinx.coroutines.delay

class EventDispatcher(
    private val queue: EventQueueRepository,
    private val sender: EventSender,
    private val maxBatchSize: Int = 100,
    private val maxRetries: Int = 3,
) {
    suspend fun flush(): FlushResult {
        var totalSent = 0
        var totalFailed = 0

        while (queue.size() > 0) {
            val batch = queue.peek(maxBatchSize)
            if (batch.isEmpty()) break

            val result = sendWithRetry(batch)
            when (result) {
                SendResult.SUCCESS -> {
                    queue.remove(batch.map { it.eventId })
                    totalSent += batch.size
                }
                SendResult.REJECTED -> {
                    queue.remove(batch.map { it.eventId })
                    totalFailed += batch.size
                }
                SendResult.SERVER_ERROR,
                SendResult.NETWORK_ERROR,
                -> {
                    totalFailed += batch.size
                    break
                }
            }
        }

        return FlushResult(sent = totalSent, failed = totalFailed)
    }

    private suspend fun sendWithRetry(batch: List<io.github.chaevi.analytics.sdk.domain.model.AnalyticsEvent>): SendResult {
        var lastResult = SendResult.NETWORK_ERROR

        for (attempt in 0 until maxRetries) {
            lastResult = sender.send(batch)

            when (lastResult) {
                SendResult.SUCCESS,
                SendResult.REJECTED,
                -> return lastResult

                SendResult.SERVER_ERROR,
                SendResult.NETWORK_ERROR,
                -> {
                    if (attempt < maxRetries - 1) {
                        val delayMs = (1000L * (1 shl attempt)).coerceAtMost(30_000L)
                        delay(delayMs)
                    }
                }
            }
        }

        return lastResult
    }
}

data class FlushResult(
    val sent: Int,
    val failed: Int,
)
