package no.nav.tsm.modules.journalfoeringhendelse

import io.ktor.server.application.*
import io.ktor.server.plugins.di.*
import kotlin.time.Duration.Companion.seconds
import no.nav.tsm.core.Environment
import no.nav.tsm.ktor.kafka.consumer.KafkaConsumer

fun Application.configureJournalfoeringHendelse() {
    configureConsumer()
}

private fun Application.configureConsumer() {

    val env: Environment by dependencies

    val topic =
        JournalfoeringHendelseAvroTopic(environment.config.config("kafka.schemaRegistry"), log)

    install(KafkaConsumer) {
        clientId = env.runtime.name
        groupId = "tsm-papirmottak-consumer"
        pollDuration = 10.seconds
        retryDuration = 60.seconds

        topics += topic
    }

    monitor.subscribe(ApplicationStopped) { topic.close() }
}
