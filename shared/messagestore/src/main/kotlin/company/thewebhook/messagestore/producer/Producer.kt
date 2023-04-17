package company.thewebhook.messagestore.producer

import company.thewebhook.messagestore.util.ApplicationEnv

abstract class Producer<T> {
    companion object {
        private const val configEnvPrefix = "${ApplicationEnv.envPrefix}PRODUCER_CONFIG_"
        fun getConfigFromEnv() = ApplicationEnv.getAllMatchingPrefix(this.configEnvPrefix, true)
    }

    abstract suspend fun connect(config: Map<String, String>)
    abstract suspend fun publish(topic: String, message: T): Boolean
    abstract suspend fun close()
}
