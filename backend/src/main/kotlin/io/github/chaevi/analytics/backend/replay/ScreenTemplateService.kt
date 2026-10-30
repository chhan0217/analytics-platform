package io.github.chaevi.analytics.backend.replay

import io.github.chaevi.analytics.backend.application.ApplicationRepository
import io.github.chaevi.analytics.backend.common.ApiException
import io.github.chaevi.analytics.backend.credential.AppCredentialRepository
import io.github.chaevi.analytics.backend.credential.IngestionKeyUtil
import io.github.oshai.kotlinlogging.KotlinLogging
import io.ktor.http.HttpStatusCode
import org.jetbrains.exposed.sql.and
import org.jetbrains.exposed.sql.insert
import org.jetbrains.exposed.sql.selectAll
import org.jetbrains.exposed.sql.transactions.transaction
import java.util.UUID

private val logger = KotlinLogging.logger {}

class ScreenTemplateService(
    private val credentialRepository: AppCredentialRepository = AppCredentialRepository(),
    private val applicationRepository: ApplicationRepository = ApplicationRepository(),
) {
    @Suppress("ThrowsCount", "LongMethod")
    fun ingestTemplates(
        ingestionKey: String?,
        request: ScreenTemplateRequest,
    ): ScreenTemplateResponse {
        if (ingestionKey.isNullOrBlank()) {
            throw ApiException(
                "MISSING_INGESTION_KEY",
                HttpStatusCode.Unauthorized,
                "X-Ingestion-Key 헤더가 필요합니다",
            )
        }

        val keyHash = IngestionKeyUtil.hash(ingestionKey)
        val credential =
            transaction { credentialRepository.findByKeyHash(keyHash) }
                ?: throw ApiException(
                    "INVALID_INGESTION_KEY",
                    HttpStatusCode.Unauthorized,
                    "유효하지 않은 수집 키입니다",
                )

        if (credential.status != "ACTIVE") {
            throw ApiException(
                "REVOKED_INGESTION_KEY",
                HttpStatusCode.Unauthorized,
                "폐기된 수집 키입니다",
            )
        }

        val application =
            transaction { applicationRepository.findById(UUID.fromString(credential.applicationId)) }
                ?: throw ApiException(
                    "INTERNAL_ERROR",
                    HttpStatusCode.InternalServerError,
                    "앱 정보를 찾을 수 없습니다",
                )

        val projectId = UUID.fromString(application.projectId)
        val applicationId = UUID.fromString(application.id)

        var accepted = 0
        var skipped = 0

        transaction {
            for (template in request.templates) {
                val exists =
                    ScreenTemplateTable
                        .selectAll()
                        .where {
                            (ScreenTemplateTable.projectId eq projectId) and
                                (ScreenTemplateTable.applicationId eq applicationId) and
                                (ScreenTemplateTable.screenId eq template.screenId) and
                                (ScreenTemplateTable.appVersion eq request.appVersion)
                        }.count() > 0

                if (exists) {
                    skipped++
                    continue
                }

                ScreenTemplateTable.insert {
                    it[ScreenTemplateTable.projectId] = projectId
                    it[ScreenTemplateTable.applicationId] = applicationId
                    it[screenId] = template.screenId
                    it[appVersion] = request.appVersion
                    it[viewportWidth] = template.viewport.width
                    it[viewportHeight] = template.viewport.height
                    it[wireframe] = template.nodes.toString()
                }
                accepted++
            }
        }

        logger.info {
            "화면 템플릿 수집: accepted=$accepted, skipped=$skipped, " +
                "version=${request.appVersion}, app=$applicationId"
        }
        return ScreenTemplateResponse(accepted = accepted, skipped = skipped)
    }
}
