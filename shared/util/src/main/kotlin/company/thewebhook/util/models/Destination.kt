package company.thewebhook.util.models

import kotlinx.serialization.Serializable

enum class HttpVersion {
    V1_1,
    V2
}

@Serializable
data class Destination(
    val id: String,
    val url: String,
    val httpVersion: HttpVersion,
    val timeoutMillis: Long,
    val delayBeforeSendingMillis: Long,
    val filterTransform: Transform?,
    val bodyTransform: Transform?,
    val weight: Int,
    val onFailureDestinations: List<List<String>>
)
