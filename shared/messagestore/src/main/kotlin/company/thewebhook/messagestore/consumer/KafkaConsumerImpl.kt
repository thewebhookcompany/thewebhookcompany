package company.thewebhook.messagestore.consumer

import company.thewebhook.util.NotConnectedException
import company.thewebhook.util.toBase64
import java.time.Duration
import java.util.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.withContext
import org.apache.kafka.clients.consumer.ConsumerConfig
import org.apache.kafka.clients.consumer.KafkaConsumer
import org.apache.kafka.common.serialization.StringSerializer
import org.slf4j.Logger
import org.slf4j.LoggerFactory

class KafkaConsumerImpl<T>(
    private val readTimeout: Duration,
) : Consumer<T>() {
    private val instanceId = UUID.randomUUID().toBase64()
    private val logger: Logger = LoggerFactory.getLogger(this::class.java.name + "-" + instanceId)
    private var consumerClient: KafkaConsumer<String, T>? = null
    private var config: Map<String, String>? = null
    private var topics: List<String>? = null

    override suspend fun connect(config: Map<String, String>) {
        logger.debug("Attempting to connect")
        consumerClient?.let {
            logger.debug("Existing client found. Closing its connection...")
            this.close()
            logger.debug("Existing client connection closed")
        }
        val stringSerializerClassName =
            StringSerializer::class.qualifiedName
                ?: throw Exception("Cannot get the qualified name for string serializer")
        this.config =
            config + (ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG to stringSerializerClassName)
        consumerClient = KafkaConsumer(this.config)
        logger.debug("Connection established")
    }

    override suspend fun subscribe(topics: List<String>) {
        logger.trace("Subscribing to ${topics.size} topics")
        consumerClient?.let {
            withContext(Dispatchers.IO) {
                this@KafkaConsumerImpl.topics = topics
                it.subscribe(this@KafkaConsumerImpl.topics)
                logger.trace("Subscription complete")
            }
        }
            ?: throw NotConnectedException()
    }

    override suspend fun unsubscribe() {
        logger.trace("Unsubscribing from all currently assigned topics")
        consumerClient?.let { withContext(Dispatchers.IO) { it.unsubscribe() } }
            ?: throw NotConnectedException()
    }
    override suspend fun read(): List<Record<T>> {
        logger.trace("Consuming messages")
        return consumerClient?.let { consumer ->
            withContext(Dispatchers.IO) {
                val records = consumer.poll(readTimeout)
                records
                    .map { Consumer.Record(it.topic(), it.value()) }
                    .also { logger.trace("Consumed ${it.size} messages") }
            }
        }
            ?: throw NotConnectedException()
    }

    override suspend fun ack() {
        consumerClient?.let { withContext(Dispatchers.IO) { it.commitSync() } }
            ?: throw NotConnectedException()
    }

    override suspend fun nack() {
        consumerClient?.let {
            withContext(Dispatchers.IO) {
                it.committed(it.assignment()).map { (t, u) -> async { it.seek(t, u) } }.awaitAll()
            }
        }
            ?: throw NotConnectedException()
    }

    override suspend fun close() {
        withContext(Dispatchers.IO) {
            logger.debug("Closing consumer connection")
            consumerClient?.close()
            consumerClient = null
            logger.debug("Consumer connection closed")
        }
    }
}
