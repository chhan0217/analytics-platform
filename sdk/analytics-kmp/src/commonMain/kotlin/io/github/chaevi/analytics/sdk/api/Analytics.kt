package io.github.chaevi.analytics.sdk.api

import io.github.chaevi.analytics.sdk.data.local.InMemoryEventQueue
import io.github.chaevi.analytics.sdk.data.local.collectPlatformContext
import io.github.chaevi.analytics.sdk.data.local.createPlatformStorage
import io.github.chaevi.analytics.sdk.data.local.randomUuid
import io.github.chaevi.analytics.sdk.data.remote.HttpEventSender
import io.github.chaevi.analytics.sdk.domain.model.AnalyticsEvent
import io.github.chaevi.analytics.sdk.domain.repository.EventQueueRepository
import io.github.chaevi.analytics.sdk.domain.repository.StorageRepository
import io.github.chaevi.analytics.sdk.domain.usecase.EventDispatcher
import io.github.chaevi.analytics.sdk.domain.usecase.IdentityManager
import io.github.chaevi.analytics.sdk.domain.usecase.SdkLogger
import io.github.chaevi.analytics.sdk.domain.usecase.SessionManager
import io.ktor.client.HttpClient
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.serialization.kotlinx.json.json
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.datetime.Clock
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonNamingStrategy
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive

private const val SDK_VERSION = "0.1.0"

object Analytics {
    private var initialized = false
    private var config: AnalyticsConfig? = null
    private lateinit var identityManager: IdentityManager
    private lateinit var sessionManager: SessionManager
    private lateinit var eventQueue: EventQueueRepository
    private lateinit var eventDispatcher: EventDispatcher
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    @OptIn(ExperimentalSerializationApi::class)
    fun initialize(
        config: AnalyticsConfig,
        storage: StorageRepository = createPlatformStorage(),
    ) {
        if (initialized) return

        this.config = config
        SdkLogger.enabled = config.enableLogging
        SdkLogger.d("SDK", "초기화 시작: baseUrl=${config.baseUrl}")
        identityManager = IdentityManager(storage)
        sessionManager = SessionManager(storage, config.sessionTimeoutMs, config.sessionMaxDurationMs)
        eventQueue = InMemoryEventQueue(config.maxQueueSize)

        val json =
            Json {
                namingStrategy = JsonNamingStrategy.SnakeCase
                ignoreUnknownKeys = true
                encodeDefaults = true
            }

        val client =
            HttpClient {
                install(ContentNegotiation) { json(json) }
            }

        val sender = HttpEventSender(client, config.baseUrl, config.apiKey)
        eventDispatcher = EventDispatcher(eventQueue, sender, config.maxBatchSize, config.maxRetries)

        initialized = true
        startPeriodicFlush()

        val (sessionId, transition) = sessionManager.getOrCreateSessionId()
        if (transition != null) {
            trackInternalWithSessionId(
                "session_end",
                transition.previousSessionId,
                mapOf("duration_ms" to transition.previousDurationMs),
            )
        }
        trackInternalWithSessionId("session_start", sessionId)
        trackInternal("app_open")
    }

    fun isInitialized(): Boolean = initialized

    fun track(
        eventKey: String,
        properties: Map<String, Any?> = emptyMap(),
    ) {
        checkInitialized()
        trackInternal(eventKey, properties)
    }

    fun trackScreen(
        screenId: String,
        properties: Map<String, Any?> = emptyMap(),
    ) {
        checkInitialized()
        val merged = mutableMapOf<String, Any?>("screen_id" to screenId)
        merged.putAll(properties)
        trackInternal("screen_view", merged)
    }

    fun trackScreenLeave(
        screenId: String,
        durationMs: Long,
    ) {
        checkInitialized()
        trackInternal(
            "screen_leave",
            mapOf("screen_id" to screenId, "duration_ms" to durationMs),
        )
    }

