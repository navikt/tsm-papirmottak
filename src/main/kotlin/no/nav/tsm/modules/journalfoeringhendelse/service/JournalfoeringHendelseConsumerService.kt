package no.nav.tsm.modules.journalfoeringhendelse.service

import no.nav.joarkjournalfoeringhendelser.JournalfoeringHendelseRecord
import no.nav.tsm.ktor.logger
import no.nav.tsm.modules.journalfoeringhendelse.saf.SafJournalpostClient

class JournalfoeringHendelseConsumerService(
    private val safjournalpostClient: SafJournalpostClient
) {
    private val logger = logger()

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
            "Received a JournalfoeringHendelseRecord ${record.hendelsesId} and journalpostId: ${record.journalpostId}"
        )

        val journalpostMetadata =
            safjournalpostClient.getJournalpostMetadata(record.journalpostId.toString())

        logger.info(
            "Journalpostid: ${record.journalpostId} er gjelderUtland: ${journalpostMetadata?.gjelderUtland}"
        )
        }
       else {
           logger.info(
            "Received a JournalfoeringHendelseRecord that we do not want ${record.hendelsesId} and journalpostId: ${record.journalpostId}"
        )
       }
    }
}
