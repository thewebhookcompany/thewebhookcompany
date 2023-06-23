package company.thewebhook.util.models

import kotlinx.serialization.Serializable

@Serializable
data class Destination(
    val id: String,
    val url: String,
    val delayBeforeSendingMillis: Long,
    val transformationScript: String?,
    val weight: Int,
    val onFailureDestinations: List<List<String>>
)
