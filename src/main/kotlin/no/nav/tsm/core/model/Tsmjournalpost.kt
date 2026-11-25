package no.nav.tsm.core.model

import com.fasterxml.jackson.annotation.JsonProperty

enum class TsmJournalpostType {
    NORSK_PAPIR_SYKMELDING_OCR_OK,
    NORSK_PAPIR_SYKMELDING_OCR_DELVIS,
    NORSK_PAPIR_SYKMELDING_OCR_INGEN,
    UTENLANDSK_PAPIR_SYKMELDING,
    UKJENT,
}

sealed interface Journalpost {
    @get:JsonProperty(access = JsonProperty.Access.READ_ONLY) val type: TsmJournalpostType
    val journalpostId: String
    val sykmeldingId: String

    data class NorskPapirSykmeldingOcrOK(
        override val sykmeldingId: String,
        override val journalpostId: String,
        val ocrFile: String,
        val newSykmeldingFormat: String,
    ) : Journalpost {
        override val type = TsmJournalpostType.NORSK_PAPIR_SYKMELDING_OCR_OK
    }

    data class NorskPapirSykmeldingOcrDelvis(
        override val sykmeldingId: String,
        override val journalpostId: String,
        val ocrFile: String,
    ) : Journalpost {
        override val type = TsmJournalpostType.NORSK_PAPIR_SYKMELDING_OCR_DELVIS
    }

    data class NorskPapirSykmeldingOcrIngen(
        override val sykmeldingId: String,
        override val journalpostId: String,
    ) : Journalpost {
        override val type = TsmJournalpostType.NORSK_PAPIR_SYKMELDING_OCR_INGEN
    }

    data class UtenlandskPapirSykmelding(
        override val sykmeldingId: String,
        override val journalpostId: String,
    ) : Journalpost {
        override val type = TsmJournalpostType.UTENLANDSK_PAPIR_SYKMELDING
    }

    data class Ukjent(override val sykmeldingId: String, override val journalpostId: String) :
        Journalpost {
        override val type = TsmJournalpostType.UKJENT
    }
}
