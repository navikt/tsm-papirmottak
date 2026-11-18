package no.nav.tsm.modules.journalfoeringhendelse.avro

import io.confluent.kafka.serializers.AbstractKafkaSchemaSerDeConfig
import io.confluent.kafka.serializers.KafkaAvroDeserializer
import io.confluent.kafka.serializers.KafkaAvroDeserializerConfig
import io.ktor.server.config.ApplicationConfig
import no.nav.joarkjournalfoeringhendelser.JournalfoeringHendelseRecord
import no.nav.tsm.ktor.kafka.consumer.KafkaTopic
import no.nav.tsm.ktor.kafka.consumer.RecordMeta
import no.nav.tsm.ktor.logger
import no.nav.tsm.modules.journalfoeringhendelse.saf.SafJournalpostClient
import no.nav.tsm.modules.journalfoeringhendelse.service.JournalfoeringHendelseConsumerService
import org.apache.avro.util.ClassSecurityValidator
import tools.jackson.databind.ObjectMapper

class JournalfoeringHendelseAvroTopic(
    schemaRegistryConfig: ApplicationConfig,
    safJournalpostClient: SafJournalpostClient,
) : KafkaTopic.Unbatched<JournalfoeringHendelseRecord>, AutoCloseable {

    private val logger = logger()
    private val journalfoeringHendelseConsumerService =
        JournalfoeringHendelseConsumerService(safjournalpostClient = safJournalpostClient)

    init {
        ClassSecurityValidator.setGlobal(
            ClassSecurityValidator.composite(
                ClassSecurityValidator.getGlobal(),
                ClassSecurityValidator.builder()
                    .add(JournalfoeringHendelseRecord::class.java)
                    .build(),
            )
        )
    }

    private val deserializerConfig =
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

    private val deserializer =
        KafkaAvroDeserializer().apply { configure(deserializerConfig, false) }

    override val topic = "teamdokumenthandtering.aapen-dok-journalfoering"
    override val shouldSkip: (suspend (RecordMeta, Exception) -> Boolean)? = null
    override val onTombstone: suspend (RecordMeta) -> Unit = { meta ->
        logger.info("Received a JournalfoeringHendelseRecord tombstone for ID ${meta.key}, skips")
    }

    override fun close() {
        deserializer.close()
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
        journalfoeringHendelseConsumerService.handleRecord(record)
    }
}
