package io.github.chaevi.analytics.sdk.domain.usecase

import io.github.chaevi.analytics.sdk.data.local.randomUuid
import io.github.chaevi.analytics.sdk.domain.repository.StorageRepository
import kotlinx.datetime.Clock
import kotlinx.datetime.Instant

data class SessionTransition(
    val previousSessionId: String,
    val previousDurationMs: Long,
    val newSessionId: String,
)

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

    /**
     * 세션 ID를 반환한다.
     * 세션이 만료되었으면 새 세션을 시작하고 [SessionTransition]을 반환한다.
     * 기존 세션이 유효하면 null을 반환한다.
     */
    fun getOrCreateSessionId(): Pair<String, SessionTransition?> {
        val now = Clock.System.now()
        val currentSessionId = storage.getString(KEY_SESSION_ID)
        val lastEventAt = storage.getString(KEY_LAST_EVENT_AT)?.let { Instant.parse(it) }
        val sessionStart = storage.getString(KEY_SESSION_START)?.let { Instant.parse(it) }

        val shouldStartNew =
            currentSessionId == null ||
                isTimedOut(now, lastEventAt) ||
                isMaxDurationExceeded(now, sessionStart)

        return if (shouldStartNew) {
            val transition =
                if (currentSessionId != null && sessionStart != null) {
                    val endTime = lastEventAt ?: sessionStart
                    SessionTransition(
                        previousSessionId = currentSessionId,
                        previousDurationMs = (endTime - sessionStart).inWholeMilliseconds,
                        newSessionId = "",
                    )
                } else {
                    null
                }

            val newId = startNewSession(now)
            val finalTransition = transition?.copy(newSessionId = newId)
            Pair(newId, finalTransition)
        } else {
            storage.putString(KEY_LAST_EVENT_AT, now.toString())
            Pair(currentSessionId!!, null)
        }
    }

    fun getCurrentSessionId(): String? = storage.getString(KEY_SESSION_ID)

    fun getSessionDurationMs(): Long {
        val start = storage.getString(KEY_SESSION_START)?.let { Instant.parse(it) } ?: return 0
        val lastEvent = storage.getString(KEY_LAST_EVENT_AT)?.let { Instant.parse(it) } ?: return 0
        return (lastEvent - start).inWholeMilliseconds
    }

    fun getNextSequenceNumber(): Int = ++sequenceNumber

    fun isSessionExpired(): Boolean {
        val now = Clock.System.now()
        val lastEventAt = storage.getString(KEY_LAST_EVENT_AT)?.let { Instant.parse(it) }
        val sessionStart = storage.getString(KEY_SESSION_START)?.let { Instant.parse(it) }
        return isTimedOut(now, lastEventAt) || isMaxDurationExceeded(now, sessionStart)
    }

    private fun startNewSession(now: Instant): String {
        val newSessionId = randomUuid()
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
