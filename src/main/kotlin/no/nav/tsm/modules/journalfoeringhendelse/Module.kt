package no.nav.tsm.modules.journalfoeringhendelse

import io.confluent.kafka.serializers.AbstractKafkaSchemaSerDeConfig
import io.confluent.kafka.serializers.KafkaAvroDeserializer
import io.confluent.kafka.serializers.KafkaAvroDeserializerConfig
import io.ktor.server.application.*
import io.ktor.server.plugins.di.*
import kotlin.time.Duration.Companion.seconds
import no.nav.joarkjournalfoeringhendelser.JournalfoeringHendelseRecord
import no.nav.tsm.core.Environment
import no.nav.tsm.ktor.kafka.consumer.KafkaConsumer
import no.nav.tsm.ktor.kafka.consumer.KafkaTopic
import no.nav.tsm.ktor.kafka.consumer.RecordMeta
import org.apache.avro.util.ClassSecurityValidator
import tools.jackson.databind.ObjectMapper

fun Application.configureJournalfoeringHendelse() {
    configureConsumer()
}

private fun Application.configureConsumer() {

    val env: Environment by dependencies

    ClassSecurityValidator.setGlobal(
        ClassSecurityValidator.composite(
            ClassSecurityValidator.getGlobal(),
            ClassSecurityValidator.builder().add(JournalfoeringHendelseRecord::class.java).build(),
        )
    )

    val schemaRegistryConfig = environment.config.config("kafka.schemaRegistry")
    val deserializerConfig =
        buildMap<String, Any> {
            put(
                AbstractKafkaSchemaSerDeConfig.SCHEMA_REGISTRY_URL_CONFIG,
                schemaRegistryConfig.property("url").getString(),
            )
            put(KafkaAvroDeserializerConfig.SPECIFIC_AVRO_READER_CONFIG, true)

            val username = schemaRegistryConfig.propertyOrNull("username")?.getString()
            val password = schemaRegistryConfig.propertyOrNull("password")?.getString()
            require((username == null) == (password == null)) {
                "Schema registry username and password must be configured together"
            }
            if (username != null && password != null) {
                put("basic.auth.credentials.source", "USER_INFO")
                put("basic.auth.user.info", "$username:$password")
            }
        }
    val deserializer = KafkaAvroDeserializer().apply { configure(deserializerConfig, false) }

    install(KafkaConsumer) {
        clientId = env.runtime.name
        groupId = "tsm-papirmottak-consumer"
        pollDuration = 10.seconds
        retryDuration = 60.seconds

        topics +=
            object : KafkaTopic.Unbatched<JournalfoeringHendelseRecord> {
                override val topic = "teamdokumenthandtering.aapen-dok-journalfoering"
                override val shouldSkip: (suspend (RecordMeta, Exception) -> Boolean)? = null
                override val onTombstone: suspend (RecordMeta) -> Unit = { meta ->
                    log.info(
                        "Received a JournalfoeringHendelseRecord tombstone for ID ${meta.key}, skips"
                    )
                }

                override suspend fun handleRecord(
                    value: ByteArray?,
                    meta: RecordMeta,
                    objectMapper: ObjectMapper,
                ) {
                    if (value == null) {
                        onTombstone(meta)
                        return
                    }

                    val record = deserializer.deserialize(meta.topic, value)
                    check(record is JournalfoeringHendelseRecord) {
                        "Expected JournalfoeringHendelseRecord on ${meta.topic}, received ${record?.javaClass?.name}"
                    }
                    log.info("Received a JournalfoeringHendelseRecord stuff ${record.hendelsesId}")
                }
            }
    }

    monitor.subscribe(ApplicationStopped) { deserializer.close() }
}
