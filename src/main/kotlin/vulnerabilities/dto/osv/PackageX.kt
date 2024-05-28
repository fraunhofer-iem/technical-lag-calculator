package vulnerabilities.dto.osv


import dependencies.model.ArtifactVersion
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class PackageX(
    @SerialName("ecosystem")
    val ecosystem: String? = null,
    @SerialName("name")
    val name: String,
    @SerialName("purl")
    val purl: String? = null,
    @SerialName("versions")
    var versions: MutableList<ArtifactVersion> = mutableListOf()
)