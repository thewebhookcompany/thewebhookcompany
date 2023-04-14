package company.thewebhook.messagestore.producer

interface Producer<T> {
    suspend fun connect(config: Map<String, String>)
    suspend fun publish(topic: String, message: T): Boolean
    suspend fun close()
}
