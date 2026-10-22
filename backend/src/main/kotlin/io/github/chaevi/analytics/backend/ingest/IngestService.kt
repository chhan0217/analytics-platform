package io.github.chaevi.analytics.backend.ingest

import io.github.chaevi.analytics.backend.application.ApplicationRepository
import io.github.chaevi.analytics.backend.common.ApiException
import io.github.chaevi.analytics.backend.credential.AppCredentialRepository
import io.github.chaevi.analytics.backend.credential.IngestionKeyUtil
import io.github.chaevi.analytics.backend.project.ProjectRepository
import io.github.chaevi.analytics.backend.taxonomy.TaxonomyEventRepository
import io.github.oshai.kotlinlogging.KotlinLogging
import io.ktor.http.HttpStatusCode
import kotlinx.datetime.Clock
import kotlinx.datetime.Instant
import org.jetbrains.exposed.sql.and
import org.jetbrains.exposed.sql.insert
import org.jetbrains.exposed.sql.selectAll
import org.jetbrains.exposed.sql.transactions.transaction
import org.jetbrains.exposed.sql.update
import java.util.UUID

private val logger = KotlinLogging.logger {}

class IngestService(
    private val credentialRepository: AppCredentialRepository = AppCredentialRepository(),
    private val applicationRepository: ApplicationRepository = ApplicationRepository(),
    private val projectRepository: ProjectRepository = ProjectRepository(),
    private val taxonomyEventRepository: TaxonomyEventRepository = TaxonomyEventRepository(),
) {
    companion object {
        const val MAX_BATCH_SIZE = 100
        const val MAX_PAYLOAD_EVENTS = 100
    }

    @Suppress("ThrowsCount")
    fun ingest(
        ingestionKey: String?,
        request: IngestRequest,
    ): IngestResponse {
        validateIngestRequest(ingestionKey, request)

        val keyHash = IngestionKeyUtil.hash(ingestionKey!!)
        val credential =
            transaction { credentialRepository.findByKeyHash(keyHash) }
                ?: throw ApiException("INVALID_INGESTION_KEY", HttpStatusCode.Unauthorized, "유효하지 않은 수집 키입니다")

        if (credential.status != "ACTIVE") {
            throw ApiException("REVOKED_INGESTION_KEY", HttpStatusCode.Unauthorized, "폐기된 수집 키입니다")
        }

        val application =
            transaction { applicationRepository.findById(UUID.fromString(credential.applicationId)) }
                ?: throw ApiException("INTERNAL_ERROR", HttpStatusCode.InternalServerError, "앱 정보를 찾을 수 없습니다")

        val projectId = UUID.fromString(application.projectId)
        val applicationId = UUID.fromString(application.id)
        val now = Clock.System.now()
        val policy = transaction { projectRepository.findById(projectId) }?.unregisteredEventPolicy ?: "PERMISSIVE"

        val (accepted, duplicated) = processEvents(request.events, projectId, applicationId, policy, now)

        logger.info { "이벤트 수집 완료: accepted=$accepted, duplicated=$duplicated, app=$applicationId" }
        return IngestResponse(accepted = accepted, duplicated = duplicated)
    }

    @Suppress("ThrowsCount")
    private fun validateIngestRequest(
        ingestionKey: String?,
        request: IngestRequest,
    ) {
        if (ingestionKey.isNullOrBlank()) {
            throw ApiException("INVALID_INGESTION_KEY", HttpStatusCode.Unauthorized, "수집 키가 필요합니다")
        }
        if (request.events.isEmpty()) {
            throw ApiException("VALIDATION_ERROR", HttpStatusCode.BadRequest, "이벤트가 비어있습니다")
        }
        if (request.events.size > MAX_BATCH_SIZE) {
            throw ApiException(
                "PAYLOAD_TOO_LARGE",
                HttpStatusCode.PayloadTooLarge,
                "배치 최대 ${MAX_BATCH_SIZE}개를 초과했습니다: ${request.events.size}",
            )
        }
    }

    @Suppress("LoopWithTooManyJumpStatements")
    private fun processEvents(
        events: List<IngestEvent>,
        projectId: UUID,
        applicationId: UUID,
        policy: String,
        now: Instant,
    ): Pair<Int, Int> {
        var accepted = 0
        var duplicated = 0

        transaction {
            for (event in events) {
                val eventId =
                    try {
                        UUID.fromString(event.eventId)
                    } catch (e: IllegalArgumentException) {
                        logger.warn { "유효하지 않은 event_id 무시: ${event.eventId}" }
                        continue
                    }

                val exists =
                    AnalyticsEventTable
                        .selectAll()
                        .where { AnalyticsEventTable.id eq eventId }
                        .count() > 0

                if (exists) {
                    duplicated++
                    continue
                }

                validateEvent(event, projectId, policy)

                val occurredAt = parseInstant(event.occurredAt)
                val sessionId = UUID.fromString(event.sessionId)
                val anonymousId = UUID.fromString(event.anonymousId)

                ensureAnonymousIdentity(projectId, anonymousId, now)
                val analyticsUserId =
                    event.externalUserId?.let {
                        ensureAnalyticsUser(projectId, it, now)
                    }
                ensureSession(projectId, applicationId, sessionId, anonymousId, analyticsUserId, occurredAt)
                updateSessionLastEvent(sessionId, occurredAt)

                AnalyticsEventTable.insert {
                    it[AnalyticsEventTable.id] = eventId
                    it[AnalyticsEventTable.projectId] = projectId
                    it[AnalyticsEventTable.applicationId] = applicationId
                    it[AnalyticsEventTable.sessionId] = sessionId
                    it[AnalyticsEventTable.anonymousId] = anonymousId
                    it[AnalyticsEventTable.analyticsUserId] = analyticsUserId
                    it[AnalyticsEventTable.eventKey] = event.eventKey
                    it[eventSchemaVersion] = event.schemaVersion
                    it[properties] = event.properties.toString()
                    it[context] = event.context.toString()
                    it[AnalyticsEventTable.occurredAt] = occurredAt
                    it[receivedAt] = now
                    it[sequenceNumber] = event.sequenceNumber
                }

                accepted++
            }
        }

        return Pair(accepted, duplicated)
    }

    private fun validateEvent(
        event: IngestEvent,
        projectId: UUID,
        policy: String,
    ) {
        val taxonomy = taxonomyEventRepository.findByProjectAndEventKey(projectId, event.eventKey)

        if (taxonomy == null) {
            when (policy) {
                "STRICT" -> throw ApiException(
                    "UNREGISTERED_EVENT",
                    HttpStatusCode.BadRequest,
                    "미등록 이벤트입니다: ${event.eventKey}",
                )
                "PERMISSIVE" -> logger.warn { "미등록 이벤트 수집 허용 (PERMISSIVE): ${event.eventKey}" }
            }
            return
        }

        if (taxonomy.status == "ARCHIVED") {
            throw ApiException("ARCHIVED_EVENT", HttpStatusCode.BadRequest, "보관된 이벤트입니다: ${event.eventKey}")
        }

        if (taxonomy.status == "DEPRECATED") {
            logger.warn { "비권장 이벤트 수집: ${event.eventKey}" }
        }
    }

    private fun ensureAnonymousIdentity(
        projectId: UUID,
        anonymousId: UUID,
        now: Instant,
    ) {
        val exists =
            AnonymousIdentityTable
                .selectAll()
                .where {
                    (AnonymousIdentityTable.projectId eq projectId) and
                        (AnonymousIdentityTable.anonymousId eq anonymousId)
                }.count() > 0

        if (!exists) {
            AnonymousIdentityTable.insert {
                it[AnonymousIdentityTable.id] = UUID.randomUUID()
                it[AnonymousIdentityTable.projectId] = projectId
                it[AnonymousIdentityTable.anonymousId] = anonymousId
                it[createdAt] = now
            }
        }
    }

    private fun ensureAnalyticsUser(
        projectId: UUID,
        externalUserId: String,
        now: Instant,
    ): UUID {
        val existing =
            AnalyticsUserTable
                .selectAll()
                .where {
                    (AnalyticsUserTable.projectId eq projectId) and
                        (AnalyticsUserTable.externalUserId eq externalUserId)
                }.firstOrNull()

        if (existing != null) {
            return existing[AnalyticsUserTable.id].value
        }

        val userId = UUID.randomUUID()
        AnalyticsUserTable.insert {
            it[AnalyticsUserTable.id] = userId
            it[AnalyticsUserTable.projectId] = projectId
            it[AnalyticsUserTable.externalUserId] = externalUserId
            it[createdAt] = now
            it[updatedAt] = now
        }
        return userId
    }

    private fun ensureSession(
        projectId: UUID,
        applicationId: UUID,
        sessionId: UUID,
        anonymousId: UUID,
        analyticsUserId: UUID?,
        occurredAt: Instant,
    ) {
        val exists =
            AnalyticsSessionTable
                .selectAll()
                .where { AnalyticsSessionTable.id eq sessionId }
                .count() > 0

        if (!exists) {
            AnalyticsSessionTable.insert {
                it[AnalyticsSessionTable.id] = sessionId
                it[AnalyticsSessionTable.projectId] = projectId
                it[AnalyticsSessionTable.applicationId] = applicationId
                it[AnalyticsSessionTable.anonymousId] = anonymousId
                it[AnalyticsSessionTable.analyticsUserId] = analyticsUserId
                it[startedAt] = occurredAt
                it[lastEventAt] = occurredAt
            }
        }
    }

    private fun updateSessionLastEvent(
        sessionId: UUID,
        occurredAt: Instant,
    ) {
        AnalyticsSessionTable.update({ AnalyticsSessionTable.id eq sessionId }) {
            it[lastEventAt] = occurredAt
        }
    }

    private fun parseInstant(value: String): Instant =
        try {
            Instant.parse(value)
        } catch (e: Exception) {
            throw ApiException("INVALID_EVENT_SCHEMA", HttpStatusCode.BadRequest, "유효하지 않은 시간 형식입니다: $value")
        }
}
