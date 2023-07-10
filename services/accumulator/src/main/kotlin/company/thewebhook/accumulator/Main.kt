package company.thewebhook.accumulator

import company.thewebhook.datastore.DatabaseFactory
import company.thewebhook.datastore.destination.DestinationDao
import company.thewebhook.messagestore.ProviderMapper
import company.thewebhook.messagestore.consumer.Consumer
import company.thewebhook.messagestore.producer.Producer
import company.thewebhook.util.ConfigException
import company.thewebhook.util.models.OutgoingMappingDto
import company.thewebhook.util.models.WebhookRequestData
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.serialization.ExperimentalSerializationApi
import kotlin.time.Duration.Companion.seconds
import kotlinx.serialization.cbor.Cbor
import kotlinx.serialization.decodeFromByteArray
import kotlinx.serialization.encodeToByteArray

suspend fun getOutgoingDto(webhookRequestData: WebhookRequestData): OutgoingMappingDto? {
    val destinationDao = DestinationDao.get()
    val dest = destinationDao.getByURL(webhookRequestData.uri, 1)

    if(dest === null) return null

    return OutgoingMappingDto(
        webhookRequestData = webhookRequestData,
        currentDepth = 0,
        maxDepth = 10,
        currentDestinationGroups = dest.onFailureDestinations,
        destinationMapping = destinationDao.getAll(),
    )
}

@OptIn(ExperimentalSerializationApi::class)
suspend fun main() {
    val providerMapper = ProviderMapper()
    val producerConfig =
        providerMapper.getProducerProviderConfig(listOf("outgoing-mapping"))
            ?: throw ConfigException("No producer provider configured for \"outgoing-mapping\" topic")
    val consumerConfig =
        providerMapper.getConsumerProviderConfig(listOf("incoming"))
            ?: throw ConfigException(
                "No consumer provider configured for \"incoming\" topic"
            )
    val producer = Producer.get<ByteArray>(producerConfig.provider)
    producer.connect(producerConfig.internalConfig)

    DatabaseFactory.connect()

    withContext(Dispatchers.IO) {
        launch {
            val consumer = Consumer.get<ByteArray>(consumerConfig.provider, 1.seconds)
            consumer.connect(consumerConfig.internalConfig)
            consumer.subscribe(listOf("incoming"))

            while (true) {
                delay(5000)
                val records = consumer.read()
                if (records.isNotEmpty()) {
                    val webhookDataList =
                        records
                            .map { Cbor.decodeFromByteArray<WebhookRequestData>(it.message) }

                    val outgoingMappingDtos = webhookDataList
                        .map { Cbor.encodeToByteArray(getOutgoingDto(it)) }

                    producer.publish("outgoing-mapping", outgoingMappingDtos)
                    consumer.ack()
                }
            }
        }
    }
}