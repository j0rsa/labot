package com.j0rsa.labot

import com.typesafe.config.ConfigFactory
import io.github.config4k.extract

object AppConfig {
    val config: Config = ConfigFactory.load().extract()
}

data class Config(
    val database: DatabaseConfig,
    val anki: AnkiConfig,
    val skyeng: SkyengConfig,
    val chatterbug: ChatterbugConfig,
)

data class DatabaseConfig(
    val url: String,
    val user: String,
    val password: String
)

data class AnkiConfig(
    val url: String,
    val deck: String,
)

data class SkyengConfig(
    val user: String,
    val password: String,
    val studentId: String,
    val uploadAfter: String? = null
)

data class ChatterbugConfig(
    val user: String,
    val password: String,
)
