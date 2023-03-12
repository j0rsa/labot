package com.j0rsa.labot.client

import com.expediagroup.graphql.client.ktor.GraphQLKtorClient
import com.j0rsa.chatterbug.generated.UnitsWordsQuery
import com.j0rsa.chatterbug.generated.enums.LanguageEnum
import com.j0rsa.labot.StringUtils.similarityWith
import com.j0rsa.labot.client.support.Tatoeba
import io.ktor.client.HttpClient
import io.ktor.client.engine.cio.CIO
import io.ktor.client.plugins.cookies.AcceptAllCookiesStorage
import io.ktor.client.plugins.cookies.HttpCookies
import io.ktor.client.request.forms.submitForm
import io.ktor.client.request.get
import io.ktor.client.request.headers
import io.ktor.client.statement.bodyAsText
import io.ktor.http.Parameters
import org.jsoup.Jsoup
import java.net.URL

class Chatterbug(
    private val user: String,
    private val password: String,
    private val nativeLanguage: LanguageEnum = LanguageEnum.EN,
    private val learningLanguage: LanguageEnum = LanguageEnum.DE
) {
    private val ktorClient = HttpClient(CIO) {
        install(HttpCookies) {
            storage = AcceptAllCookiesStorage()
        }
    }
    private val host = "https://app.chatterbug.com/"
    private val client = GraphQLKtorClient(
        url = URL("$host/api/graphql"),
        httpClient = ktorClient
    )

    suspend fun login(): Chatterbug {
        val responseText = ktorClient.get("$host/login").bodyAsText()
        val jsoup = Jsoup.parse(responseText)
        val csrfParameter = jsoup.select("meta[name=csrf-param]")
            .first()?.attr("content") ?: ""
        val csrfToken = jsoup.select("meta[name=csrf-token]")
            .first()?.attr("content") ?: ""
        ktorClient.submitForm(
            "$host/login",
            Parameters.build {
                append("user[login]", user)
                append("user[password]", password)
                append("commit", "Login")
                append(csrfParameter, csrfToken)
            }
        ) {
            headers {
                append("Content-Type", "application/x-www-form-urlencoded")
            }
        }
        return this
    }

    suspend fun getWordsFromUnit(unitId: String): UnitsWordsQuery.Result? {
        val variables = UnitsWordsQuery.Variables(unitId, nativeLanguage, learningLanguage)
        val request = UnitsWordsQuery(variables)
        return client.execute(request).data
    }

    companion object {
        suspend fun toAnkiClozeNote(result: UnitsWordsQuery.Result?, deckName: String) =
            result?.curriculum?.unit?.words?.mapNotNull {
                val originalPhrase = it.word ?: return@mapNotNull null
                val translation = it.translations?.firstOrNull() ?: return@mapNotNull null

                val wordSoundAttachment = it.recordings?.firstOrNull()?.recordingUrl?.let {
                    val filename = it.split("?")[0].split("/").last()
                    Anki.Companion.Attachment(url = it, filename)
                }

                val examples = Tatoeba.getVariousPhrases(originalPhrase)
                examples.map {
                    val cWrappedExample = it.phrase.wrapExampleInC1(originalPhrase)
                    val maskedText = """
                    |$cWrappedExample<br/>
                    |- <b>$translation</b><br/>
                    |${if (wordSoundAttachment != null) "{{c1::[sound:${wordSoundAttachment.filename}]}}<br/>" else ""}
                    |${it.en}<br/>
                    |${it.ru}
                """.trimMargin()
                    Anki.Companion.Note(
                        deckName = deckName,
                        modelName = "Cloze",
                        fields = Anki.Companion.Fields(maskedText),
                        tags = listOf("chatterbug", "tatoeba", "labot"),
                        audio = wordSoundAttachment?.let { listOf(it) } ?: emptyList(),
                        picture = emptyList()
                    )
                }
            }?.flatten() ?: emptyList()

        fun String.wrapExampleInC1(word: String): String {
            val wordToWrap = split(" ").map {
                it to it.lowercase().similarityWith(word.lowercase())
            }.maxBy { it.second }.first.replace("[.,?!-:()]".toRegex(), "")
            return replace(wordToWrap, "{{c1::$wordToWrap}}")
        }
    }
}
