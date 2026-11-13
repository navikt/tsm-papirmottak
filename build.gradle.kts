import com.diffplug.gradle.spotless.SpotlessExtension
import com.github.benmanes.gradle.versions.updates.DependencyUpdatesTask
import dev.detekt.gradle.Detekt

group = "no.nav.tsm"
version = "0.0.1"

plugins {
    alias(libs.plugins.kotlin.jvm)
    alias(ktorLibs.plugins.ktor)
    alias(libs.plugins.spotless)
    alias(libs.plugins.detekt)
}

application {
    mainClass = "io.ktor.server.netty.EngineMain"
}

dependencies {
    implementation(ktorLibs.server.core)
    implementation(ktorLibs.server.netty)


    implementation(tsmKtorLibs.core)
    implementation(tsmKtorLibs.kafka)
    implementation(libs.kafka.avro)
    implementation(libs.teamdokumenthandtering.avro)

    implementation(libs.logback.classic)
    implementation(libs.logback.encoder)
    constraints {
        implementation(libs.jackson.core) {
            because("Vulnerability CVE-2026-29062 from libs.logback.encoder")
        }
    }

    testImplementation(libs.kotlin.test.junit)
    testImplementation(ktorLibs.server.testHost)
    testImplementation(ktorLibs.serialization.jackson3)
    testImplementation(ktorLibs.client.contentNegotiation)
    testImplementation(tsmKtorLibs.kafka.test)
    testImplementation(libs.kotest)


}

kotlin {
    jvmToolchain {
        languageVersion.set(libs.versions.jvmVersion.map { JavaLanguageVersion.of(it) })
    }
}

tasks {
    shadowJar {
        duplicatesStrategy = DuplicatesStrategy.INCLUDE
        mergeServiceFiles {}
        from("src/main/resources/logback.xml") {
            into("/")
        }
    }

    configure<SpotlessExtension> {
        kotlin { ktfmt(libs.versions.ktfmt.get()).kotlinlangStyle() }
    }

    check {
        setDependsOn(dependsOn.filter { !it.toString().contains("detekt") })
        dependsOn("spotlessApply")
    }

    register<Exec>("preRunLocal") {
        description = "running application local"
        group = "application"
        commandLine("./scripts/pre-dev.sh")
    }

    register<JavaExec>("runLocal") {
        description = "Running application locally"
        group = "application"
        mainClass.set("io.ktor.server.netty.EngineMain")
        classpath = sourceSets["main"].runtimeClasspath

        args("-config=application-local.conf")
        jvmArgs("-Dio.ktor.development=true", "-Dlogback.configurationFile=logback-local.xml")

        dependsOn("preRunLocal")

    }

    withType<Detekt>().configureEach {
        config.setFrom(file("detekt.yml"))
        buildUponDefaultConfig = true

        dependsOn("spotlessApply")
    }

    named<DependencyUpdatesTask>("dependencyUpdates") {
        fun String.isNonStable(): Boolean {
            val stableKeyword = listOf("RELEASE", "FINAL", "GA").any { uppercase().contains(it) }
            val regex = "^[0-9,.v-]+(-r)?$".toRegex()
            val isStable = stableKeyword || regex.matches(this)
            return isStable.not()
        }

        rejectVersionIf {
            candidate.version.isNonStable()
        }
    }

}
