package no.nav.tsm.modules.journalfoeringhendelse.service

import arrow.core.getOrElse
import no.nav.joarkjournalfoeringhendelser.JournalfoeringHendelseRecord
import no.nav.tsm.ktor.logger
import no.nav.tsm.ktor.teamLogger
import no.nav.tsm.modules.journalfoeringhendelse.pdl.PdlClient
import no.nav.tsm.modules.journalfoeringhendelse.pdl.models.PdlIdentgruppe
import no.nav.tsm.modules.journalfoeringhendelse.saf.SafJournalpostClient

class JournalfoeringHendelseConsumerService(
    private val safjournalpostClient: SafJournalpostClient,
    private val pdlClient: PdlClient,
) {
    private val logger = logger()
    private val teamlogger = teamLogger()

    suspend fun handleRecord(record: JournalfoeringHendelseRecord) {

        val journalpostId = record.journalpostId.toString()
        val temaNytt = record.temaNytt.toString()
        val mottaksKanal = record.mottaksKanal.toString()
        val hendelsesType = record.hendelsesType.toString()

        if (
            temaNytt == "SYM" &&
                (mottaksKanal == "SKAN_NETS" || mottaksKanal == "SKAN_IM") &&
                (hendelsesType == "MidlertidigJournalført" ||
                    hendelsesType == "JournalpostMottatt" ||
                    hendelsesType == "TemaEndret")
        ) {

            logger.info(
                "Received a JournalfoeringHendelseRecord ${record.hendelsesId} and journalpostId: $journalpostId"
            )

            val journalpostMetadata = safjournalpostClient.getJournalpostMetadata(journalpostId)

            logger.info(
                "Journalpostid: $journalpostId er gjelderUtland: ${journalpostMetadata?.gjelderUtland}"
            )

            val brukerIdentId =
                when (journalpostMetadata?.bruker?.type) {
                    "ORGNR" -> throw IllegalStateException("Bruker id is ORGNR")
                    else -> journalpostMetadata?.bruker?.id
                } ?: throw IllegalStateException("Bruker id is null")

            val person =
                pdlClient.getPerson(brukerIdentId).getOrElse {
                    throw RuntimeException("Did not find person for journalpostId: $journalpostId ")
                }

            teamlogger.info(
                "Got PDL person ident ${person.identer.first { !it.historisk && it.gruppe == PdlIdentgruppe.FOLKEREGISTERIDENT }.ident} for journalpostId: $journalpostId"
            )
        } else {
            logger.debug(
                "Received a JournalfoeringHendelseRecord that we do not want ${record.hendelsesId} and journalpostId: ${record.journalpostId}"
            )
        }
    }
}
