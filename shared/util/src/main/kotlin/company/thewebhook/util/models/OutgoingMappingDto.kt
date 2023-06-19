package company.thewebhook.util.models

import kotlinx.serialization.Serializable

@Serializable
data class OutgoingMappingDto(
    val webhookRequestData: WebhookRequestData,
    val currentDepth: Int,
    val maxDepth: Int,
    val currentDestinationGroups: List<List<String>>,
    val destinationMapping: Map<String, Destination>
)
