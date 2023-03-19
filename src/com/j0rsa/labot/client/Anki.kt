package com.j0rsa.labot.client

import com.j0rsa.labot.loggerFor
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.engine.cio.CIO
import io.ktor.client.plugins.HttpRequestRetry
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.plugins.cookies.AcceptAllCookiesStorage
import io.ktor.client.plugins.cookies.HttpCookies
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.http.ContentType
import io.ktor.http.contentType
import io.ktor.http.isSuccess
import io.ktor.serialization.kotlinx.json.json
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

/**
 * Implementation of the spec
 * https://foosoft.net/projects/anki-connect/
 */
class Anki(
    private val url: String
) {
    val log = loggerFor<Anki>()

    @OptIn(ExperimentalSerializationApi::class)
    private val client = HttpClient(CIO) {
        install(HttpRequestRetry) {
            maxRetries = 5
            retryIf { _, response ->
                !response.status.isSuccess()
            }
            delayMillis { retry ->
                retry * 3000L
            }
        }
        install(ContentNegotiation) {
            val json = Json {
                prettyPrint = true
                encodeDefaults = true
                explicitNulls = false
                ignoreUnknownKeys = true
            }
            json(json)
            json(json, contentType = ContentType.Text.Any)
        }
        install(HttpCookies) {
            storage = AcceptAllCookiesStorage()
        }
    }

    private suspend fun sendAction(action: String, data: Note? = null) = client.post(url) {
        contentType(ContentType.Application.Json)
        setBody(Action(action = action, params = data?.let { NoteWrapper(it) }))
    }.body<AnkiResponse>()

    suspend fun sync() = sendAction("sync")

    suspend fun addNote(note: Note) = sendAction("addNote", note)

    suspend fun addNotes(notes: Collection<Note>) = run {
        log.info("adding notes")
        val chunks = notes.chunked(10)
        val size = chunks.size
        chunks.withIndex().map { (i, v) ->
            client.post(url) {
                contentType(ContentType.Application.Json)
                setBody(Action(action = "addNotes", params = NoteWrapper(notes = v)))
            }.body<AnkiResponse>().also {
                log.info("Finished chunk (${i + 1}/$size)...")
            }
        }.also {
            log.info("Finished!")
        }.mapNotNull { it.error }.joinToString("\n").let {
            AnkiResponse(error = it)
        }
    }

    companion object {
        @Serializable
        data class Action(
            val version: Int = 6,
            val action: String,
            val params: NoteWrapper? = null
        )

        @Serializable
        data class AnkiResponse(
            val error: String? = null
        )

        @Serializable
        data class NoteWrapper(
            val note: Note? = null,
            val notes: Collection<Note>? = null,
        )

        @Serializable
        data class Note(
            val deckName: String,
            val modelName: String,
            val fields: Fields,
            val options: Options = Options(),
            val tags: Collection<String>,
            val audio: Collection<Attachment>,
            val picture: Collection<Attachment>,
        )

        @Serializable
        data class Fields(
            @SerialName("Text") val text: String
        )

        @Serializable
        data class Options(
            val allowDuplicate: Boolean = false,
            val duplicateScope: String = "deck",
            val duplicateScopeOptions: DuplicateScopeOptions? = DuplicateScopeOptions()
        )

        @Serializable
        data class DuplicateScopeOptions(
            val deckName: String = "Default",
            val checkChildren: Boolean = false,
            val checkAllModels: Boolean = false,
        )

        @Serializable
        data class Attachment(
            val url: String = "",
            val filename: String = "",
            val fields: Collection<String> = listOf("Extra")
        )
    }
}
