package io.github.chaevi.analytics.backend.credential

import java.security.MessageDigest
import java.security.SecureRandom

object IngestionKeyUtil {
    private const val PREFIX = "ink_"
    private const val KEY_LENGTH = 32
    private val random = SecureRandom()

    fun generate(): String {
        val bytes = ByteArray(KEY_LENGTH)
        random.nextBytes(bytes)
        val hex = bytes.joinToString("") { "%02x".format(it) }
        return "$PREFIX$hex"
    }

    fun hash(key: String): String {
        val digest = MessageDigest.getInstance("SHA-256")
        val bytes = digest.digest(key.toByteArray())
        return bytes.joinToString("") { "%02x".format(it) }
    }

    fun prefix(key: String): String = key.take(12)
}
