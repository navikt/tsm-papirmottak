package no.nav.tsm.core

import com.typesafe.config.ConfigFactory
import io.kotest.matchers.equals.shouldBeEqual
import io.ktor.server.config.HoconApplicationConfig
import kotlin.collections.plus
import kotlin.test.Test

class EnvironmentTest {

    private fun config(overrides: Map<String, String>) =
        HoconApplicationConfig(
            ConfigFactory.parseMap(baseNaisVars + overrides)
                .withFallback(ConfigFactory.parseResources("application.conf"))
                .resolve()
        )

    private val baseNaisVars =
        mapOf("NAIS_POD_NAME" to "tsm-papirmottak-prod-123", "NAIS_CLUSTER_NAME" to "prod-gcp")

    @Test
    fun `base nais environment should be properly configured`() {

        val environment = initializeEnvironment(config(emptyMap()))

        environment.runtime.name shouldBeEqual "tsm-papirmottak-prod-123"
    }
}
