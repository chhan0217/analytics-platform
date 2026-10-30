package io.github.chaevi.analytics.sdk.data.remote

import io.github.chaevi.analytics.sdk.domain.model.AnalyticsEvent
import io.github.chaevi.analytics.sdk.domain.model.IngestPayload
import io.github.chaevi.analytics.sdk.domain.model.SdkInfo
import io.github.chaevi.analytics.sdk.domain.repository.EventSender
import io.github.chaevi.analytics.sdk.domain.repository.SendResult
import io.github.chaevi.analytics.sdk.domain.usecase.SdkLogger
import io.ktor.client.HttpClient
import io.ktor.client.request.header
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.client.statement.bodyAsText
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

            SdkLogger.d(
                "HTTP",
                "POST /ingest/events (${events.size}건) → $baseUrl",
            )
            events.forEach { e ->
                SdkLogger.d("HTTP", "  - ${e.eventKey} [${e.eventId.take(8)}...]")
            }

            val response =
                client.post("$baseUrl/ingest/events") {
                    header("X-Ingestion-Key", apiKey)
                    contentType(ContentType.Application.Json)
                    setBody(payload)
                }

            val body = response.bodyAsText()
            val status = response.status.value
            SdkLogger.d("HTTP", "응답: $status $body")

            when (status) {
                in 200..299 -> SendResult.SUCCESS
                in 400..499 -> {
                    SdkLogger.e("HTTP", "거부됨: $status $body")
                    SendResult.REJECTED
                }
                in 500..599 -> {
                    SdkLogger.e("HTTP", "서버 오류: $status")
                    SendResult.SERVER_ERROR
                }
                else -> SendResult.NETWORK_ERROR
            }
        } catch (e: Exception) {
            SdkLogger.e("HTTP", "네트워크 오류: ${e.message}")
            SendResult.NETWORK_ERROR
        }

    companion object {
        const val SDK_VERSION = "0.1.0"
    }
}
