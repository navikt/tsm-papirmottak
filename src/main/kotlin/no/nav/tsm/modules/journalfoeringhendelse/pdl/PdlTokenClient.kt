package no.nav.tsm.modules.journalfoeringhendelse.pdl

import no.nav.tsm.ktor.auth.texas.Texas

sealed interface PdlTokenClient {
    suspend fun token(): String

    class Local : PdlTokenClient {
        override suspend fun token(): String {
            return "token"
        }
    }

    class Remote(private val texas: Texas) : PdlTokenClient {

        override suspend fun token(): String {
            return texas.entraIdToken(namespace = "tsm", app = "tsm-pdl-cache").token
        }
    }
}
