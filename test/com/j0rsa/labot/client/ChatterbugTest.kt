package com.j0rsa.labot.client

import com.j0rsa.labot.AppConfig
import com.j0rsa.labot.client.Chatterbug.Companion.similarity
import com.j0rsa.labot.client.Chatterbug.Companion.wrapExampleInC1
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

class ChatterbugTest : StringSpec({
    "!get words" {
        val res = Chatterbug("test@gmail.com", "pass")
            .login()
            .getWordsFromUnit("139")
        val json = Json { prettyPrint = true }
        println(json.encodeToString(res))
    }

    "check similarity" {
        similarity("asd", "asd") shouldBe 1.0f
        (similarity("oberflächlich", "Oberfläche".lowercase()) * 100).toInt() shouldBe 69

        val word = "Oberfläche"
        "Es gibt einige oberflächliche Gründe.".split(" ").map {
            it to similarity(it, word)
        }.maxBy { it.second }.first shouldBe "oberflächliche"
    }

    "c1 wrapped" {
        "Tom ist oberflächlich.".wrapExampleInC1("Oberfläche") shouldBe
            "Tom ist {{c1::oberflächlich}}."
        "Es gibt einige oberflächliche Gründe.".wrapExampleInC1("Oberfläche") shouldBe
            "Es gibt einige {{c1::oberflächliche}} Gründe."
        "Warum steigen Champagnerblasen an die Oberfläche?".wrapExampleInC1("Oberfläche") shouldBe
            "Warum steigen Champagnerblasen an die {{c1::Oberfläche}}?"
        "Seine Oberfläche war glatt wie ein Spiegel.".wrapExampleInC1("Oberfläche") shouldBe
            "Seine {{c1::Oberfläche}} war glatt wie ein Spiegel."
    }

    "anki sync test" {
        val config = AppConfig.config.chatterbug
        val results = Chatterbug(config.user, config.password).login().getWordsFromUnit("139")
        val notes = Chatterbug.toAnkiClozeNote(results, "test")
        val anki = Anki("http://10.43.149.198") // key
        anki.addNotes(notes)
        anki.sync()
    }
})
