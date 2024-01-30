package vulnerabilities.dto.osv


import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class Affected(
    @SerialName("database_specific")
    val databaseSpecific: DatabaseSpecific? = null,
    @SerialName("package")
    val packageX: PackageX,
    @SerialName("ranges")
    val ranges: MutableList<Range> = mutableListOf()
)