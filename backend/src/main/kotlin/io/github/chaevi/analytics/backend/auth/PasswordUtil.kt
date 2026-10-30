package io.github.chaevi.analytics.backend.auth

import java.security.MessageDigest

/**
 * 로컬 개발용 간이 비밀번호 해싱.
 * 운영 전에 BCrypt 등으로 교체 필수.
 */
object PasswordUtil {
    fun hash(password: String): String {
        val digest = MessageDigest.getInstance("SHA-256")
        val bytes = digest.digest(password.toByteArray())
        return bytes.joinToString("") { "%02x".format(it) }
    }

    fun verify(
        password: String,
        hashedPassword: String,
    ): Boolean = hash(password) == hashedPassword
}
