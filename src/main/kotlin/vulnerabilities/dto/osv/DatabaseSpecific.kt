package vulnerabilities.dto.osv


import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class DatabaseSpecific(
    @SerialName("source")
    val source: String? = null,
)