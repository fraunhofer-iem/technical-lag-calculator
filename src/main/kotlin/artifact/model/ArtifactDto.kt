package artifact.model

data class ArtifactDto(
    val dbId: Int = -1,
    val artifactId: String,
    val groupId: String,
    val usedVersion: String,
    val scope: String,
    val versions: List<VersionDto>,
    val transitiveDependencies: List<ArtifactDto>
)
