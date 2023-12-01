package artifact.model

import kotlinx.serialization.Serializable

@Serializable
data class ArtifactDto(
    val artifactId: String,
    val groupId: String,
    val usedVersion: String,
    val versions: List<VersionDto> = listOf(),
    val isTopLevelDependency: Boolean,
    val transitiveDependencies: List<ArtifactDto> = listOf(),
    val libyear: Long? = null,
)
