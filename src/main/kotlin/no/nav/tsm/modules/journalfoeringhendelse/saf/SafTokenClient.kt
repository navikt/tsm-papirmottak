package no.nav.tsm.modules.journalfoeringhendelse.saf

import no.nav.tsm.core.Environment
import no.nav.tsm.ktor.auth.texas.Texas
import no.nav.tsm.ktor.auth.texas.TexasTarget
import no.nav.tsm.ktor.nais.RuntimeCluster

sealed interface SafTokenClient {
    suspend fun token(): String

    class Local : SafTokenClient {
        override suspend fun token(): String {
            return "token"
        }
    }

    class Remote(private val texas: Texas, private val environment: Environment) : SafTokenClient {

        override suspend fun token(): String {
            return texas
                .entraIdToken(
                    namespace = "teamdokumenthandtering",
                    app = "saf",
                    cluster = environment.runtime.env.toCorrespondingFSS(),
                )
                .token
        }

        private fun RuntimeCluster.toCorrespondingFSS(): TexasTarget {
            return when (this) {
                RuntimeCluster.DEV -> TexasTarget.DEV_FSS
                RuntimeCluster.PROD -> TexasTarget.PROD_FSS
                else ->
                    throw IllegalStateException(
                        "Unexpected 'app.runtime' configuration: ${this}. Should be one of 'dev-gcp' or 'prod-gcp'"
                    )
            }
        }
    }
}
