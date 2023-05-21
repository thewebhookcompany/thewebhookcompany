package company.thewebhook.messagestore.producer

import company.thewebhook.util.NotConnectedException
import company.thewebhook.util.toBase64
import java.util.*
import java.util.concurrent.TimeUnit
import kotlin.time.Duration
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.apache.pulsar.client.api.MessageId
import org.apache.pulsar.client.api.Producer as PulsarProducerClient
import org.apache.pulsar.client.api.PulsarClient
import org.apache.pulsar.client.api.PulsarClientException
import org.slf4j.Logger
import org.slf4j.LoggerFactory

class PulsarProducerImpl : DelayedProducer<ByteArray>() {
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

    override suspend fun publish(topic: String, message: ByteArray, delay: Duration): Boolean {
        return withContext(Dispatchers.IO) {
            val messageId =
                try {
                    logger.trace("Publishing message to topic $topic")
                    publishInternal(topic, message, delay)
                } catch (e: PulsarClientException.AlreadyClosedException) {
                    logger.debug("Republishing message to topic $topic")
                    publishInternal(topic, message, delay)
                }
            logger.trace("Publishing message to topic $topic successful with message id $messageId")
            true
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
