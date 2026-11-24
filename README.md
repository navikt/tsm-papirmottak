# tsm-papirmottak
Application for handling papirsykmeldinger


## FlowChart
This the high level flow of the application
```mermaid
  graph TD
    papirsykmelding --> Skanner
    Skanner --> id1(Scanned documents file area)
    id1(Scanned documents file arear) --> id2([Skanmot Helse])
    id2([Skanmot Helse]) --> id3([joark])
    id3([joark]) --> A[\teamdokumenthandtering.aapen-dok-journalfoering/]
    A[\teamdokumenthandtering.aapen-dok-journalfoering/] --> |read topic| id4[tsm-papirmottak]
    id4[tsm-papirmottak] <--> |Fetch journalpostMetdata| id5[saf]
    id4[tsm-papirmottak] <--> |Fetch behandler| id6[tsm-behandler]
    id4[tsm-papirmottak] <--> |Fetch pasient| id7[tsm-pdl-cache]
    id4[tsm-papirmottak] <--> |Create oppgave| id8[tsm-oppgave]
    id4[tsm-papirmottak] <--> |Get ICPC-2B codes| id9[hdir-icpc-2]
    id4[tsm-papirmottak] <--> |Get Tss for infotrygdId| id10[tss]
    id4[tsm-papirmottak] <--> |Update and finish journalpost| id11[dokakriv]
    id4[tsm-papirmottak] <--> |Write topic| C[\teamsykmelding.ok-sykmelding
/]
```

## Requirements for developing the application
#### Java
Make sure you have the Java JDK 25 installed
You can check which version you have installed using this command:
``` bash
java -version
```
or install it: https://sdkman.io/

#### Docker
Make sure you have docker installed using this command:
You can check which version you have installed using this command:
``` bash
docker --version
```
or install it: https://docs.docker.com/engine/install/

#### Docker Compose
Make sure you have docker compose installed using this command:
You can check which version you have installed using this command:
``` bash
docker compose version
```
or install it: https://docs.docker.com/compose/install/


### Building the application
To build the application locally and run tests you can simply run
``` bash
./gradlew clean build
 ```

### Running the application locally
Run in development to enable hot reloading and stubbed external dependencies:

#### With Gradle and Docker Compose

``` bash
./gradlew runLocal
```

#### With IntelliJ

In IntelliJ go to the run the main function,
then "Modify Run configuration..."
then add `-Dio.ktor.development=true -config=application-local.conf` to the "Program arguments".

#### Kafka message
```bash
docker compose exec -T schema-registry kafka-avro-console-producer \
  --bootstrap-server kafka:29092 \
  --topic teamdokumenthandtering.aapen-dok-journalfoering \
  --property schema.registry.url=http://localhost:8081 \
  --property parse.key=true \
  --property key.separator=: \
  --property key.serializer=org.apache.kafka.common.serialization.StringSerializer \
  --property 'value.schema={
    "type": "record",
    "name": "JournalfoeringHendelseRecord",
    "namespace": "no.nav.joarkjournalfoeringhendelser",
    "fields": [
      {"name": "hendelsesId", "type": "string"},
      {"name": "versjon", "type": "int"},
      {"name": "hendelsesType", "type": "string"},
      {"name": "journalpostId", "type": "long"},
      {"name": "journalpostStatus", "type": "string"},
      {"name": "temaGammelt", "type": "string"},
      {"name": "temaNytt", "type": "string"},
      {"name": "mottaksKanal", "type": "string"},
      {"name": "kanalReferanseId", "type": "string"},
      {"name": "behandlingstema", "type": "string", "default": ""}
    ]
  }' \
  < src/test/resources/kafkaMessage.txt
```


### Finding new available dependencies
``` bash
./gradlew dependencyUpdates
```

### Upgrading the Gradle wrapper version to latest
``` bash
./gradlew :wrapper --gradle-version latest
```

## Contributing

To get started, fork the repository and create a new branch.

See details in [CONTRIBUTING.md](CONTRIBUTING.md)

### Contact

This project is maintained by [navikt/tsm](CODEOWNERS)

Questions and/or feature requests? Please create an [issue](https://github.com/navikt/tsm-papirmottak/issues)

If you work in [@navikt](https://github.com/navikt) you can reach us at the Slack
channel [#team-symfoni](https://nav-it.slack.com/archives/C07MY3KCDS5)
