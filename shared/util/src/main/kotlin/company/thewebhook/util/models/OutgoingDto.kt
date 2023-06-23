package company.thewebhook.util.models

import kotlinx.serialization.Serializable

@Serializable
data class OutgoingDto(
    val webhookRequestData: WebhookRequestData,
    val currentDepth: Int,
    val maxDepth: Int,
    val currentDestinationId: String,
    val destinationMapping: Map<String, Destination>
)
