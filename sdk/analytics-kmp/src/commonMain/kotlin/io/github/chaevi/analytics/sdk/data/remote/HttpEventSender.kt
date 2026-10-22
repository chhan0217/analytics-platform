package io.github.chaevi.analytics.sdk.data.remote

import io.github.chaevi.analytics.sdk.domain.model.AnalyticsEvent
import io.github.chaevi.analytics.sdk.domain.model.IngestPayload
import io.github.chaevi.analytics.sdk.domain.model.SdkInfo
import io.github.chaevi.analytics.sdk.domain.repository.EventSender
import io.github.chaevi.analytics.sdk.domain.repository.SendResult
import io.ktor.client.HttpClient
import io.ktor.client.request.header
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.http.ContentType
import io.ktor.http.contentType

class HttpEventSender(
    private val client: HttpClient,
    private val baseUrl: String,
    private val apiKey: String,
) : EventSender {
    override suspend fun send(events: List<AnalyticsEvent>): SendResult =
        try {
            val payload =
                IngestPayload(
                    sdkInfo = SdkInfo(sdkName = "analytics-kmp", sdkVersion = SDK_VERSION),
                    events = events,
                )

            val response =
                client.post("$baseUrl/ingest/events") {
                    header("X-Ingestion-Key", apiKey)
                    contentType(ContentType.Application.Json)
                    setBody(payload)
                }

            when (response.status.value) {
                in 200..299 -> SendResult.SUCCESS
                in 400..499 -> SendResult.REJECTED
                in 500..599 -> SendResult.SERVER_ERROR
                else -> SendResult.NETWORK_ERROR
            }
        } catch (e: Exception) {
            SendResult.NETWORK_ERROR
        }

    companion object {
        const val SDK_VERSION = "0.1.0"
    }
}
