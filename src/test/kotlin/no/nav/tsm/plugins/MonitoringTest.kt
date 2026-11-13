package no.nav.tsm.plugins

import io.kotest.matchers.equals.shouldEqual
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.request.get
import io.ktor.serialization.jackson3.jackson
import io.ktor.server.testing.ApplicationTestBuilder
import io.ktor.server.testing.testApplication
import kotlin.test.Test

class MonitoringTest {

    private fun ApplicationTestBuilder.configureMonitoringTest() {

        client = createClient { install(ContentNegotiation) { jackson {} } }

        application { configureMonitoring() }
    }

    @Test
    fun `validate alive endpoint returns 200 http status code`() = testApplication {
        configureMonitoringTest()

        val httpResponse = client.get("/internal/health/alive")

        httpResponse.status.value shouldEqual 200
    }

    @Test
    fun `validate ready endpoint returns 200 http status code`() = testApplication {
        configureMonitoringTest()

        val httpResponse = client.get("/internal/health/ready")

        httpResponse.status.value shouldEqual 200
    }
}
