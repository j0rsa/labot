package com.j0rsa.labot.client

import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe

class AnkiTest : StringSpec({
    val anki = Anki("http://10.43.149.198")
    "sync" {
        val sync = anki.sync()
        sync.error shouldBe null
    }

    "addNote" {
    }
})
