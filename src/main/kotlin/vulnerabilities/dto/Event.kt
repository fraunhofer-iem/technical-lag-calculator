package vulnerabilities.dto


import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class Event(
    @SerialName("fixed")
    val fixed: String?,
    @SerialName("introduced")
    val introduced: String?
)