package com.j0rsa.labot

import com.typesafe.config.ConfigFactory
import io.github.config4k.extract

class AppConfig {
    val config: Config = ConfigFactory.load().extract()
}

data class Config(
    val database: DatabaseConfig,
    val anki: AnkiConfig,
    val skyeng: SkyengConfig
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
)
