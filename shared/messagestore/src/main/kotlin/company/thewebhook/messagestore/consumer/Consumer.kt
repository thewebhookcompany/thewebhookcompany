package company.thewebhook.messagestore.consumer

interface Consumer<T> {
    data class Record<T>(
        val topic: String,
        val message: T,
    )

    suspend fun connect(config: Map<String, String>)

    // Subscribes to the topics given in the list.
    // It will completely replace the existing topic
    // assignment.
    suspend fun subscribe(topics: List<String>)

    // Unsubscribes from all topics that was subscribed to.
    // Same as calling subscribe with an empty list.
    suspend fun unsubscribe()

    suspend fun read(): List<Record<T>>

    // Acknowledges all unacknowledged messages read
    // by the consumer till now.
    // Note that this is regardless of any message specifics
    // like topics, or partitions (in Kafka).
    suspend fun ack()

    // Negative acknowledges all unacknowledged messages read
    // by the consumer till now.
    // Note that this is regardless of any message specifics
    // like topics, or partitions (in Kafka).
    suspend fun nack()

    suspend fun close()
}
