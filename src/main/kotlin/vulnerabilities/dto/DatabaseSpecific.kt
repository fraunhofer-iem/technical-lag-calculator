package vulnerabilities.dto


import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class DatabaseSpecific(
    @SerialName("source")
    val source: String?
)