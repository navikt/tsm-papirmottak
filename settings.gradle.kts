rootProject.name = "tsm-papirmottak"


val ktorVersion = "3.6.1"
val tsmKtorVersion = "1.3.8"

dependencyResolutionManagement {
    repositories {
        maven("https://github-package-registry-mirror.gc.nav.no/cached/maven-release") {
            content {
                includeGroup("no.nav.tsm")
                includeGroup("no.nav.teamdokumenthandtering")
            }
        }
        maven("https://jitpack.io") {
            content {
                includeGroup("dev.hayden")
            }
        }
        maven("https://packages.confluent.io/maven/") {
            content {
                includeGroup("io.confluent")
                includeModule("org.apache.kafka", "kafka-clients")
            }
        }
        mavenCentral {
            content {
                excludeGroup("no.nav.teamdokumenthandtering")
                excludeGroup("no.nav.tsm")
                excludeGroup("dev.hayden")
            }
        }

    }

    versionCatalogs {
        create("ktorLibs").from("io.ktor:ktor-version-catalog:${ktorVersion}")
        create("tsmKtorLibs").from("no.nav.tsm:ktor-version-catalog:${tsmKtorVersion}")
    }
}

pluginManagement {
    repositories {
        gradlePluginPortal()
        mavenCentral()
    }
}

plugins {
    id("io.github.ben-manes.versions.settings") version "0.67.0"
}
