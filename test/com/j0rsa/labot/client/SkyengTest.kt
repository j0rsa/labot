package com.j0rsa.labot.client

import com.j0rsa.labot.AppConfig
import com.j0rsa.labot.client.Skyeng.Companion.Voice
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.ints.shouldBeGreaterThanOrEqual
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import java.time.LocalDate

class SkyengTest : StringSpec({
    val skyengConf = AppConfig.config.skyeng["a"]!!
    val skyeng = Skyeng(
        skyengConf.user,
        skyengConf.password,
        skyengConf.studentId,
    )

    "csrf should not be empty" {
        val csrf = Skyeng("", "", 0L).getCsrf()
        println(csrf)
        csrf shouldNotBe ""
    }

    "!login works" {
        skyeng.login() shouldNotBe ""
    }

    "!getWordSet" {
        val result = skyeng.getWordSets()
        result.size shouldBeGreaterThanOrEqual 1
    }

    "!getWords" {
        val result = skyeng.getWords()
        result.size shouldBeGreaterThanOrEqual 1
    }

    "!getMeanings" {
        val words = skyeng.getWords().map { it.word }
        val meanings = skyeng.getMeaning(words)
        meanings.size shouldBe words.size
    }

    "voice toString" {
        Voice.Female2.toString() shouldBe "female_2"
        "${Voice.Female1}" shouldBe "female_1"
    }

    "custom sound Url" {
        val meaning = Skyeng.Companion.Meaning(
            definition = Skyeng.Companion.MeaningDefinition("some definition", ""),
            id = 123,
            soundUrl =
            "https://vimbox-tts.skyeng.ru/api/v1/tts?text=You+could+never+predict+that!&lang=ru&voice=female_2",
            text = "",
            translation = Skyeng.Companion.Translation("text"),
            transcription = ""
        )

        meaning.customSoundUrl() shouldBe
            "https://vimbox-tts.skyeng.ru/api/v1/tts?text=You+could+never+predict+that!&lang=en&voice=male_2"
    }

    "custom sound Attachment" {
        val meaning = Skyeng.Companion.Meaning(
            definition = Skyeng.Companion.MeaningDefinition("some definition", ""),
            id = 123,
            soundUrl =
            "https://vimbox-tts.skyeng.ru/api/v1/tts?text=You+could+never+predict+that!&lang=ru&voice=female_2",
            text = "",
            translation = Skyeng.Companion.Translation("text"),
            transcription = ""
        )

        meaning.customSoundAttachment() shouldBe Anki.Companion.Attachment(
            "https://vimbox-tts.skyeng.ru/api/v1/tts?text=You+could+never+predict+that!&lang=en&voice=male_2",
            "en_male_2_You_could_never_predict_that.mp3",
            listOf("Extra")
        )
    }

    "picture attachment" {
        val meaning = Skyeng.Companion.Meaning(
            definition = Skyeng.Companion.MeaningDefinition("some definition", ""),
            id = 123,
            soundUrl =
            "https://vimbox-tts.skyeng.ru/api/v1/tts?text=You+could+never+predict+that!&lang=ru&voice=female_2",
            text = "",
            images = listOf(
                Skyeng.Companion.MeaningImage(
                    "https://cdn-user77752.skyeng.ru/skyconvert/unsafe/200x150/" +
                        "https://cdn-user77752.skyeng.ru/images/246df0e95de2ea0f7ac1de1cb4f5aa78.jpeg"
                )
            ),
            translation = Skyeng.Companion.Translation("text"),
            transcription = ""
        )

        meaning.pictureAttachment() shouldBe Anki.Companion.Attachment(
            "https://cdn-user77752.skyeng.ru/skyconvert/unsafe/400x300/https://cdn-user77752.skyeng.ru/" +
                "images/246df0e95de2ea0f7ac1de1cb4f5aa78.jpeg",
            "246df0e95de2ea0f7ac1de1cb4f5aa78.jpeg",
            listOf("Extra")
        )
    }

    "to anki cloze note" {
        val notes = Skyeng.Companion.Meaning(
            alternatives = listOf(),
            definition = Skyeng.Companion.MeaningDefinition(
                text = "Feeling or showing envy of someone or their advantages and achievements.",
                soundUrl = "https://vimbox-tts.skyeng.ru/api/v1/tts?text=Feeling+or+showing+envy+of+someone+or+" +
                    "their+advantages+and+achievements.&lang=en&voice=male_1"
            ),
            examples = listOf(
                Skyeng.Companion.MeaningDefinition(
                    text = "His success has made some of his old friends [jealous].",
                    soundUrl = "https://vimbox-tts.skyeng.ru/api/v1/tts?text=His+success+has+made+some+of+his+old+" +
                        "friends+jealous.&lang=en&voice=male_1"
                ),
                Skyeng.Companion.MeaningDefinition(
                    text = "These shoes are so pretty, I'm so [jealous]!",
                    soundUrl = "https://vimbox-tts.skyeng.ru/api/v1/tts?text=These+shoes+are+so+pretty+I%27m+so+" +
                        "jealous%21&lang=en&voice=male_1"
                )
            ),
            id = 157237,
            images = listOf(
                Skyeng.Companion.MeaningImage(
                    url = "https://cdn-user77752.skyeng.ru/skyconvert/unsafe/200x150/https://cdn-user77752.skyeng.ru/" +
                        "images/7188466db2df0e76cc7291b55fb839d0.jpeg"
                )
            ),
            soundUrl = "https://vimbox-tts.skyeng.ru/api/v1/tts?text=jealous&lang=en&voice=male_1",
            text = "jealous",
            transcription = "ˈʤɛləs",
            translation = Skyeng.Companion.Translation(text = "завистливый")
        ).toAnkiClozeNote("test").filterNotNull()
        notes.size shouldBe 2
        val first = notes.first()
        first.fields.text shouldBe """
            |His success has made some of his old friends {{c1::jealous}}.<br/>
            |- <b>завистливый</b><br/>
            |{{c1::[sound:en_male_2_jealous.mp3]}}<br/>
            |Его успех заставил некоторых из его старых друзей завидовать.<br/>
            |{{c1::[sound:en_male_2_His_success_has_made_some_of_his_old_friends_jealous.mp3]}}<br/>
            |{{c1::<img src='7188466db2df0e76cc7291b55fb839d0.jpeg'/>}}
            """.trimMargin()
    }

    "!full sync test" {
        val words = skyeng.getWords().map { it.word }
        println("Fetched words: ${words.size}")
        // on prod +1 step: to filter out old words
        val meanings = skyeng.getMeaning(words)
        println("Fetched meanings: ${meanings.size}")
        val notes = meanings.flatMap { it.toAnkiClozeNote("skyeng") }.filterNotNull()
        println("Detected notes: ${notes.size}")

//        val anki = Anki("http://10.43.149.198")
        val anki = Anki("http://10.43.9.155") // tv
        println("Uploading notes")
        anki.addNotes(notes)
        println("Syncing")
        anki.sync()
    }

    "!partial sync test" {
        val updateAfter = skyengConf.uploadAfter?.let { LocalDate.parse(it) }
        updateAfter shouldNotBe null
        val words = skyeng.getWords().map { it.word }
            .filter { it.createdAt.toLocalDate() > updateAfter!! }
        println("Fetched words: ${words.size}")
        // on prod +1 step: to filter out old words
        val meanings = skyeng.getMeaning(words)
        println("Fetched meanings: ${meanings.size}")
        val notes = meanings.flatMap { it.toAnkiClozeNote("skyeng") }
            .filterNotNull()
        println("Detected notes: ${notes.size}")

//        val anki = Anki("http://10.43.149.198")
        val anki = Anki("http://10.43.9.155") // tv
        println("Uploading notes")
        anki.addNotes(notes)
        println("Syncing")
        anki.sync()
    }
})
