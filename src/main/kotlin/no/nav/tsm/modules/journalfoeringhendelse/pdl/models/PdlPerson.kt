package no.nav.tsm.modules.journalfoeringhendelse.pdl.models

import java.time.LocalDate

data class PdlPerson(val foedselsdato: LocalDate?, val identer: List<PdlIdent>)

data class PdlIdent(val ident: String, val gruppe: PdlIdentgruppe, val historisk: Boolean)

enum class PdlIdentgruppe {
    AKTORID,
    FOLKEREGISTERIDENT,
    NPID,
}

enum class PdlErrors {
    NotFound,
    UnknownError,
}
