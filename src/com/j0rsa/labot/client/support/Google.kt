package com.j0rsa.labot.client.support

import io.ktor.client.HttpClient
import io.ktor.client.engine.cio.CIO
import io.ktor.client.request.get
import io.ktor.client.request.parameter
import io.ktor.client.statement.bodyAsText

object Google {
    private val client = HttpClient(CIO)

    suspend fun translateEnToRu(text: String) =
        client.get("https://translate.googleapis.com/translate_a/single") {
            parameter("client", "gtx")
            parameter("sl", "en")
            parameter("tl", "ru")
            parameter("dt", "t")
            parameter("q", text)
        }.bodyAsText()
            .replaceFirst("[[[\"", "")
            .replaceAfter("\",\"", "")
            .replace("\",\"", "")
}
