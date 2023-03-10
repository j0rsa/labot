package com.j0rsa.labot.client

import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.engine.cio.CIO
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.request.get
import io.ktor.client.request.parameter
import io.ktor.serialization.kotlinx.json.json
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

object Tatoeba {
    @OptIn(ExperimentalSerializationApi::class)
    private val client = HttpClient(CIO) {
        install(ContentNegotiation) {
            json(
                Json {
                    prettyPrint = true
                    encodeDefaults = true
                    explicitNulls = false
                    ignoreUnknownKeys = true
                }
            )
        }
    }

    suspend fun getVariousPhrases(phrase: String): Set<TatoebaShortPhrase> =
        (getPhrases("=$phrase").short(2) + getPhrases(phrase).short(3)).toSet()

    suspend fun getPhrases(word: String): TatoebaPhrases {
        return client.get("https://tatoeba.org/eng/api_v0/search") {
            parameter("from", "deu")
            parameter("to", "eng")
            parameter("trans_to", "eng")
            parameter("query", word)
            parameter("sort", "relevance")
            parameter("orphans", "no")
            parameter("unapproved", "no")
            parameter("page", "1")
        }.body()
    }

    @Serializable
    data class TatoebaPhrases(
        val results: List<TatoebaResult>,
    ) {
        suspend fun short(limit: Int = 3): List<TatoebaShortPhrase> = results.mapNotNull {
            val en = (it.translations.flatten().firstOrNull() ?: return@mapNotNull null).text
            val ru = Google.translateEnToRu(en)
            TatoebaShortPhrase(
                it.text,
                en,
                ru,
            )
        }.take(limit)
    }

    @Serializable
    data class TatoebaResult(
        val text: String,
        val translations: List<List<TatoebaTranslation>>
    )

    @Serializable
    data class TatoebaTranslation(
        val text: String
    )

    data class TatoebaShortPhrase(
        val phrase: String,
        val en: String,
        val ru: String,
    )
}
