package company.thewebhook.messagestore.consumer

import company.thewebhook.util.NotConnectedException
import company.thewebhook.util.toBase64
import java.util.*
import java.util.concurrent.TimeUnit
import kotlin.time.Duration
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.apache.pulsar.client.api.BatchReceivePolicy
import org.apache.pulsar.client.api.Consumer as PulsarConsumerClient
import org.apache.pulsar.client.api.MessageId
import org.apache.pulsar.client.api.PulsarClient
import org.slf4j.Logger
import org.slf4j.LoggerFactory

class PulsarConsumerImpl(
    private val readTimeout: Duration,
) : Consumer<ByteArray>(), Consumer.MessageAcknowledgment<ByteArray> {
    private val instanceId = UUID.randomUUID().toBase64()
    private val logger: Logger = LoggerFactory.getLogger(this::class.java.name + "-" + instanceId)
    private var client: PulsarClient? = null
    private var consumerClient: PulsarConsumerClient<ByteArray>? = null
    private var consumerConfig: Map<String, Any?>? = null

    override suspend fun connect(config: Map<String, Any?>) {
        logger.debug("Initialising")
        @Suppress("UNCHECKED_CAST") val clientConfig = config["client"] as Map<String, Any?>
        @Suppress("UNCHECKED_CAST")
        consumerConfig = config["consumer"] as Map<String, Any?>
        PulsarClient.builder().loadConf(clientConfig).build().let { client = it }
        logger.debug("Initialisation successful")
    }

    override suspend fun subscribe(topics: List<String>) {
        logger.trace("Subscribing to ${topics.size} topics")
        unsubscribe()
        client?.let {
            consumerClient =
                it.newConsumer()
                    .loadConf(
                        consumerConfig?.filterKeys { k -> !k.startsWith("batchReceivePolicy") }
                            ?: mapOf()
                    )
                    .batchReceivePolicy(
                        BatchReceivePolicy.builder()
                            .maxNumMessages(
                                consumerConfig
                                    ?.get("batchReceivePolicy.maxNumMessages")
                                    ?.toString()
                                    ?.toInt()
                                    ?: 0
                            )
                            .maxNumBytes(
                                consumerConfig
                                    ?.get("batchReceivePolicy.maxNumBytes")
                                    ?.toString()
                                    ?.toInt()
                                    ?: 0
                            )
                            .messagesFromMultiTopicsEnabled(
                                consumerConfig?.get(
                                    "batchReceivePolicy.messagesFromMultiTopicsEnabled"
                                ) !== "false"
                            )
                            .timeout(readTimeout.inWholeMilliseconds.toInt(), TimeUnit.MILLISECONDS)
                            .build()
                    )
                    .topics(topics)
                    .subscribe()
            logger.trace("Subscription complete")
        }
            ?: throw NotConnectedException()
    }

    override suspend fun unsubscribe() {
        logger.trace("Unsubscribing from all currently assigned topics")
        withContext(Dispatchers.IO) { consumerClient?.close() }
        consumerClient = null
    }

    private val unacknowledgedMessages = mutableListOf<MessageId>()
    override suspend fun read(): List<Record<ByteArray>> {
        logger.trace("Consuming messages")
        return consumerClient?.let { consumer ->
            withContext(Dispatchers.IO) {
                val records = consumer.batchReceive()
                unacknowledgedMessages.addAll(records.map { r -> r.messageId })
                records
                    .map { Record(it.topicName, it.value) }
                    .also { logger.trace("Consumed ${it.size} messages") }
            }
        }
            ?: throw NotConnectedException()
    }

    override suspend fun ack() {
        consumerClient?.let {
            withContext(Dispatchers.IO) { it.acknowledge(unacknowledgedMessages) }
        }
            ?: throw NotConnectedException()
    }

    override suspend fun nack() {
        consumerClient?.let {
            withContext(Dispatchers.IO) {
                unacknowledgedMessages.forEach { mId -> it.negativeAcknowledge(mId) }
            }
        }
            ?: throw NotConnectedException()
    }

    override suspend fun ack(messageId: MessageId) {
        consumerClient?.let {
            withContext(Dispatchers.IO) { it.acknowledge(messageId) }
        }
            ?: throw NotConnectedException()
    }

    override suspend fun nack(messageId: MessageId) {
        consumerClient?.let {
            withContext(Dispatchers.IO) { it.negativeAcknowledge(messageId) }
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
