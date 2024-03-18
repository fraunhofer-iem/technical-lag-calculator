package vulnerabilities.dto.osv


import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class Event(
    @SerialName("fixed")
    val fixed: String? = null,
    @SerialName("introduced")
    val introduced: String? = null,
)