package com.example.security

import java.security.MessageDigest
import java.security.SecureRandom

object PasscodeCrypto {

    fun generateSaltHex(): String {
        val bytes = ByteArray(16)
        SecureRandom().nextBytes(bytes)
        return bytes.joinToString("") { "%02x".format(it) }
    }

    fun hashPasscode(passcode: String, saltHex: String): String {
        val digest = MessageDigest.getInstance("SHA-256")
        val input = "$saltHex:$passcode".toByteArray(Charsets.UTF_8)
        return digest.digest(input).joinToString("") { "%02x".format(it) }
    }

    fun verifyPasscode(candidate: String, saltHex: String, expectedHashHex: String): Boolean {
        if (expectedHashHex.isBlank() || saltHex.isBlank()) return false
        val actual = hashPasscode(candidate, saltHex).toByteArray(Charsets.UTF_8)
        val expected = expectedHashHex.toByteArray(Charsets.UTF_8)
        return MessageDigest.isEqual(actual, expected)
    }
}
