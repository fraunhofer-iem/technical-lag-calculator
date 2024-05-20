package dependencies.model

import artifact.model.VersionDto
import kotlinx.serialization.Serializable

@Serializable
data class AnalyzerResultDto(
    val repositoryInfo: RepositoryInfoDto,
    val environmentInfo: EnvironmentInfoDto,
    val dependencyGraphDto: DependencyGraphDto,
    val versions: List<VersionMap>
)

@Serializable
data class VersionMap(
    val id: String,
    val versions: List<VersionDto>
)
