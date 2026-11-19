package no.nav.tsm.modules.journalfoeringhendelse.saf

import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.plugins.HttpRequestRetry
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.request.headers
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.http.HttpHeaders
import io.ktor.serialization.jackson3.jackson
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.ZoneOffset
import no.nav.tsm.core.Environment
import no.nav.tsm.ktor.logger
import no.nav.tsm.modules.journalfoeringhendelse.saf.models.Bruker
import no.nav.tsm.modules.journalfoeringhendelse.saf.models.Dokument
import no.nav.tsm.modules.journalfoeringhendelse.saf.models.DokumentInfo
import no.nav.tsm.modules.journalfoeringhendelse.saf.models.DokumentMedTittel
import no.nav.tsm.modules.journalfoeringhendelse.saf.models.DokumentVariantFormat
import no.nav.tsm.modules.journalfoeringhendelse.saf.models.FindJournalpostRequest
import no.nav.tsm.modules.journalfoeringhendelse.saf.models.FindJournalpostResponse
import no.nav.tsm.modules.journalfoeringhendelse.saf.models.GraphQLResponse
import no.nav.tsm.modules.journalfoeringhendelse.saf.models.JournalpostMetadata
import no.nav.tsm.modules.journalfoeringhendelse.saf.models.Journalstatus
import no.nav.tsm.modules.journalfoeringhendelse.saf.models.Variables

val log = logger()

class SafJournalpostClient(
    private val safTokenClient: SafTokenClient,
    httpClient: HttpClient,
    environment: Environment,
    private val safEndpoint: String = environment.safEndpoint,
) {

    private val httpClient: HttpClient = httpClient.config {
        install(ContentNegotiation) { jackson {} }
        install(HttpRequestRetry) {
            retryOnServerErrors(maxRetries = 3)
            exponentialDelay()
        }
    }

    val findJournalpostGraphQlQuery =
        SafJournalpostClient::class
            .java
            .getResource("/graphql/findJournalpost.graphql")!!
            .readText()
            .replace(Regex("[\n\t]"), "")

    suspend fun getJournalpostMetadata(journalpostId: String): JournalpostMetadata? {
        val token = safTokenClient.token()

        val findJournalpostRequest =
            FindJournalpostRequest(
                query = findJournalpostGraphQlQuery,
                variables = Variables(id = journalpostId),
            )

        val findJournalpostResponse =
            httpClient
                .post("$safEndpoint/graphql") {
                    setBody(findJournalpostRequest)
                    headers {
                        append(HttpHeaders.Authorization, "Bearer $token")
                        append("X-Correlation-ID", journalpostId)
                        append(HttpHeaders.ContentType, "application/json")
                    }
                }
                .body<GraphQLResponse<FindJournalpostResponse>?>()

        if (findJournalpostResponse == null) {
            log.error("Call to Saf failed for journalpostId: $journalpostId")
            return null
        }
        if (findJournalpostResponse.errors != null) {
            findJournalpostResponse.errors.forEach { log.error("Saf threw an error: {} ", it) }
            return null
        }

        if (findJournalpostResponse.data.journalpost.journalstatus == null) {
            log.error("Failed to retrieve data from Saf journalpostId: $journalpostId")
            return null
        }

        val journalpost = findJournalpostResponse.data.journalpost

        val alleDokumenter =
            findJournalpostResponse.data.journalpost.dokumenter?.flatMap { dokument ->
                dokument.dokumentvarianter.map { variant ->
                    DokumentInfo(
                        journalpostId = journalpostId,
                        dokumentInfoId = dokument.dokumentInfoId,
                        variantFormat = variant.variantformat,
                        filtype = variant.filtype,
                        filUUID = variant.filuuid,
                    )
                }
            } ?: emptyList()

        val dokumentId: String? = finnDokumentIdForOcr(journalpost.dokumenter)
        return journalpost.let {
            val dokumenter = finnDokumentIdForPdf(journalpost.dokumenter)

            JournalpostMetadata(
                bruker = Bruker(it.bruker?.id, it.bruker?.type?.name),
                jpErIkkeJournalfort = erIkkeJournalfort(it.journalstatus),
                gjelderUtland = sykmeldingGjelderUtland(it.dokumenter, dokumentId),
                datoOpprettet = dateTimeStringTilLocalDateTime(it.datoOpprettet),
                dokumentInfoIdPdf = dokumenter.first().dokumentInfoId,
                dokumenter = dokumenter,
                alleDokumenter = alleDokumenter,
            )
        }
    }

    private fun erIkkeJournalfort(journalstatus: Journalstatus?): Boolean {
        return journalstatus?.name?.let {
            it.equals("MOTTATT", true) || it.equals("FEILREGISTRERT", true)
        } ?: false
    }
}

