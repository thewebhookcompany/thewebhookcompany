package company.thewebhook.messagestore.producer

import company.thewebhook.messagestore.Provider
import company.thewebhook.util.ConfigException
import kotlin.time.Duration

abstract class Producer<T> {
    interface DelayedMessages<T> {
        suspend fun publish(topic: String, message: T, delay: Duration): Boolean
        suspend fun publish(topic: String, messages: List<T>, delay: Duration): List<Boolean>
        suspend fun publish(topic: String, messages: List<T>, delays: List<Duration>): List<Boolean>
    }

    companion object {
        inline fun <reified T> get(provider: Provider): Producer<T> {
            return when (provider) {
                Provider.KafkaProducer -> {
                    KafkaProducerImpl()
                }
                Provider.PulsarProducer -> {
                    if (T::class == ByteArray::class) {
                        @Suppress("UNCHECKED_CAST")
                        PulsarProducerImpl() as Producer<T>
                    } else {
                        throw ConfigException(
                            "Pulsar Producer Provider currently does not support any other type other than ByteArray"
                        )
                    }
                }
                else -> {
                    throw ConfigException("Unrecognized provider $provider in Producer.get")
                }
            }
        }
    }

    abstract suspend fun connect(config: Map<String, Any?>)
    abstract suspend fun publish(topic: String, message: T): Boolean
    abstract suspend fun publish(topic: String, messages: List<T>): List<Boolean>
    abstract suspend fun close()
}
