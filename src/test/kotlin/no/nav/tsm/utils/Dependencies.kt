package no.nav.tsm.utils

import arrow.core.right
import io.ktor.server.plugins.di.*
import io.ktor.server.testing.ApplicationTestBuilder
import io.mockk.coEvery
import io.mockk.mockk
import java.time.LocalDate
import java.time.LocalDateTime
import no.nav.tsm.core.Environment
import no.nav.tsm.core.Runtime
import no.nav.tsm.ktor.nais.RuntimeCluster
import no.nav.tsm.module
import no.nav.tsm.modules.journalfoeringhendelse.pdl.PdlClient
import no.nav.tsm.modules.journalfoeringhendelse.pdl.models.PdlIdent
import no.nav.tsm.modules.journalfoeringhendelse.pdl.models.PdlIdentgruppe
import no.nav.tsm.modules.journalfoeringhendelse.pdl.models.PdlPerson
import no.nav.tsm.modules.journalfoeringhendelse.saf.SafJournalpostClient
import no.nav.tsm.modules.journalfoeringhendelse.saf.models.Bruker
import no.nav.tsm.modules.journalfoeringhendelse.saf.models.JournalpostMetadata

fun ApplicationTestBuilder.configureFullIntegrationTests() {
    val mockedSafJournalpostClient = mockk<SafJournalpostClient>()
    val mockedPdlClient = mockk<PdlClient>()

    application.dependencies {
        provide<Environment> { createIntegrationEnvironment() }
        provide<SafJournalpostClient> { mockedSafJournalpostClient }
        provide<PdlClient> { mockedPdlClient }
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

    coEvery { mockedPdlClient.getPerson(any()) } answers
        {
            PdlPerson(
                    foedselsdato = LocalDate.parse("1990-01-01"),
                    identer =
                        listOf(
                            PdlIdent(
                                ident = "123456767",
                                gruppe = PdlIdentgruppe.FOLKEREGISTERIDENT,
                                historisk = false,
                            ),
                            PdlIdent(
                                ident = "12345678912345",
                                gruppe = PdlIdentgruppe.AKTORID,
                                historisk = false,
                            ),
                        ),
                )
                .right()
        }
    application.module()
}

fun createIntegrationEnvironment() =
    Environment(
        runtime = Runtime(env = RuntimeCluster.LOCAL, name = "test-app"),
        safEndpoint = "http://localhost:8818/",
        tsmPdlCacheEndpoint = "http://localhost:8819/",
    )