fun dateTimeStringTilLocalDateTime(dateTime: String?): LocalDateTime? {
    dateTime?.let {
        return try {
            LocalDateTime.parse(dateTime)
                .atZone(ZoneId.of("Europe/Oslo"))
                .withZoneSameInstant(ZoneOffset.UTC)
                .toLocalDateTime()
        } catch (e: Exception) {
            log.error("Journalpost entry does not have a valid datoOpprettet", e)
            null
        }
    }
    log.error("Journalpost missing datoOpprettet")
    return null
}

fun finnDokumentIdForOcr(dokumentListe: List<Dokument>?): String? {
    dokumentListe?.forEach { dokument ->
        dokument.dokumentvarianter.forEach {
            if (it.variantformat.name == "ORIGINAL") {
                log.info("Found OCR-document dokumentInfoId: ${dokument.dokumentInfoId}")
                return dokument.dokumentInfoId
            }
        }
    }
    log.warn("Not found OCR-document")
    return null
}

fun finnDokumentIdForPdf(dokumentListe: List<Dokument>?): List<DokumentMedTittel> {
    val dokumenter =
        dokumentListe
            ?.filter {
                it.dokumentvarianter.any { variant -> variant.variantformat.name == "ARKIV" }
            }
            ?.map { dokument ->
                DokumentMedTittel(
                    tittel = dokument.tittel ?: "Dokument uten tittel",
                    dokumentInfoId = dokument.dokumentInfoId,
                    variantformat = DokumentVariantFormat.ARKIV,
                )
            }

    if (dokumenter.isNullOrEmpty()) {
        log.error("Could not find PDF document")
        throw RuntimeException("Received a sykmelding without a PDF")
    }

    return dokumenter
}

const val GAMMEL_BREVKODE_UTLAND: String = "900023"
const val BREVKODE_UTLAND: String = "NAV 08-07.04 U"
const val BREVKODE_NORSK: String = "NAV 08-07.04"

fun sykmeldingGjelderUtland(dokumentListe: List<Dokument>?, dokumentId: String?): Boolean {
    if (dokumentListe.isNullOrEmpty()) {
        log.warn(
            "Missing information regarding document code; assuming a utenlandsk sykmelding."
        )
        return true
    }

    var brevkode: String? = null

    if (dokumentId != null) {
        val dokumenterMedRiktigId = dokumentListe.filter { it.dokumentInfoId == dokumentId }
        if (dokumenterMedRiktigId.isNotEmpty()) {
            brevkode = dokumenterMedRiktigId[0].brevkode
            if (brevkode != BREVKODE_NORSK) {
                log.warn("Found OCR file with unexpected letter code: $brevkode")
            }
        }
    } else {
        log.info(
            "Missing document ID for OCR; attempting to determine letter code from remaining documents."
        )
        val inneholderUtlandBrevkode: Boolean = dokumentListe.any { dok ->
            dok.brevkode == BREVKODE_UTLAND
        }
        if (inneholderUtlandBrevkode) {
            brevkode = BREVKODE_UTLAND
        } else {
            val inneholderGammelUtlandBrevkode: Boolean = dokumentListe.any { dok ->
                dok.brevkode == GAMMEL_BREVKODE_UTLAND
            }
            if (inneholderGammelUtlandBrevkode) {
                brevkode = GAMMEL_BREVKODE_UTLAND
            }
        }
    }
    return if (brevkode == BREVKODE_UTLAND || brevkode == GAMMEL_BREVKODE_UTLAND) {
        log.info("Sykmelding refers to a utenlandsk sykmelding, brevkode: {}", brevkode)
        true
    } else {
        log.info("Sykmelding refers to a innenlands-sykmelding, brevkode: {}", brevkode)
        false
    }
}
