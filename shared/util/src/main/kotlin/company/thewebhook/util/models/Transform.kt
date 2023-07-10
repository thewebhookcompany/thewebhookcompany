package company.thewebhook.util.models

import kotlinx.serialization.Serializable

@Serializable
data class Transform(
    val id: String,
    val script: String,
)
