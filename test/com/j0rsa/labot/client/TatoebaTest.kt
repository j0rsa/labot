package com.j0rsa.labot.client

import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.ints.shouldBeGreaterThanOrEqual
import io.kotest.matchers.ints.shouldBeLessThanOrEqual

class TatoebaTest : StringSpec({
    "get 5 phrases" {
        val res = Tatoeba.getVariousPhrases("tolerant")
//        println(res)
        res.size shouldBeGreaterThanOrEqual 3
        res.size shouldBeLessThanOrEqual 5
    }
})
