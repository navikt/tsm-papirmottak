package no.nav.tsm.modules.journalfoeringhendelse.service

import arrow.core.getOrElse
import no.nav.joarkjournalfoeringhendelser.JournalfoeringHendelseRecord
import no.nav.tsm.ktor.logger
import no.nav.tsm.ktor.teamLogger
import no.nav.tsm.modules.journalfoeringhendelse.pdl.PdlClient
import no.nav.tsm.modules.journalfoeringhendelse.pdl.models.PdlIdentgruppe
import no.nav.tsm.modules.journalfoeringhendelse.saf.SafJournalpostClient
import no.nav.tsm.modules.journalfoeringhendelse.saf.models.JournalpostMetadata

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
                (mottaksKanal == "SKAN_NETS" ||
                    mottaksKanal == "SKAN_IM" ||
                    mottaksKanal == "NAV_NO" ||
                    mottaksKanal == "EESSI") &&
                (hendelsesType == "MidlertidigJournalført" ||
                    hendelsesType == "JournalpostMottatt" ||
                    hendelsesType == "TemaEndret")
        ) {

            logger.info(
                "Received a JournalfoeringHendelseRecord ${record.hendelsesId} and" +
                    " journalpostId: $journalpostId fra mottaksKanal: $mottaksKanal og hendelsesType: $hendelsesType "
            )

            val journalpostMetadata =
                safjournalpostClient.getJournalpostMetadata(journalpostId)
                    ?: throw IllegalStateException(
                        "Unable to find journalpost with id $journalpostId"
                    )

            logger.info(
                "Journalpostid: $journalpostId er gjelderUtland: ${journalpostMetadata.gjelderUtland}"
            )

            if (journalpostMetadata.jpErIkkeJournalfort) {
                val pasient =
                    journalpostMetadata.bruker.let {
                        if (it.id.isNullOrEmpty() || it.type.isNullOrEmpty()) {
                            logger.info(
                                "Mottatt papirsykmelding der bruker mangler journalpost: $journalpostId"
                            )
                            null
                        } else {
                            hentBrukerIdFraJournalpost(journalpostMetadata)?.let { ident ->
                                pdlClient.getPerson(ident).getOrElse {
                                    null
                                }
                            }
                        }
                    }

                teamlogger.info(
                    "Got PDL person ident ${pasient?.identer?.first { !it.historisk && it.gruppe == PdlIdentgruppe.FOLKEREGISTERIDENT }?.ident} for journalpostId: $journalpostId"
                )
                // Handel utenlandsk sykmelding this way
                // Handel norsk sykmelding this way, fetch behandelr with tsm-behandler and some ocr service

            } else {
                logger.info(
                    "Received a JournalfoeringHendelseRecord has already been handeled hendelsesId: ${record.hendelsesId} and journalpostId: ${record.journalpostId}"
                )
            }
        } else {
            logger.debug(
                "Received a JournalfoeringHendelseRecord that we do not want ${record.hendelsesId} and journalpostId: $journalpostId"
            )
        }
    }

    fun hentBrukerIdFraJournalpost(journalpost: JournalpostMetadata): String? {
        val bruker = journalpost.bruker
        val brukerId =
            bruker.id
                ?: throw IllegalStateException(
                    "Journalpost missing brukerId; this should not be able to happen"
                )
        return if (bruker.type == "AKTOERID" || bruker.type == "FNR") {
            brukerId
        } else {
            null
        }
    }
}
