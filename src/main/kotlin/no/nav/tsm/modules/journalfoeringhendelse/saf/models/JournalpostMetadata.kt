package no.nav.tsm.modules.journalfoeringhendelse.saf.models

import java.time.LocalDateTime

data class JournalpostMetadata(
    val bruker: Bruker,
    val dokumenter: List<DokumentMedTittel>,
    val jpErIkkeJournalfort: Boolean,
    val gjelderUtland: Boolean,
    val datoOpprettet: LocalDateTime?,
    val dokumentInfoIdPdf: String,
    val alleDokumenter: List<DokumentInfo>,
)

data class Bruker(val id: String?, val type: String?)

data class DokumentInfo(
    val journalpostId: String,
    val dokumentInfoId: String,
    val filtype: String,
    val filUUID: String,
    val variantFormat: DokumentVariantFormat,
)

data class DokumentMedTittel(
    val tittel: String,
    val dokumentInfoId: String,
    val variantformat: DokumentVariantFormat,
)
