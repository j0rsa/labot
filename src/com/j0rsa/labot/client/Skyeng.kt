package com.j0rsa.labot.client

import com.j0rsa.labot.client.support.Google
import com.j0rsa.labot.ktor.SetCookie
import com.j0rsa.labot.loggerFor
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.engine.cio.CIO
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.plugins.cookies.AcceptAllCookiesStorage
import io.ktor.client.plugins.cookies.HttpCookies
import io.ktor.client.request.bearerAuth
import io.ktor.client.request.forms.submitForm
import io.ktor.client.request.get
import io.ktor.client.request.headers
import io.ktor.client.request.parameter
import io.ktor.client.request.post
import io.ktor.client.statement.bodyAsText
import io.ktor.http.Parameters
import io.ktor.http.URLBuilder
import io.ktor.http.Url
import io.ktor.serialization.kotlinx.json.json
import java.nio.charset.Charset
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.KSerializer
import kotlinx.serialization.Serializable
import kotlinx.serialization.Serializer
import kotlinx.serialization.descriptors.PrimitiveKind
import kotlinx.serialization.descriptors.PrimitiveSerialDescriptor
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import kotlinx.serialization.json.Json
import org.jsoup.Jsoup
import java.time.ZonedDateTime

class Skyeng(
    private val user: String,
    private val password: String,
) {
    private val log = loggerFor<Skyeng>()

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
        install(HttpCookies) {
            storage = AcceptAllCookiesStorage()
        }
    }

    private val host = "https://id.skyeng.ru"
    private val apiHost = "https://api.words.skyeng.ru/api"
    private val dictionaryHost = "https://dictionary.skyeng.ru/api"

    private var authCookie: SetCookie? = null

    suspend fun getCsrf(): String {
        val responseText = client.get("$host/login").bodyAsText(Charset.defaultCharset())
        val jsoup = Jsoup.parse(responseText)
        return jsoup.select("input[name=csrfToken]").first()?.attr("value") ?: ""
    }

    suspend fun login(): String {
        log.info("Login")
        authCookie?.let {
            log.info("auth cookie exists")
            if (!it.isExpired()) {
                log.info("auth cookie not yet expired")
                return it.value
            }
        }
        log.info("Performing login")
        client.submitForm(
            "$host/frame/login-submit",
            Parameters.build {
                append("username", user)
                append("password", password)
                append("redirect", "https://skyeng.ru/")
                append("csrfToken", getCsrf())
            }
        ) {
            headers {
                append("Content-Type", "application/x-www-form-urlencoded")
            }
        }
        return getJwt().ifBlank {
            throw IllegalStateException("Unable to login! Please check your credentials!")
        }
    }

    private suspend fun getJwt(): String {
        log.info("Getting JWT")
        val response = client.post("$host/user-api/v1/auth/jwt")
        val setCookies = response.headers.getAll("Set-Cookie")
        log.info("Setting cookies returned: ${setCookies?.size}")
        return setCookies?.firstOrNull()?.let {
            val cookie = SetCookie.parse(it)
            authCookie = cookie
            log.info("New auth cookie persisted")
            cookie?.value
        } ?: ""
    }

    suspend fun getWordSets(token: String, studentId: String): List<WordSetData> {
        val pageSize = 100
        val firstPage = client.get("$apiHost/for-vimbox/v1/wordsets.json") {
            parameter("page", 1)
            parameter("pageSize", pageSize)
            parameter("studentId", studentId)
            bearerAuth(token)
        }.body<WordSet>()

        return firstPage.data + (2..firstPage.meta.lastPage).flatMap {
            client.get("$apiHost/v1/wordsets.json") {
                parameter("page", it)
                parameter("pageSize", pageSize)
                parameter("studentId", studentId)
                bearerAuth(token)
            }.body<WordSet>().data
        }
    }

    suspend fun getWords(token: String, studentId: String): List<WordOfSet> {
        val wordSets = getWordSets(token, studentId)
        val pageSize = 100
        return wordSets.flatMap { set ->
            val path = "$apiHost/v1/wordsets/${set.id}/words.json"
            val firstPage: Words = client.get(path) {
                parameter("page", 1)
                parameter("pageSize", pageSize)
                parameter("studentId", studentId)
                parameter("acceptLanguage", "ru")
                parameter("noCache", System.currentTimeMillis())
                bearerAuth(token)
            }.body()
            val restPages: List<Words> = (2..firstPage.meta.lastPage).map {
                client.get(path) {
                    parameter("page", it)
                    parameter("pageSize", pageSize)
                    parameter("studentId", studentId)
                    parameter("acceptLanguage", "ru")
                    parameter("noCache", System.currentTimeMillis())
                    bearerAuth(token)
                }.body()
            }
            (restPages + firstPage).flatMap { words ->
                words.data.map {
                    WordOfSet(set, it)
                }
            }
        }
    }

    suspend fun getMeaning(token: String, words: List<WordData>): List<Meaning> = words.chunked(25).flatMap { chunk ->
        val ids = chunk.joinToString(",") { it.meaningId.toString() }
        client.get("$dictionaryHost/for-services/v2/meanings") {
            parameter("ids", ids)
            bearerAuth(token)
        }.body<List<Meaning>>()
    }

    companion object {

        @Serializable
        data class WordSet(
            val meta: Meta,
            val data: List<WordSetData>
        )

        @Serializable
        data class WordSetData(
            val id: Int,
            val title: String,
            val subtitle: String,
        )

        @Serializable
        data class Meta(
            val total: Int,
            val currentPage: Int,
            val lastPage: Int,
            val pageSize: Int,
        )

        @Serializable
        data class WordOfSet(
            val wordSet: WordSetData,
            val word: WordData,
        )

        @Serializable
        data class Words(
            val meta: Meta,
            val data: List<WordData>,
        )

        @Serializable
        data class WordData(
            val meaningId: Int,
            @Serializable(with = ZonedDateTimeSerializer::class) val createdAt: ZonedDateTime,
        )

        @OptIn(ExperimentalSerializationApi::class)
        @Serializer(forClass = ZonedDateTime::class)
        object ZonedDateTimeSerializer : KSerializer<ZonedDateTime> {
            override val descriptor: SerialDescriptor = PrimitiveSerialDescriptor("ZonedDateTime", PrimitiveKind.STRING)

            override fun serialize(encoder: Encoder, value: ZonedDateTime) = encoder.encodeString(value.toString())
            override fun deserialize(decoder: Decoder): ZonedDateTime = ZonedDateTime.parse(decoder.decodeString())
        }

        @Serializable
        data class Meaning(
            val alternatives: List<MeaningAlternative> = emptyList(),
            val definition: MeaningDefinition,
            val examples: List<MeaningDefinition> = emptyList(),
            val id: Int,
            val images: List<MeaningImage> = emptyList(),
            override val soundUrl: String,
            val text: String,
            val transcription: String,
            val translation: Translation
        ) : CustomSoundAttachment {
            fun pictureAttachment(scale: Int = 2, fields: Collection<String> = listOf("Extra")) = run {
                val url = images.firstOrNull()?.url ?: return@run Anki.Companion.Attachment()
                val dimensions =
                    url.replaceBefore("/unsafe/", "").replace("/unsafe/", "").replaceAfter("/", "").replace("/", "")
                val newDimensions = dimensions.split("x").map { it.toInt() * scale }.joinToString("x")
                val filename = url.replaceBeforeLast("/", "").replace("/", "")
                Anki.Companion.Attachment(
                    url.replace(dimensions, newDimensions), filename, fields
                )
            }

            suspend fun toAnkiClozeNote(deckName: String, voice: Voice = Voice.Male2) = examples.map {
                val cWrappedExample = (it.text ?: return@map null).replace("[", "{{c1::").replace("]", "}}")
                val clearExample = it.text.replace("[", "").replace("]", "")
                val wordSoundAttachment = customSoundAttachment(voice) ?: return@map null
                val sentenceSoundAttachment = it.customSoundAttachment(voice) ?: return@map null
                val pictureAttachment = pictureAttachment()
                val maskedText = """
                    |$cWrappedExample<br/>
                    |- <b>${translation.text}</b><br/>
                    |{{c1::[sound:${wordSoundAttachment.filename}]}}<br/>
                    |${Google.translateEnToRu(clearExample)}<br/>
                    |{{c1::[sound:${sentenceSoundAttachment.filename}]}}<br/>
                    |{{c1::<img src='${pictureAttachment.filename}'/>}}
                """.trimMargin()
                Anki.Companion.Note(
                    deckName = deckName,
                    modelName = "Cloze",
                    fields = Anki.Companion.Fields(maskedText),
                    tags = listOf("skyeng", "labot"),
                    audio = listOf(wordSoundAttachment, sentenceSoundAttachment),
                    picture = listOf(pictureAttachment)
                )
            }
        }

        @Suppress("unused")
        enum class Language {
            EN, RU
        }

        @Suppress("unused")
        enum class Voice {
            Male1, Male2, Female1, Female2;

            override fun toString(): String = "${this.name.lowercase().dropLast(1)}_${this.name.takeLast(1)}"
        }

        @Serializable
        data class MeaningAlternative(
            val text: String,
            val translation: Translation?,
        )

        @Serializable
        data class Translation(
            val text: String,
        )

        @Serializable
        data class MeaningDefinition(
            val text: String? = null,
            override val soundUrl: String? = null,
        ) : CustomSoundAttachment

        @Serializable
        data class MeaningImage(
            val url: String,
        )

        interface CustomSoundAttachment {
            val soundUrl: String?
            fun customSoundUrl(voice: Voice = Voice.Male2, language: Language = Language.EN): String? = soundUrl?.let {
                URLBuilder(it).apply {
                    parameters["lang"] = language.name.lowercase()
                    parameters["voice"] = voice.toString()
                }.buildString()
            }

            fun customSoundAttachment(
                voice: Voice = Voice.Male2,
                language: Language = Language.EN,
                fields: Collection<String> = listOf("Extra")
            ) = run {
                val url = customSoundUrl(voice, language)
                url ?: return@run null
                val u = Url(url)
                val filename = listOfNotNull(
                    u.parameters["lang"],
                    "_",
                    u.parameters["voice"],
                    "_",
                    u.parameters["text"]?.replace(" ", "_")?.replace("[^A-Za-z0-9_]".toRegex(), ""),
                    ".mp3"
                ).joinToString("")
                Anki.Companion.Attachment(
                    url, filename, fields
                )
            }
        }
    }
}
