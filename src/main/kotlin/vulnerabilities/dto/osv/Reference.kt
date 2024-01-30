package vulnerabilities.dto.osv


import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class Reference(
    @SerialName("type")
    val type: String? = null,
    @SerialName("url")
    val url: String? = null
)