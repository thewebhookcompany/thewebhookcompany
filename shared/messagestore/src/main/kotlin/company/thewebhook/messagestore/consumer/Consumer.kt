package company.thewebhook.messagestore.consumer

import company.thewebhook.messagestore.Provider
import company.thewebhook.util.ConfigException
import kotlin.time.Duration

abstract class Consumer<T> {
    interface MessageAcknowledgment<T> {
        suspend fun ack(messageId: ByteArray)
        suspend fun nack(messageId: ByteArray)
    }

    companion object {
        inline fun <reified T> get(provider: Provider, readTimeout: Duration): Consumer<T> {
            return when (provider) {
                Provider.KafkaConsumer -> {
                    KafkaConsumerImpl(readTimeout)
                }
                Provider.PulsarConsumer -> {
                    if (T::class == ByteArray::class) {
                        @Suppress("UNCHECKED_CAST")
                        PulsarConsumerImpl(readTimeout) as Consumer<T>
                    } else {
                        throw ConfigException(
                            "Pulsar Consumer Provider currently does not support any other type other than ByteArray"
                        )
                    }
                }
                else -> {
                    throw ConfigException("Unrecognized provider $provider in Consumer.get")
                }
            }
        }
    }

    data class Record<T>(
        val topic: String,
        val message: T,
    )

    abstract suspend fun connect(config: Map<String, Any?>)

    // Subscribes to the topics given in the list.
    // It will completely replace the existing topic
    // assignment.
    abstract suspend fun subscribe(topics: List<String>)

    // Unsubscribes from all topics that was subscribed to.
    // Same as calling subscribe with an empty list.
    abstract suspend fun unsubscribe()

    abstract suspend fun read(): List<Record<T>>

    // Acknowledges all unacknowledged messages read
    // by the consumer till now.
    // Note that this is regardless of any message specifics
    // like topics, or partitions (in Kafka).
    abstract suspend fun ack()

    // Negative acknowledges all unacknowledged messages read
    // by the consumer till now.
    // Note that this is regardless of any message specifics
    // like topics, or partitions (in Kafka).
    abstract suspend fun nack()

    abstract suspend fun close()
}
