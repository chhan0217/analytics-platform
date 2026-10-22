package io.github.chaevi.analytics.backend.common

import com.zaxxer.hikari.HikariConfig
import com.zaxxer.hikari.HikariDataSource
import io.github.oshai.kotlinlogging.KotlinLogging
import io.ktor.server.application.Application
import org.flywaydb.core.Flyway
import org.jetbrains.exposed.sql.Database

private val logger = KotlinLogging.logger {}
private var initialized = false

fun Application.configureDatabase() {
    if (initialized) return
    initialized = true

    val host = System.getenv("DB_HOST") ?: "localhost"
    val port = System.getenv("DB_PORT") ?: "5432"
    val name = System.getenv("DB_NAME") ?: "analytics"
    val user = System.getenv("DB_USER") ?: "analytics"
    val password = System.getenv("DB_PASSWORD") ?: "analytics_local"

    val jdbcUrl = "jdbc:postgresql://$host:$port/$name"

    val config =
        HikariConfig().apply {
            this.jdbcUrl = jdbcUrl
            driverClassName = "org.postgresql.Driver"
            username = user
            this.password = password
            maximumPoolSize = 5
            isAutoCommit = false
            validate()
        }

    val dataSource = HikariDataSource(config)

    runFlyway(jdbcUrl, user, password)

    Database.connect(dataSource)

    logger.info { "데이터베이스 연결 완료: $host:$port/$name" }
}

private fun runFlyway(
    jdbcUrl: String,
    user: String,
    password: String,
) {
    val flyway =
        Flyway
            .configure()
            .dataSource(jdbcUrl, user, password)
            .locations("classpath:db/migration")
            .load()

    val result = flyway.migrate()
    logger.info { "Flyway 마이그레이션 완료: ${result.migrationsExecuted}개 실행" }
}
