package com.j0rsa.labot.client

import com.j0rsa.labot.client.support.Google
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.collections.shouldBeOneOf
import io.kotest.matchers.shouldBe

class GoogleTest : StringSpec({

    "translate" {
        Google.translateEnToRu("Some example") shouldBeOneOf listOf(
            "Какой-то пример",
            "Некоторый пример",
        )
    }
})