    fun identify(externalUserId: String) {
        checkInitialized()
        identityManager.identify(externalUserId)
        trackInternal("identify", mapOf("external_user_id" to externalUserId))
    }

    fun setUserProperties(properties: Map<String, Any?>) {
        checkInitialized()
    }

    fun resetIdentity() {
        checkInitialized()
        identityManager.resetIdentity()
    }

    /**
     * 앱이 백그라운드로 진입할 때 호출한다.
     * 큐에 남은 이벤트를 즉시 전송한다. session_end는 보내지 않는다.
     * (세션 종료는 실제 만료 시점에만 판정한다)
     */
    fun onBackground() {
        checkInitialized()
        SdkLogger.d("SDK", "백그라운드 진입: flush")
        flush()
    }

    /**
     * 앱이 포그라운드로 복귀할 때 호출한다.
     * 세션이 만료되었으면 session_end + 새 session_start를 전송한다.
     */
    fun onForeground() {
        checkInitialized()
        if (sessionManager.isSessionExpired()) {
            val previousSessionId = sessionManager.getCurrentSessionId()
            val previousDurationMs = sessionManager.getSessionDurationMs()
            if (previousSessionId != null) {
                trackInternalWithSessionId(
                    "session_end",
                    previousSessionId,
                    mapOf("duration_ms" to previousDurationMs),
                )
            }
            val (newSessionId, _) = sessionManager.getOrCreateSessionId()
            trackInternalWithSessionId("session_start", newSessionId)
            SdkLogger.d("SDK", "포그라운드 복귀: 세션 만료 → 새 세션 시작 $newSessionId")
        } else {
            SdkLogger.d("SDK", "포그라운드 복귀: 기존 세션 이어감")
        }
    }

    fun flush() {
        checkInitialized()
        SdkLogger.d("SDK", "flush 요청 (큐: ${eventQueue.size()}건)")
        scope.launch {
            try {
                val result = eventDispatcher.flush()
                SdkLogger.d("SDK", "flush 완료: sent=${result.sent}, failed=${result.failed}")
            } catch (e: Exception) {
                notifyError("FLUSH_ERROR", e.message ?: "이벤트 전송 실패")
            }
        }
    }

    private fun trackInternal(
        eventKey: String,
        properties: Map<String, Any?> = emptyMap(),
    ) {
        val (sessionId, transition) = sessionManager.getOrCreateSessionId()

        if (transition != null) {
            trackInternalWithSessionId(
                "session_end",
                transition.previousSessionId,
                mapOf("duration_ms" to transition.previousDurationMs),
            )
            trackInternalWithSessionId("session_start", sessionId)
            SdkLogger.d("SDK", "세션 전환: ${transition.previousSessionId.take(8)}... → ${sessionId.take(8)}...")
        }

        val anonymousId = identityManager.getAnonymousId()
        val now = Clock.System.now()

        val jsonProperties =
            JsonObject(
                properties.mapValues { (_, v) ->
                    when (v) {
                        is String -> JsonPrimitive(v)
                        is Number -> JsonPrimitive(v)
                        is Boolean -> JsonPrimitive(v)
                        null -> JsonPrimitive(null as String?)
                        else -> JsonPrimitive(v.toString())
                    }
                },
            )

        val platformCtx = collectPlatformContext()
        val ctxMap =
            buildMap<String, JsonPrimitive> {
                put("sdk_version", JsonPrimitive(SDK_VERSION))
                config?.appVersion?.let { put("app_version", JsonPrimitive(it)) }
                platformCtx.osName?.let { put("os_name", JsonPrimitive(it)) }
                platformCtx.osVersion?.let { put("os_version", JsonPrimitive(it)) }
                platformCtx.deviceModel?.let { put("device_model", JsonPrimitive(it)) }
                platformCtx.deviceManufacturer?.let { put("device_manufacturer", JsonPrimitive(it)) }
                platformCtx.locale?.let { put("locale", JsonPrimitive(it)) }
                platformCtx.timezone?.let { put("timezone", JsonPrimitive(it)) }
                platformCtx.screenWidth?.let { put("screen_width", JsonPrimitive(it)) }
                platformCtx.screenHeight?.let { put("screen_height", JsonPrimitive(it)) }
                platformCtx.networkType?.let { put("network_type", JsonPrimitive(it)) }
            }

        val event =
            AnalyticsEvent(
                eventId = generateEventId(),
                sessionId = sessionId,
                anonymousId = anonymousId,
                eventKey = eventKey,
                occurredAt = now.toString(),
                sequenceNumber = sessionManager.getNextSequenceNumber(),
                properties = jsonProperties,
                context = JsonObject(ctxMap),
                externalUserId = identityManager.getExternalUserId(),
            )

        eventQueue.enqueue(event)
        SdkLogger.d("SDK", "이벤트 큐 추가: $eventKey [${event.eventId.take(8)}...] (큐: ${eventQueue.size()}건)")
    }

