package com.j0rsa.labot.ktor

import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter

data class SetCookie(
    val name: String,
    val value: String,
    val expiration: Long?
) {
    fun isExpired(): Boolean = expiration?.let { it < System.currentTimeMillis() } ?: false

    companion object {
        private val format = DateTimeFormatter.ofPattern("EEE, dd-LLL-yyyy HH:mm:ss z")
        fun parse(raw: String): SetCookie? {
            val parts = raw.split(";")
            val mainContent = parts.firstOrNull() ?: return null
            val mainParts = mainContent.split("=")

            val name = mainParts.firstOrNull() ?: return null
            val value = mainParts.lastOrNull() ?: return null

            val expires = parts.find { it.trim().split("=")[0].lowercase() == "expires" }
                ?.let { it.split("=").lastOrNull() ?: return null }
                ?.let { ZonedDateTime.parse(it, format).toEpochSecond() * 1000 }

            return SetCookie(name, value, expires)
        }
    }
}
