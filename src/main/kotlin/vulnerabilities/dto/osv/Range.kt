package vulnerabilities.dto.osv


import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class Range(
    @SerialName("events")
    val events: MutableList<Event> = mutableListOf(),
    @SerialName("type")
    val type: String? = null
)