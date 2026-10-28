package io.github.chaevi.analytics.backend.common

import kotlinx.datetime.Instant

private val ZONE_KST = java.time.ZoneId.of("Asia/Seoul")

/** 날짜만 넘기면 KST 해당일 00:00:00 (= UTC 전날 15:00:00), ISO면 그대로 파싱 */
fun parseStartInstant(value: String): Instant =
    if (value.contains("T")) {
        Instant.parse(value)
    } else {
        val kstStart =
            java.time.LocalDate
                .parse(value)
                .atStartOfDay(ZONE_KST)
        Instant.parse(kstStart.toInstant().toString())
    }

/** 날짜만 넘기면 KST 해당일 23:59:59 (= UTC 당일 14:59:59), ISO면 그대로 파싱 */
fun parseEndInstant(value: String): Instant =
    if (value.contains("T")) {
        Instant.parse(value)
    } else {
        val kstEnd =
            java.time.LocalDate
                .parse(value)
                .atTime(23, 59, 59)
                .atZone(ZONE_KST)
        Instant.parse(kstEnd.toInstant().toString())
    }
