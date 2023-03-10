package com.j0rsa.labot.client

import com.expediagroup.graphql.client.ktor.GraphQLKtorClient
import com.j0rsa.chatterbug.generated.UnitsWordsQuery
import com.j0rsa.chatterbug.generated.enums.LanguageEnum
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
import java.util.Locale

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
                examples.mapNotNull {
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
        fun similarity(s1: String, s2: String): Double {
            var longer = s1
            var shorter = s2
            if (s1.length < s2.length) { // longer should always have greater length
                longer = s2
                shorter = s1
            }
            val longerLength = longer.length
            return if (longerLength == 0) {
                1.0 /* both strings are zero length */
            } else (longerLength - editDistance(longer, shorter)) / longerLength.toDouble()
        }

        // Example implementation of the Levenshtein Edit Distance
        // See http://rosettacode.org/wiki/Levenshtein_distance#Java
        private fun editDistance(s1: String, s2: String): Int {
            var s1 = s1
            var s2 = s2
            s1 = s1.lowercase(Locale.getDefault())
            s2 = s2.lowercase(Locale.getDefault())
            val costs = IntArray(s2.length + 1)
            for (i in 0..s1.length) {
                var lastValue = i
                for (j in 0..s2.length) {
                    if (i == 0) costs[j] = j else {
                        if (j > 0) {
                            var newValue = costs[j - 1]
                            if (s1[i - 1] != s2[j - 1]) newValue = Math.min(
                                Math.min(newValue, lastValue),
                                costs[j]
                            ) + 1
                            costs[j - 1] = lastValue
                            lastValue = newValue
                        }
                    }
                }
                if (i > 0) costs[s2.length] = lastValue
            }
            return costs[s2.length]
        }
        fun String.wrapExampleInC1(word: String): String {
            val wordToWrap = split(" ").map {
                it to similarity(it.lowercase(), word.lowercase())
            }.maxBy { it.second }.first.replace("[.,?!-:()]".toRegex(), "")
            return replace(wordToWrap, "{{c1::$wordToWrap}}")
        }
    }
}
