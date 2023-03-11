package company.thewebhook.messagestore

import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import kotlin.coroutines.suspendCoroutine
import kotlinx.coroutines.*
import org.apache.kafka.clients.producer.KafkaProducer as KProducer
import org.apache.kafka.clients.producer.ProducerConfig
import org.apache.kafka.clients.producer.ProducerRecord
import org.apache.kafka.clients.producer.RecordMetadata
import org.apache.kafka.common.errors.RecordTooLargeException
import org.apache.kafka.common.serialization.StringSerializer
import org.slf4j.Logger
import org.slf4j.LoggerFactory

interface Producer<T> {
    suspend fun connect(config: Map<String, String>)
    suspend fun publish(topic: String, message: T): Boolean
    suspend fun close()
}

class NotConnectedException(message: String) : Exception(message)

class MessageTooLargeException : Exception()

class EmptyResultException : Exception()

class KafkaProducer<T> : Producer<T> {
    private val logger: Logger = LoggerFactory.getLogger(this::class.java)
    private var producerClient: KProducer<String, T>? = null
    private var config: Map<String, String>? = null

    override suspend fun connect(config: Map<String, String>) {
        logger.debug("Attempting to connect")
        if (producerClient !== null) {
            logger.debug("Existing producer client found. Closing its connection...")
            this.close()
            logger.debug("Existing producer client connection closed")
        }
        val stringSerializerClassName =
            StringSerializer::class.qualifiedName
                ?: throw Exception("Cannot get the qualified name for string serializer")
        this.config =
            config + (ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG to stringSerializerClassName)
        producerClient = KProducer(this.config)
        logger.debug("Connection established")
    }

    override suspend fun publish(topic: String, message: T): Boolean {
        logger.trace("Publishing message to topic $topic")
        return producerClient?.let { producer ->
            withContext(Dispatchers.IO) {
                suspendCoroutine {
                    producer.send(ProducerRecord(topic, message)) {
                        recordMetadata: RecordMetadata?,
                        exception: Exception? ->
                        if (exception != null) {
                            logger.error(
                                "Exception thrown while sending message to kafka",
                                exception
                            )
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
        }
            ?: throw NotConnectedException("Producer is not connected to the cluster")
    }

    override suspend fun close() {
        withContext(Dispatchers.IO) {
            logger.debug("Closing producer connection")
            producerClient?.close()
            producerClient = null
            logger.debug("Producer connection closed")
        }
    }
}
