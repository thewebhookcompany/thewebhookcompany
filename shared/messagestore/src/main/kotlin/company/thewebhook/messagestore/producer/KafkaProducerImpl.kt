package company.thewebhook.messagestore.producer

import company.thewebhook.util.*
import java.util.*
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import kotlin.coroutines.suspendCoroutine
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.apache.kafka.clients.producer.KafkaProducer
import org.apache.kafka.clients.producer.ProducerConfig
import org.apache.kafka.clients.producer.ProducerRecord
import org.apache.kafka.clients.producer.RecordMetadata
import org.apache.kafka.common.errors.RecordTooLargeException
import org.apache.kafka.common.serialization.StringSerializer
import org.slf4j.Logger
import org.slf4j.LoggerFactory

class KafkaProducerImpl<T> : Producer<T>() {
    private val instanceId = UUID.randomUUID().toBase64()
    private val logger: Logger = LoggerFactory.getLogger(this::class.java.name + "-" + instanceId)
    private var producerClient: KafkaProducer<String, T>? = null
    private var config: Map<String, Any?>? = null

    override suspend fun connect(config: Map<String, Any?>) {
        logger.debug("Attempting to connect")
        if (producerClient !== null) {
            logger.debug("Existing client found. Closing its connection...")
            this.close()
            logger.debug("Existing client connection closed")
        }
        val stringSerializerClassName =
            StringSerializer::class.qualifiedName
                ?: throw Exception("Cannot get the qualified name for string serializer")
        this.config =
            config + (ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG to stringSerializerClassName)
        producerClient = KafkaProducer(this.config)
        logger.debug("Connection established")
    }

    private suspend fun publishInternal(topic: String, message: T): Boolean {
        return producerClient?.let { producer ->
            suspendCoroutine {
                producer.send(ProducerRecord(topic, message)) {
                    recordMetadata: RecordMetadata?,
                    exception: Exception? ->
                    if (exception != null) {
                        logger.error("Exception thrown while sending message to kafka", exception)
                        if (exception is RecordTooLargeException) {
                            return@send it.resumeWithException(MessageTooLargeException())
                        }
                        return@send it.resumeWithException(exception)
                    }
                    if (recordMetadata != null) {
                        logger.trace("Publishing message to topic $topic success")
                        return@send it.resume(true)
                    }
                    logger.error("Result Record Metadata is empty")
                    val e = EmptyResultException()
                    it.resumeWithException(e)
                }
            }
        }
            ?: throw NotConnectedException()
    }

    override suspend fun publish(topic: String, message: T): Boolean {
        return publish(topic, listOf(message))[0]
    }

    override suspend fun publish(topic: String, messages: List<T>): List<Boolean> {
        logger.trace("Publishing ${messages.size} messages to topic $topic")
        return messages.map { publishInternal(topic, it) }
    }

    override suspend fun close() {
        withContext(Dispatchers.IO) {
            logger.debug("Closing connection")
            producerClient?.close()
            producerClient = null
            logger.debug("Connection closed")
        }
    }
}
