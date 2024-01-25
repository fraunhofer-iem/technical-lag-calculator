package vulnerabilities.dto


import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class Affected(
    @SerialName("database_specific")
    val databaseSpecific: DatabaseSpecific?,
    @SerialName("package")
    val packageX: Package?,
    @SerialName("ranges")
    val ranges: List<Range?>?
)