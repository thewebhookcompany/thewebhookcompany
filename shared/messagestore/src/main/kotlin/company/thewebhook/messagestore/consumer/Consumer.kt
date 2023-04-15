package company.thewebhook.messagestore.consumer

import company.thewebhook.messagestore.util.ApplicationEnv

abstract class Consumer<T> {
    companion object {
        private const val configEnvPrefix = "${ApplicationEnv.envPrefix}CONSUMER_CONFIG_"
        fun getConfigFromEnv() = ApplicationEnv.getAllMatchingPrefix(this.configEnvPrefix, true)
    }

    data class Record<T>(
        val topic: String,
        val message: T,
    )

    abstract suspend fun connect(config: Map<String, String>)

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
