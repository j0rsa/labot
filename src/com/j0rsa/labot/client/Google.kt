package com.j0rsa.labot.client

import io.ktor.client.*
import io.ktor.client.engine.cio.*
import io.ktor.client.request.*
import io.ktor.client.statement.*

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
