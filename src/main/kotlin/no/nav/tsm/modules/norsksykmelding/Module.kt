package no.nav.tsm.modules.norsksykmelding

import io.ktor.server.application.*
import io.ktor.server.plugins.di.*
import no.nav.tsm.modules.norsksykmelding.service.NorskSykmeldingService

fun Application.configureNorskSykmelding() {
    configureDependencies()
}

private fun Application.configureDependencies() {

    dependencies { provide(NorskSykmeldingService::class) }
}
