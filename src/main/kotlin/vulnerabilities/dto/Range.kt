package vulnerabilities.dto


import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class Range(
    @SerialName("events")
    val events: List<Event?>?,
    @SerialName("type")
    val type: String?
)