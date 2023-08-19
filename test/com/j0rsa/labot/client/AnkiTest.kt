package com.j0rsa.labot.client

import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe
import java.util.UUID

class AnkiTest : StringSpec({
    val anki = Anki("http://10.43.149.198")
    "sync" {
        val sync = anki.sync()
        sync.error shouldBe null
    }

    "add card" {
        val notes = (0 until 100).map {
            note()
        }
        val response = anki.addNotes(notes)
        println(response)
    }
})

fun note(text: String = UUID.randomUUID().toString()) =
    Anki.Companion.Note(
        deckName = "test2",
        modelName = "Cloze",
        fields = Anki.Companion.Fields(text),
        tags = listOf("test", "labot"),
        audio = emptyList(),
        picture = emptyList()
    )
