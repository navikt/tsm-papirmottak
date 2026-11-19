package no.nav.tsm.modules.journalfoeringhendelse.pdl

import arrow.core.Either
import arrow.core.left
import arrow.core.right
import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.plugins.HttpRequestRetry
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.client.request.*
import io.ktor.http.*
import io.ktor.serialization.jackson3.jackson
import io.opentelemetry.instrumentation.annotations.WithSpan
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import no.nav.tsm.core.Environment
import no.nav.tsm.ktor.logger
import no.nav.tsm.modules.journalfoeringhendelse.pdl.models.PdlErrors
import no.nav.tsm.modules.journalfoeringhendelse.pdl.models.PdlPerson

class PdlClient(
    httpClient: HttpClient,
    private val pdlTokenClient: PdlTokenClient,
    environment: Environment,
) {

    private val tsmPdlConfig = environment.tsmPdlCacheEndpoint
    private val logger = logger()

    private val httpClient: HttpClient = httpClient.config {
        install(ContentNegotiation) { jackson {} }
        install(HttpRequestRetry) {
            retryOnServerErrors(maxRetries = 3)
            exponentialDelay()
        }
    }

    @WithSpan
    suspend fun getPerson(ident: String): Either<PdlErrors, PdlPerson> {
        val token = pdlTokenClient.token()

        val response =
            httpClient.get("${tsmPdlConfig}/api/person") {
                headers {
                    append("Nav-Consumer-Id", "tsm-papirmottak")
                    append("Authorization", "Bearer $token")
                    append("Ident", ident)
                }
            }

        return when {
            response.status.isSuccess() ->
                try {
                    response.body<PdlPerson>().right()
                } catch (exception: Exception) {
                    if (exception is CancellationException) {
                        currentCoroutineContext().ensureActive()
                    }
                    logger.error("Error deserializing PDL response", exception)
                    return PdlErrors.UnknownError.left()
                }

            response.status == HttpStatusCode.NotFound -> PdlErrors.NotFound.left()
            else -> {
                logger.error("Unable to get person from pdl, see team logs for ident")
                PdlErrors.UnknownError.left()
            }
        }
    }
}
