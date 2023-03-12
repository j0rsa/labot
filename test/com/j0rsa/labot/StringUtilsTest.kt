package com.j0rsa.labot

import com.j0rsa.labot.StringUtils.similarityWith
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe

class StringUtilsTest : StringSpec({
    "check similarity" {
        "asd".similarityWith("asd") shouldBe 1.0f
        ("oberflächlich".similarityWith("Oberfläche".lowercase()) * 100).toInt() shouldBe 69

        val word = "Oberfläche"
        "Es gibt einige oberflächliche Gründe.".split(" ").map {
            it to it.similarityWith(word)
        }.maxBy { it.second }.first shouldBe "oberflächliche"
    }

})
