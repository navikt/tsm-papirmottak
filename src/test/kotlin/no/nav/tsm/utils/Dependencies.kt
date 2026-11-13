package no.nav.tsm.utils

import io.ktor.server.plugins.di.*
import io.ktor.server.testing.ApplicationTestBuilder
import no.nav.tsm.core.Environment
import no.nav.tsm.core.Runtime
import no.nav.tsm.ktor.nais.RuntimeCluster
import no.nav.tsm.module

fun ApplicationTestBuilder.configureFullIntegrationTests() {
    application.dependencies { provide<Environment> { createIntegrationEnvironment() } }

    application.module()
}

fun createIntegrationEnvironment() =
    Environment(runtime = Runtime(env = RuntimeCluster.LOCAL, name = "test-app"))
