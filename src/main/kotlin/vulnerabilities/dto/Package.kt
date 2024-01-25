package vulnerabilities.dto


import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class Package(
    @SerialName("ecosystem")
    val ecosystem: String?,
    @SerialName("name")
    val name: String?,
    @SerialName("purl")
    val purl: String?
)