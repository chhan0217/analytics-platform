package io.github.chaevi.analytics.backend.common

import com.zaxxer.hikari.HikariConfig
import com.zaxxer.hikari.HikariDataSource
import io.github.oshai.kotlinlogging.KotlinLogging
import io.ktor.server.application.Application
import org.jetbrains.exposed.sql.Database

private val logger = KotlinLogging.logger {}

fun Application.configureDatabase() {
    val host = System.getenv("DB_HOST") ?: "localhost"
    val port = System.getenv("DB_PORT") ?: "5432"
    val name = System.getenv("DB_NAME") ?: "analytics"
    val user = System.getenv("DB_USER") ?: "analytics"
    val password = System.getenv("DB_PASSWORD") ?: "analytics_local"

    val config =
        HikariConfig().apply {
            jdbcUrl = "jdbc:postgresql://$host:$port/$name"
            driverClassName = "org.postgresql.Driver"
            username = user
            this.password = password
            maximumPoolSize = 10
            isAutoCommit = false
            validate()
        }

    val dataSource = HikariDataSource(config)
    Database.connect(dataSource)

    logger.info { "데이터베이스 연결 완료: $host:$port/$name" }
}