    private fun trackInternalWithSessionId(
        eventKey: String,
        sessionId: String,
        properties: Map<String, Any?> = emptyMap(),
    ) {
        val anonymousId = identityManager.getAnonymousId()
        val now = Clock.System.now()

        val jsonProperties =
            JsonObject(
                properties.mapValues { (_, v) ->
                    when (v) {
                        is String -> JsonPrimitive(v)
                        is Number -> JsonPrimitive(v)
                        is Boolean -> JsonPrimitive(v)
                        null -> JsonPrimitive(null as String?)
                        else -> JsonPrimitive(v.toString())
                    }
                },
            )

        val platformCtx = collectPlatformContext()
        val ctxMap =
            buildMap<String, JsonPrimitive> {
                put("sdk_version", JsonPrimitive(SDK_VERSION))
                config?.appVersion?.let { put("app_version", JsonPrimitive(it)) }
                platformCtx.osName?.let { put("os_name", JsonPrimitive(it)) }
                platformCtx.osVersion?.let { put("os_version", JsonPrimitive(it)) }
                platformCtx.deviceModel?.let { put("device_model", JsonPrimitive(it)) }
                platformCtx.deviceManufacturer?.let { put("device_manufacturer", JsonPrimitive(it)) }
                platformCtx.locale?.let { put("locale", JsonPrimitive(it)) }
                platformCtx.timezone?.let { put("timezone", JsonPrimitive(it)) }
                platformCtx.screenWidth?.let { put("screen_width", JsonPrimitive(it)) }
                platformCtx.screenHeight?.let { put("screen_height", JsonPrimitive(it)) }
                platformCtx.networkType?.let { put("network_type", JsonPrimitive(it)) }
            }

        val event =
            AnalyticsEvent(
                eventId = generateEventId(),
                sessionId = sessionId,
                anonymousId = anonymousId,
                eventKey = eventKey,
                occurredAt = now.toString(),
                sequenceNumber = null,
                properties = jsonProperties,
                context = JsonObject(ctxMap),
                externalUserId = identityManager.getExternalUserId(),
            )

        eventQueue.enqueue(event)
        SdkLogger.d("SDK", "이벤트 큐 추가: $eventKey [${event.eventId.take(8)}...]")
    }

    private fun startPeriodicFlush() {
        val intervalMs = config?.flushIntervalMs ?: return
        scope.launch {
            while (isActive) {
                delay(intervalMs)
                try {
                    eventDispatcher.flush()
                } catch (e: Exception) {
                    notifyError("FLUSH_ERROR", e.message ?: "주기적 전송 실패")
                }
            }
        }
    }

    private fun generateEventId(): String = randomUuid()

    private fun notifyError(
        code: String,
        message: String,
    ) {
        config?.errorListener?.invoke(AnalyticsError(code, message))
    }

    private fun checkInitialized() {
        require(initialized) {
            "Analytics SDK가 초기화되지 않았습니다. Analytics.initialize()를 먼저 호출하세요."
        }
    }
}
