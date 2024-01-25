package vulnerabilities.dto


import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class Reference(
    @SerialName("type")
    val type: String?,
    @SerialName("url")
    val url: String?
)