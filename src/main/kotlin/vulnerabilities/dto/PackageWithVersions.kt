package vulnerabilities.dto

import artifact.model.VersionDto
import kotlinx.serialization.Serializable

@Serializable
data class PackageWithVersions(
    val packageX: Package,
    val versions: List<VersionDto> = listOf()
)
