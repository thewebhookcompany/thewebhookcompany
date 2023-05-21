package company.thewebhook.messagestore.producer

import company.thewebhook.messagestore.Provider
import company.thewebhook.util.ConfigException
import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds

abstract class Producer<T> {
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
    abstract suspend fun close()
}

abstract class DelayedProducer<T> : Producer<T>() {
    abstract suspend fun publish(topic: String, message: T, delay: Duration): Boolean
    override suspend fun publish(topic: String, message: T): Boolean {
        return publish(topic, message, 0.seconds)
    }
}
