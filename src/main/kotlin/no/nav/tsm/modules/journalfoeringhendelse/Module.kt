package no.nav.tsm.modules.journalfoeringhendelse

import io.ktor.server.application.*
import io.ktor.server.plugins.di.*
import kotlin.time.Duration.Companion.seconds
import no.nav.tsm.core.Environment
import no.nav.tsm.ktor.di.dynamicDependencies
import no.nav.tsm.ktor.kafka.consumer.KafkaConsumer
import no.nav.tsm.modules.journalfoeringhendelse.avro.JournalfoeringHendelseAvroTopic
import no.nav.tsm.modules.journalfoeringhendelse.pdl.PdlClient
import no.nav.tsm.modules.journalfoeringhendelse.pdl.PdlTokenClient
import no.nav.tsm.modules.journalfoeringhendelse.saf.SafJournalpostClient
import no.nav.tsm.modules.journalfoeringhendelse.saf.SafTokenClient

fun Application.configureJournalfoeringHendelse() {

    dynamicDependencies {
        local {
            provide(SafTokenClient.Local::class)
            provide(PdlTokenClient.Local::class)
        }
        cloud {
            provide(SafTokenClient.Remote::class)
            provide(PdlTokenClient.Remote::class)
        }
    }

    dependencies { provide(SafJournalpostClient::class) }
    dependencies { provide(PdlClient::class) }

    val safJournalpostClient: SafJournalpostClient by dependencies
    val pdlClient: PdlClient by dependencies

    configureConsumer(safJournalpostClient, pdlClient)
}

private fun Application.configureConsumer(
    safJournalpostClient: SafJournalpostClient,
    pdlClient: PdlClient,
) {

    val env: Environment by dependencies

    val topic =
        JournalfoeringHendelseAvroTopic(
            environment.config.config("kafka.schemaRegistry"),
            safJournalpostClient,
            pdlClient,
        )

    install(KafkaConsumer) {
        clientId = env.runtime.name
        groupId = "tsm-papirmottak-consumer"
        pollDuration = 10.seconds
        retryDuration = 60.seconds

        topics += topic
    }

    monitor.subscribe(ApplicationStopped) { topic.close() }
}
