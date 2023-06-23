package company.thewebhook.messagestore.producer

import company.thewebhook.util.NotConnectedException
import company.thewebhook.util.toBase64
import java.util.*
import java.util.concurrent.TimeUnit
import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.apache.pulsar.client.api.MessageId
import org.apache.pulsar.client.api.Producer as PulsarProducerClient
import org.apache.pulsar.client.api.PulsarClient
import org.apache.pulsar.client.api.PulsarClientException
import org.slf4j.Logger
import org.slf4j.LoggerFactory

class PulsarProducerImpl : Producer<ByteArray>(), Producer.DelayedMessages<ByteArray> {
    private val instanceId = UUID.randomUUID().toBase64()
    private val logger: Logger = LoggerFactory.getLogger(this::class.java.name + "-" + instanceId)
    private var client: PulsarClient? = null
    private var producers: MutableMap<String, PulsarProducerClient<ByteArray>> = mutableMapOf()
    private var producerConfig: Map<String, Any?>? = null

    override suspend fun connect(config: Map<String, Any?>) {
        logger.debug("Initialising")
        @Suppress("UNCHECKED_CAST") val clientConfig = config["client"] as Map<String, Any?>
        @Suppress("UNCHECKED_CAST")
        producerConfig = config["producer"] as Map<String, Any?>
        producerConfig =
            config
                .filterKeys { it.startsWith("PRODUCER_") }
                .mapKeys { it.key.removePrefix("PRODUCER_") }
        client = PulsarClient.builder().loadConf(clientConfig).build()
        logger.debug("Initialisation successful")
    }

    private fun closeProducer(topic: String) {
        logger.debug("Closing producer for topic $topic")
        val producer = producers[topic]
        producers.remove(topic)
        try {
            producer?.close()
        } catch (_: PulsarClientException.AlreadyClosedException) {}
        logger.debug("Producer for topic $topic closed")
    }

    private fun connectToProducer(topic: String): PulsarProducerClient<ByteArray> {
        closeProducer(topic)
        return client?.let {
            logger.debug("Connecting to producer for topic $topic")
            val builder = it.newProducer()
            producerConfig?.let { conf -> builder.loadConf(conf) }
            producers[topic] = builder.topic(topic).create()
            logger.debug("Connection to producer for topic $topic successful")
            producers[topic]
        }
            ?: throw NotConnectedException()
    }

    private fun publishInternal(topic: String, message: ByteArray, delay: Duration): MessageId {
        val producer = producers[topic] ?: connectToProducer(topic)
        val messageBuilder = producer.newMessage()
        val delayMs = delay.inWholeMilliseconds
        if (delayMs > 0) {
            messageBuilder.deliverAfter(delayMs, TimeUnit.MILLISECONDS)
        }
        messageBuilder.value(message)
        return messageBuilder.send()
    }

    override suspend fun publish(topic: String, message: ByteArray): Boolean {
        return publish(topic, message, 0.seconds)
    }

    override suspend fun publish(topic: String, messages: List<ByteArray>): List<Boolean> {
        return publish(topic, messages, 0.seconds)
    }

    override suspend fun publish(topic: String, message: ByteArray, delay: Duration): Boolean {
        return publish(topic, listOf(message), listOf(delay))[0]
    }

    override suspend fun publish(
        topic: String,
        messages: List<ByteArray>,
        delay: Duration
    ): List<Boolean> {
        return publish(topic, messages, messages.map { delay })
    }

    override suspend fun publish(
        topic: String,
        messages: List<ByteArray>,
        delays: List<Duration>
    ): List<Boolean> {
        if (messages.size != delays.size) {
            throw IllegalArgumentException(
                "messages (size: ${messages.size
            }) and delays (size: ${delays.size}) must be of the same size"
            )
        }
        return withContext(Dispatchers.IO) {
            logger.trace("Publishing ${messages.size} messages to topic $topic")
            messages.mapIndexed { i, message ->
                val delay = delays[i]
                publishInternal(topic, message, delay)
            }
            logger.trace("Publishing ${messages.size} messages to topic $topic successful")
            messages.map { true }
        }
    }

    override suspend fun close() {
        withContext(Dispatchers.IO) {
            logger.debug("Closing all topic producers")
            // we do not need to manually close all topic producers, pulsar takes care of that for
            // us
            client?.close()
            client = null
            logger.debug("All topic producers closed")
        }
    }
}
