package no.nav.tsm.modules.journalfoeringhendelse.saf.models

data class FindJournalpostRequest(val query: String, val variables: Variables)

data class Variables(val id: String)

data class FindJournalpostResponse(val journalpost: Journalpost)

data class Journalpost(
    val avsenderMottaker: AvsenderMottaker?,
    val bruker: JournalpostBruker?,
    val datoOpprettet: String?,
    val dokumenter: List<Dokument>?,
    val journalposttype: String,
    val journalstatus: Journalstatus?,
    val kanal: String?,
    val kanalnavn: String?,
    val opprettetAvNavn: String?,
    val sak: Sak?,
    val skjerming: String?,
    val tema: String?,
    val temanavn: String?,
    val tittel: String?,
)

enum class Journalstatus {
    MOTTATT,
    JOURNALFOERT,
    FERDIGSTILT,
    EKSPEDERT,
    UNDER_ARBEID,
    FEILREGISTRERT,
    UTGAAR,
    AVBRUTT,
    UKJENT_BRUKER,
    RESERVERT,
    OPPLASTING_DOKUMENT,
    UKJENT,
}

data class Sak(val fagsakId: String?, val fagsaksystem: String?, val sakstype: String?)

data class Dokument(
    val tittel: String?,
    val dokumentInfoId: String,
    val brevkode: String?,
    val dokumentvarianter: List<Dokumentvarianter>,
)

data class Dokumentvarianter(
    val variantformat: DokumentVariantFormat,
    val filnavn: String?,
    val filtype: String,
    val filuuid: String,
)

enum class DokumentVariantFormat {
    ARKIV,
    FULLVERSJON,
    PRODUKSJON,
    PRODUKSJON_DLF,
    SLADDET,
    ORIGINAL,
}

data class AvsenderMottaker(val id: String?, val navn: String?)

data class JournalpostBruker(val id: String?, val type: BrukerIdType?)

enum class BrukerIdType {
    AKTOERID,
    FNR,
    ORGNR,
}
