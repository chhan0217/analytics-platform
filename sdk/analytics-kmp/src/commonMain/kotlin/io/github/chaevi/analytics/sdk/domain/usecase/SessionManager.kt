package io.github.chaevi.analytics.sdk.domain.usecase

import io.github.chaevi.analytics.sdk.domain.repository.StorageRepository
import kotlinx.datetime.Clock
import kotlinx.datetime.Instant
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid

class SessionManager(
    private val storage: StorageRepository,
    private val sessionTimeoutMs: Long = 30 * 60 * 1000L,
    private val sessionMaxDurationMs: Long = 24 * 60 * 60 * 1000L,
) {
    companion object {
        private const val KEY_SESSION_ID = "analytics_session_id"
        private const val KEY_SESSION_START = "analytics_session_start"
        private const val KEY_LAST_EVENT_AT = "analytics_last_event_at"
    }

    private var sequenceNumber = 0

    @OptIn(ExperimentalUuidApi::class)
    fun getOrCreateSessionId(): String {
        val now = Clock.System.now()
        val currentSessionId = storage.getString(KEY_SESSION_ID)
        val lastEventAt = storage.getString(KEY_LAST_EVENT_AT)?.let { Instant.parse(it) }
        val sessionStart = storage.getString(KEY_SESSION_START)?.let { Instant.parse(it) }

        val shouldStartNew =
            currentSessionId == null ||
                isTimedOut(now, lastEventAt) ||
                isMaxDurationExceeded(now, sessionStart)

        return if (shouldStartNew) {
            startNewSession(now)
        } else {
            storage.putString(KEY_LAST_EVENT_AT, now.toString())
            currentSessionId!!
        }
    }

    fun getNextSequenceNumber(): Int = ++sequenceNumber

    fun isSessionExpired(): Boolean {
        val now = Clock.System.now()
        val lastEventAt = storage.getString(KEY_LAST_EVENT_AT)?.let { Instant.parse(it) }
        val sessionStart = storage.getString(KEY_SESSION_START)?.let { Instant.parse(it) }
        return isTimedOut(now, lastEventAt) || isMaxDurationExceeded(now, sessionStart)
    }

    @OptIn(ExperimentalUuidApi::class)
    private fun startNewSession(now: Instant): String {
        val newSessionId = Uuid.random().toString()
        storage.putString(KEY_SESSION_ID, newSessionId)
        storage.putString(KEY_SESSION_START, now.toString())
        storage.putString(KEY_LAST_EVENT_AT, now.toString())
        sequenceNumber = 0
        return newSessionId
    }

    private fun isTimedOut(
        now: Instant,
        lastEventAt: Instant?,
    ): Boolean {
        if (lastEventAt == null) return true
        return (now - lastEventAt).inWholeMilliseconds > sessionTimeoutMs
    }

    private fun isMaxDurationExceeded(
        now: Instant,
        sessionStart: Instant?,
    ): Boolean {
        if (sessionStart == null) return true
        return (now - sessionStart).inWholeMilliseconds > sessionMaxDurationMs
    }
}
