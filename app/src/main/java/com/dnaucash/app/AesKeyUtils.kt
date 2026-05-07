package com.dnaucash.app

import java.security.SecureRandom

object AesKeyUtils {
    private val secureRandom = SecureRandom()
    private val hexRegex = Regex("^[0-9a-fA-F]{32}$")

    fun generateHexKey(): String {
        val bytes = ByteArray(16)
        secureRandom.nextBytes(bytes)
        return bytes.joinToString("") { "%02x".format(it) }
    }

    fun isValidHexKey(value: String): Boolean = hexRegex.matches(value)
}
