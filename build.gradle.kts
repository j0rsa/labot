import com.expediagroup.graphql.plugin.gradle.config.GraphQLSerializer
import com.expediagroup.graphql.plugin.gradle.graphql
import com.github.jengelman.gradle.plugins.shadow.tasks.ShadowJar
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    kotlin(Kotlin.jvmId) version Kotlin.version
    kotlin(Kotlin.kaptId) version Kotlin.version
    kotlin(Kotlin.serialization) version Kotlin.version
    listOf(
        Plugins.Shadow,
        Plugins.Docker,
        Plugins.DockerCompose,
        Plugins.KtLint,
        Plugins.KtLintIdea,
        Plugins.GraphQL,
    ).forEach {
        id(it.pluginId) version it.version
    }
    jacoco
    application
}

group = GROUP_ID
version = VERSION

application {
    @Suppress("DEPRECATION")
    mainClassName = MAIN_CLASS
}

repositories {
    mavenCentral()
    maven("https://jitpack.io")
}
dependencies {
    implementation(Kotlin.stdlib)
    implementation(Kotlin.reflect)
    implementation(Dependencies.Kotlinx.serialzation)
    implementation(Dependencies.Config4k.lib)
    api(Dependencies.Logback.classic)
    runtimeOnly(Dependencies.Logback.conditions)
    api(Dependencies.Slf4j.log4jBridge)
    api(Dependencies.Slf4j.jclBridge)

    api(platform(Dependencies.Arrow.stack))
    api(Dependencies.Arrow.core)
    api(Dependencies.Arrow.coreRetrofit)
    api(Dependencies.Arrow.fxCoroutines)
    api(Dependencies.Arrow.fxStm)
    api(Dependencies.Arrow.optics)

    runtimeOnly(Dependencies.Database.driver)

    implementation(Dependencies.Ktor.clientCore)
    implementation(Dependencies.Ktor.clientCio)
    implementation(Dependencies.Ktor.clientContentNegitiation)
    implementation(Dependencies.Ktor.serializationJson)

    implementation(Dependencies.Jsoup.lib)

    implementation(Dependencies.Graphql.client) {
        exclude(Dependencies.Graphql.jackson)
    }
    implementation(Dependencies.Graphql.serialization)
    implementation(Dependencies.Telegram.bot)

    testImplementation(Dependencies.KoTest.runner)
    testImplementation(Dependencies.KoTest.assertions)
    testImplementation(Dependencies.KoTest.property)
    testImplementation(Dependencies.Mockk.lib)
}

graphql {
    client {
        queryFileDirectory = "$projectDir/resources"
        endpoint = "https://app.chatterbug.com/api/graphql"
//        schemaFile = File("$projectDir/resources/schemas/chatterbug/schema.graphql")
        packageName = "com.j0rsa.chatterbug.generated"
        serializer = GraphQLSerializer.KOTLINX
    }
}

tasks {
    init {
        dependsOn(named("ktlintApplyToIdea"))
    }

    withType<KotlinCompile> {
        kotlinOptions {
            jvmTarget = Jvm.version
            freeCompilerArgs = listOf("-Xopt-in=kotlin.time.ExperimentalTime", "-Xopt-in=kotlin.RequiresOptIn")
            languageVersion = "1.7"
        }
    }

    named<JavaExec>("run") {
        args = listOf("-config=resources/application-dev.conf")
        jvmArgs = listOf("-Dconfig.resource=application-dev.conf")
    }

    val combineReports = register<JacocoReport>("combineReports") {
        executionData(fileTree(buildDir.absolutePath).include("jacoco/*.exec"))
        classDirectories.setFrom(sourceSets["main"].output)
        sourceDirectories.setFrom(sourceSets["main"].allSource.srcDirs)
    }

    val mergeCoverage = register<JacocoMerge>("mergeCoverage") {
        destinationFile = File("$buildDir/jacoco/allTestCoverage.exec")
        executionData = project.fileTree(".") {
            include("**/build/jacoco/*.exec")
        }
    }

    test {
        useJUnitPlatform()
        include(listOf("**/*Test.class"))
        finalizedBy(combineReports)
        finalizedBy(mergeCoverage)
    }

    val integrationTest = register<Test>("integration-test") {
        useJUnitPlatform()
        include(listOf("**/*IT.class"))
        dependsOn(test)
        dependsOn(named("composeUp"))
        finalizedBy(named("composeDown"))
        finalizedBy(combineReports)
        finalizedBy(mergeCoverage)
    }

    register("it") {
        dependsOn(integrationTest)
    }

    docker {
        dependsOn(test.get())
    }

    val ktlintFormat = named("ktlintFormat") {
        doLast {
            delete("src/main")
            delete("src/test")
        }
    }

    register("lint") {
        dependsOn(
            ktlintFormat
        )
    }
}

val projectTag = VERSION
val baseDockerName = "$DOCKER_REGISTRY/${project.name}"
val taggedDockerName = "$baseDockerName:$projectTag"

val baseDockerFile = file("$projectDir/docker/Dockerfile")
docker {
    val shadowJar: ShadowJar by tasks
    name = taggedDockerName
    setDockerfile(baseDockerFile)
    tag("DockerTag", taggedDockerName)
    buildArgs(
        mapOf(
            "JAR_FILE" to shadowJar.archiveFileName.get()
        )
    )
    files(
        shadowJar.outputs,
        *file("$projectDir/docs/api").listFiles()
    )
}

dockerCompose {
    useComposeFiles = listOf("$projectDir/docker/docker-compose.yml")
    projectName = "gradle"
    captureContainersOutput = true
    forceRecreate = true
    removeContainers = true
}

kotlin.sourceSets["main"].kotlin.srcDir("src")
kotlin.sourceSets["test"].kotlin.srcDir("test")
sourceSets["main"].resources.srcDir("resources")
sourceSets["test"].resources.srcDir("testresources")

ktlint {
    filter {
        exclude("**/generated/**")
    }
}
