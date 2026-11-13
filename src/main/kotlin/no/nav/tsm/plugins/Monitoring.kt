package no.nav.tsm.plugins

import io.ktor.server.application.*
import no.nav.tsm.ktor.nais.NaisMonitoring

fun Application.configureMonitoring() {
    install(NaisMonitoring)
}
