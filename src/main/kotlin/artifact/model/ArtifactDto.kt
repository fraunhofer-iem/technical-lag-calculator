package artifact.model

import kotlinx.serialization.Serializable
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
