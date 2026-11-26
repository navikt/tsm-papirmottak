package no.nav.tsm.core.model

import com.fasterxml.jackson.annotation.JsonProperty
import no.nav.tsm.modules.journalfoeringhendelse.saf.models.Dokument
import no.nav.tsm.sykmelding.input.core.model.Sykmelding

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
    val dokumenter: List<Dokument>

    data class NorskPapirSykmeldingOcrOK(
        override val sykmeldingId: String,
        override val journalpostId: String,
        override val dokumenter: List<Dokument>,
        val sykmelding: Sykmelding,
    ) : Journalpost {
        override val type = TsmJournalpostType.NORSK_PAPIR_SYKMELDING_OCR_OK
    }

    data class NorskPapirSykmeldingOcrDelvis(
        override val sykmeldingId: String,
        override val journalpostId: String,
        override val dokumenter: List<Dokument>,
    ) : Journalpost {
        override val type = TsmJournalpostType.NORSK_PAPIR_SYKMELDING_OCR_DELVIS
    }

    data class NorskPapirSykmeldingOcrIngen(
        override val sykmeldingId: String,
        override val journalpostId: String,
        override val dokumenter: List<Dokument>,
    ) : Journalpost {
        override val type = TsmJournalpostType.NORSK_PAPIR_SYKMELDING_OCR_INGEN
    }

    data class UtenlandskPapirSykmelding(
        override val sykmeldingId: String,
        override val journalpostId: String,
        override val dokumenter: List<Dokument>,
    ) : Journalpost {
        override val type = TsmJournalpostType.UTENLANDSK_PAPIR_SYKMELDING
    }

    data class Ukjent(
        override val sykmeldingId: String,
        override val journalpostId: String,
        override val dokumenter: List<Dokument>
    ) : Journalpost {
        override val type = TsmJournalpostType.UKJENT
    }
}
