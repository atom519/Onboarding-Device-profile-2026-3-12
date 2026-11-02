package com.example.util

import java.nio.charset.StandardCharsets

object Base64Util {

    fun encode(bytes: ByteArray): String {
        return try {
            java.util.Base64.getEncoder().encodeToString(bytes)
        } catch (_: Throwable) {
            android.util.Base64.encodeToString(bytes, android.util.Base64.NO_WRAP)
        }
    }

    fun encode(s: String): String = encode(s.toByteArray(StandardCharsets.UTF_8))

    fun decode(s: String): ByteArray {
        val clean = s.trim().replace("\n", "").replace("\r", "")
        return try {
            java.util.Base64.getDecoder().decode(clean)
        } catch (_: Throwable) {
            android.util.Base64.decode(clean, android.util.Base64.DEFAULT)
        }
    }

    fun decodeToString(s: String): String = String(decode(s), StandardCharsets.UTF_8)
}
