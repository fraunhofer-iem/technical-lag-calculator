package artifact.model

import kotlinx.serialization.Serializable
import libyears.LibyearStats
import libyears.model.LibyearResultDto

@Serializable
data class ArtifactDto(
    val artifactId: String,
    val groupId: String,
    val usedVersion: VersionDto,
    val versions: List<VersionDto> = listOf(),
    val isTopLevelDependency: Boolean,
    val transitiveDependencies: List<ArtifactDto> = listOf(),
    val libyearResult: LibyearResultDto,
)

@Serializable
data class ArtifactWithStats(
    val artifact: ArtifactDto,
    val stats: LibyearStats
)
