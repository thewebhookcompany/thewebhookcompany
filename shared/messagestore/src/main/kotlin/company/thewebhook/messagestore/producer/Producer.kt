package company.thewebhook.messagestore.producer

import company.thewebhook.util.ApplicationEnv
import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds

abstract class Producer<T> {
    companion object {
        private const val configEnvPrefix = "${ApplicationEnv.envPrefix}PRODUCER_CONFIG_"
        fun getConfigFromEnv() = ApplicationEnv.getAllMatchingPrefix(this.configEnvPrefix, true)
    }

    abstract suspend fun connect(config: Map<String, String>)
    abstract suspend fun publish(topic: String, message: T): Boolean
    abstract suspend fun close()
}

abstract class DelayedProducer<T> : Producer<T>() {
    abstract suspend fun publish(topic: String, message: T, delay: Duration): Boolean
    override suspend fun publish(topic: String, message: T): Boolean {
        return publish(topic, message, 0.seconds)
    }
}
