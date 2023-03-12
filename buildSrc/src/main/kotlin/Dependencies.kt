@file:Suppress("MemberVisibilityCanBePrivate")

object Jvm {
    const val version = "11"
}

object Kotlin {
    const val version = "1.8.0"
    const val stdlib = "org.jetbrains.kotlin:kotlin-stdlib:$version"
    const val reflect = "org.jetbrains.kotlin:kotlin-reflect:$version"
    const val jvmId = "jvm"
    const val kaptId = "kapt"
    const val serialization = "plugin.serialization"
}

object Dependencies {
    object Config4k {
        const val lib = "io.github.config4k:config4k:0.5.0"
    }

    object Kotlinx {
        val serialzation = "org.jetbrains.kotlinx:kotlinx-serialization-json:1.4.1"
    }

    object Arrow {
        private const val version = "1.1.5"
        const val stack = "io.arrow-kt:arrow-stack:$version"
        const val core = "io.arrow-kt:arrow-core"
        const val coreRetrofit = "io.arrow-kt:arrow-core-retrofit"
        const val fxCoroutines = "io.arrow-kt:arrow-fx-coroutines"
        const val fxStm = "io.arrow-kt:arrow-fx-stm"
        const val optics = "io.arrow-kt:arrow-optics"
    }

    object Logback {
        private const val version = "1.2.3"
        const val classic = "ch.qos.logback:logback-classic:$version"
        const val conditions = "org.codehaus.janino:janino:3.0.6"
    }

    object Slf4j {
        private const val version = "1.7.32"
        const val log4jBridge = "org.slf4j:log4j-over-slf4j:$version"
        const val jclBridge = "org.slf4j:jcl-over-slf4j:$version"
    }

	object KoTest {
		private const val version = "5.5.4"
		const val runner = "io.kotest:kotest-runner-junit5-jvm:$version"
		const val assertions = "io.kotest:kotest-assertions-core-jvm:$version"
		const val property = "io.kotest:kotest-property-jvm:$version"
	}

	object Database {
		const val driver = "org.postgresql:postgresql:42.5.1"
	}

    object Jsoup {
        private const val version = "1.15.3"
        const val lib = "org.jsoup:jsoup:$version"
    }

    object Ktor {
        private const val version = "2.2.4"
        const val clientCore = "io.ktor:ktor-client-core:$version"
        const val clientCio = "io.ktor:ktor-client-cio:$version"
        const val clientContentNegitiation = "io.ktor:ktor-client-content-negotiation:$version"
        const val serializationJson = "io.ktor:ktor-serialization-kotlinx-json:$version"
    }

    object Graphql {
        private const val version = "6.4.0"
        const val client = "com.expediagroup:graphql-kotlin-ktor-client:$version"
        const val jackson = "com.expediagroup:graphql-kotlin-client-jackson"
        const val serialization = "com.expediagroup:graphql-kotlin-client-serialization:$version"
    }

    object Telegram {
        private const val version = "6.0.7"
        const val bot = "io.github.kotlin-telegram-bot.kotlin-telegram-bot:telegram:$version"
    }


    object Mockk {
        const val lib = "io.mockk:mockk:1.13.3"
    }

}

sealed class Plugins(val pluginId: String, val version: String) {
    object Shadow: Plugins("com.github.johnrengelman.shadow","6.1.0")
    object Docker: Plugins("com.palantir.docker", "0.26.0")
    object DockerCompose: Plugins("com.avast.gradle.docker-compose", "0.14.2")
    object KtLint: Plugins("org.jlleitschuh.gradle.ktlint", "10.2.1")
    object KtLintIdea: Plugins("org.jlleitschuh.gradle.ktlint-idea", "10.2.1")
    object GraphQL: Plugins("com.expediagroup.graphql", "6.4.0")
}
