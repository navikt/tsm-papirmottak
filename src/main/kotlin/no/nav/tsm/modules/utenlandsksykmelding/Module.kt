package no.nav.tsm.modules.utenlandsksykmelding

import io.ktor.server.application.*
import io.ktor.server.plugins.di.*
import no.nav.tsm.modules.utenlandsksykmelding.service.UtenlandskSykmeldingService

fun Application.configureUtenlandskSykmelding() {
    configureDependencies()
}

private fun Application.configureDependencies() {

    dependencies { provide(UtenlandskSykmeldingService::class) }
}
