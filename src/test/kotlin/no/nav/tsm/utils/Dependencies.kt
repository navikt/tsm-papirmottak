package no.nav.tsm.utils

import io.ktor.server.plugins.di.*
import io.ktor.server.testing.ApplicationTestBuilder
import io.mockk.coEvery
import io.mockk.mockk
import java.time.LocalDateTime
import no.nav.tsm.core.Environment
import no.nav.tsm.core.Runtime
import no.nav.tsm.ktor.nais.RuntimeCluster
import no.nav.tsm.module
import no.nav.tsm.modules.journalfoeringhendelse.saf.SafJournalpostClient
import no.nav.tsm.modules.journalfoeringhendelse.saf.models.Bruker
import no.nav.tsm.modules.journalfoeringhendelse.saf.models.JournalpostMetadata

fun ApplicationTestBuilder.configureFullIntegrationTests() {
    val mockedSafJournalpostClient = mockk<SafJournalpostClient>()

    application.dependencies {
        provide<Environment> { createIntegrationEnvironment() }
        provide<SafJournalpostClient> { mockedSafJournalpostClient }
    }
    coEvery { mockedSafJournalpostClient.getJournalpostMetadata(any()) } answers
        {
            JournalpostMetadata(
                bruker = Bruker("12345678911", "FNR"),
                dokumenter = emptyList(),
                jpErIkkeJournalfort = true,
                gjelderUtland = true,
                datoOpprettet = LocalDateTime.now(),
                dokumentInfoIdPdf = "",
                alleDokumenter = emptyList(),
            )
        }
    application.module()
}

fun createIntegrationEnvironment() =
    Environment(
        runtime = Runtime(env = RuntimeCluster.LOCAL, name = "test-app"),
        safEndpoint = "http://localhost:8818/",
    )
