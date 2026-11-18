package no.nav.tsm.modules.journalfoeringhendelse.service

import no.nav.joarkjournalfoeringhendelser.JournalfoeringHendelseRecord
import no.nav.tsm.ktor.logger
import no.nav.tsm.modules.journalfoeringhendelse.saf.SafJournalpostClient

class JournalfoeringHendelseConsumerService(
    private val safjournalpostClient: SafJournalpostClient
) {
    private val logger = logger()

    suspend fun handleRecord(record: JournalfoeringHendelseRecord) {
        logger.info(
            "Received a JournalfoeringHendelseRecord ${record.hendelsesId} and journalpostId: ${record.journalpostId}"
        )

        val journalpostMetadata =
            safjournalpostClient.getJournalpostMetadata(record.journalpostId.toString())

        logger.info(
            "Journalpostid: ${record.journalpostId} er gjelderUtland: ${journalpostMetadata?.gjelderUtland}"
        )
    }
}
