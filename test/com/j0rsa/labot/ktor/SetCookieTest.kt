package com.j0rsa.labot.ktor

import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe

class SetCookieTest : StringSpec({
    "succesfully parse" {
        val raw = "__Secure-3PSIDCC=someValue; expires=Tue, 30-Apr-2024 08:42:25 GMT; path=/; domain=.google.com; Secure; HttpOnly; priority=high; SameSite=none"
        SetCookie.parse(raw) shouldBe SetCookie("__Secure-3PSIDCC", "someValue", 1714466545000)
    }

})
