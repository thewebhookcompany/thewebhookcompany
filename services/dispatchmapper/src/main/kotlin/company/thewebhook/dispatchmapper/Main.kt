package company.thewebhook.dispatchmapper

import company.thewebhook.messagestore.ProviderMapper
import company.thewebhook.messagestore.consumer.Consumer
import company.thewebhook.messagestore.producer.Producer
import company.thewebhook.util.ApplicationEnv
import company.thewebhook.util.ConfigException
import company.thewebhook.util.asType
import company.thewebhook.util.models.OutgoingDto
import company.thewebhook.util.models.OutgoingMappingDto
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.seconds
import kotlinx.coroutines.*
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.cbor.Cbor
import kotlinx.serialization.decodeFromByteArray
import kotlinx.serialization.encodeToByteArray

private const val DISPATCHMAPPER_MAX_THREADS_ENV_KEY =
    "${ApplicationEnv.envPrefix}DISPATCHMAPPER_MAX_THREADS"
private const val DISPATCHMAPPER_MAX_CONSUMERS_ENV_KEY =
    "${ApplicationEnv.envPrefix}DISPATCHMAPPER_MAX_CONSUMERS"

fun getOutgoingDtos(outgoingMappingDto: OutgoingMappingDto): List<Pair<OutgoingDto, Long>> {
    val outgoingDtos = mutableListOf<Pair<OutgoingDto, Long>>()
    outgoingMappingDto.currentDestinationGroups.forEach { destinationGroup ->
        val destinations = destinationGroup.mapNotNull { outgoingMappingDto.destinationMapping[it] }
        val totalWeight = destinations.sumOf { it.weight.toLong() }
        val weightChosen = (0..totalWeight).random()
        var cumWeight = 0
        val chosenDestination =
            destinations.find {
                cumWeight += it.weight
                return@find cumWeight >= weightChosen
            }
                ?: destinations[0]
        outgoingDtos.add(
            Pair(
                OutgoingDto(
                    outgoingMappingDto.webhookRequestData,
                    outgoingMappingDto.currentDepth,
                    outgoingMappingDto.maxDepth,
                    chosenDestination.id,
                    outgoingMappingDto.destinationMapping,
                ),
                chosenDestination.delayBeforeSendingMillis
            )
        )
    }
    return outgoingDtos
}

@OptIn(ExperimentalSerializationApi::class, ExperimentalCoroutinesApi::class)
suspend fun main() {
    val providerMapper = ProviderMapper()
    val producerConfig =
        providerMapper.getProducerProviderConfig(listOf("outgoing"))
            ?: throw ConfigException("No producer provider configured for \"outgoing\" topic")
    val consumerConfig =
        providerMapper.getConsumerProviderConfig(listOf("outgoing-mapping"))
            ?: throw ConfigException(
                "No consumer provider configured for \"outgoing-mapping\" topic"
            )
    val producer = Producer.get<ByteArray>(producerConfig.provider)
    val delayedMessages =
        producer.asType<Producer.DelayedMessages<ByteArray>>()
            ?: throw ConfigException(
                "The producer for the topic outgoing needs to have delayed messages functionality"
            )

    producer.connect(producerConfig.internalConfig)

    val numConsumers =
        ApplicationEnv.get(DISPATCHMAPPER_MAX_CONSUMERS_ENV_KEY)?.toIntOrNull()
            ?: throw ConfigException(
                "$DISPATCHMAPPER_MAX_CONSUMERS_ENV_KEY env should be an integer"
            )
    val numThreads =
        ApplicationEnv.get(DISPATCHMAPPER_MAX_THREADS_ENV_KEY)?.toIntOrNull() ?: numConsumers

    val dispatcher = Dispatchers.IO.limitedParallelism(numThreads)
    withContext(dispatcher) {
        val consumeFromTopics = listOf("outgoing-mapping")
        (1..numConsumers).map {
            launch {
                val consumer = Consumer.get<ByteArray>(consumerConfig.provider, 1.seconds)
                consumer.connect(consumerConfig.internalConfig)
                consumer.subscribe(consumeFromTopics)
                while (true) {
                    delay(5000)
                    val records = consumer.read()
                    if (records.isNotEmpty()) {
                        val messagesWithDelays =
                            records
                                .map { Cbor.decodeFromByteArray<OutgoingMappingDto>(it.message) }
                                .flatMap { getOutgoingDtos(it) }
                        val messages = messagesWithDelays.map { Cbor.encodeToByteArray(it.first) }
                        val delays = messagesWithDelays.map { it.second.milliseconds }
                        delayedMessages.publish("outgoing", messages, delays)
                        consumer.ack()
                    }
                }
            }
        }
    }
}
