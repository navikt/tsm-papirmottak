package no.nav.tsm

import io.confluent.kafka.schemaregistry.testutil.MockSchemaRegistry
import io.confluent.kafka.serializers.AbstractKafkaSchemaSerDeConfig
import io.confluent.kafka.serializers.KafkaAvroSerializer
import io.kotest.assertions.nondeterministic.eventually
import io.kotest.matchers.shouldBe
import io.ktor.server.config.MapApplicationConfig
import io.ktor.server.config.mergeWith
import io.ktor.server.testing.ApplicationTestBuilder
import io.ktor.server.testing.testApplication
import java.util.UUID
import kotlin.test.AfterTest
import kotlin.test.Test
import kotlin.time.Duration.Companion.seconds
import no.nav.joarkjournalfoeringhendelser.JournalfoeringHendelseRecord
import no.nav.tsm.ktor.kafka.test.KafkaContainer
import no.nav.tsm.utils.configureFullIntegrationTests
import org.apache.kafka.clients.admin.AdminClient
import org.apache.kafka.clients.producer.KafkaProducer
import org.apache.kafka.clients.producer.ProducerConfig
import org.apache.kafka.clients.producer.ProducerRecord
import org.apache.kafka.common.TopicPartition
import org.apache.kafka.common.serialization.StringSerializer

const val JOURNALFOERING_HENDELSE_TOPIC = "teamdokumenthandtering.aapen-dok-journalfoering"

class IntegrationTest {
    val kafka = KafkaContainer(createTopics = listOf(JOURNALFOERING_HENDELSE_TOPIC))

    private val schemaRegistryScope = UUID.randomUUID().toString()
    private val schemaRegistryUrl = "mock://$schemaRegistryScope"
    private val producer =
        KafkaProducer<String, JournalfoeringHendelseRecord>(
            kafka.config +
                mapOf(
                    ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG to StringSerializer::class.java,
                    ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG to KafkaAvroSerializer::class.java,
                    AbstractKafkaSchemaSerDeConfig.SCHEMA_REGISTRY_URL_CONFIG to schemaRegistryUrl,
                )
        )

    @AfterTest
    fun cleanup() {
        try {
            producer.close()
        } finally {
            try {
                kafka.container.stop()
            } finally {
                MockSchemaRegistry.dropScope(schemaRegistryScope)
            }
        }
    }

    private fun ApplicationTestBuilder.configureEverythingTest() {
        kafka.configureKafka(this)
        environment {
            config =
                config.mergeWith(
                    MapApplicationConfig("kafka.schemaRegistry.url" to schemaRegistryUrl)
                )
        }

        configureFullIntegrationTests()
    }

    @Test
    fun `Should consume Avro records and tombstones from kafka topic`() = testApplication {
        configureEverythingTest()

        val key = UUID.randomUUID().toString()

        val record =
            JournalfoeringHendelseRecord.newBuilder()
                .setHendelsesId(key)
                .setVersjon(1)
                .setHendelsesType("JournalpostMottatt")
                .setJournalpostId(123456789L)
                .setJournalpostStatus("MOTTATT")
                .setTemaGammelt("")
                .setTemaNytt("SYM")
                .setMottaksKanal("SKAN_IM")
                .setKanalReferanseId(UUID.randomUUID().toString())
                .setBehandlingstema("")
                .build()

        startApplication()

        AdminClient.create(kafka.config).use { admin ->
            val partition = TopicPartition(JOURNALFOERING_HENDELSE_TOPIC, 0)
            producer.send(ProducerRecord(JOURNALFOERING_HENDELSE_TOPIC, key, record)).get()

            eventually(30.seconds) {
                admin
                    .listConsumerGroupOffsets("tsm-papirmottak-consumer")
                    .partitionsToOffsetAndMetadata()
                    .get()[partition]
                    ?.offset() shouldBe 1L
            }

            producer.send(ProducerRecord(JOURNALFOERING_HENDELSE_TOPIC, key, null)).get()

            eventually(30.seconds) {
                admin
                    .listConsumerGroupOffsets("tsm-papirmottak-consumer")
                    .partitionsToOffsetAndMetadata()
                    .get()[partition]
                    ?.offset() shouldBe 2L
            }
        }
    }
}
