package com.j0rsa.labot.client

import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe

class GoogleTest : StringSpec({

    "translate" {
        Google.translateEnToRu("Some example") shouldBe "Какой-то пример"
    }
})
