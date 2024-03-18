package http.deps.model


import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class VersionKeyX(
    @SerialName("name")
    val name: String,
    @SerialName("system")
    val system: String,
    @SerialName("version")
    val version: String
)