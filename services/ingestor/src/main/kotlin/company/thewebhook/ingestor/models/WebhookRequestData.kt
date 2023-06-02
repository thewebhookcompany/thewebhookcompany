package company.thewebhook.ingestor.models

import kotlinx.serialization.Serializable

@Serializable
data class WebhookRequestData(
    val method: String,
    val uri: String,
    val headers: Map<String, List<String>>,
    val body: String,
    val receivedAt: Long,
    val senderHost: String,
    val serverHost: String,
)
