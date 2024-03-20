package artifact.model

import kotlinx.serialization.Serializable
import libyears.LibyearStats

@Serializable
data class ArtifactDto(
    val artifactId: String,
    val groupId: String,
    val usedVersion: VersionDto,
    val versions: List<VersionDto> = listOf(),
    val updatePossibilities: UpdatePossibilities? = null,
    val transitiveDependencies: List<ArtifactDto> = listOf(),
)

@Serializable
data class UpdatePossibilities(
    val minor: ArtifactDto? = null,
    val major: ArtifactDto? = null,
    val patch: ArtifactDto? = null
)

@Serializable
data class ArtifactWithStats(
    val artifact: ArtifactDto,
    val stats: LibyearStats
)
