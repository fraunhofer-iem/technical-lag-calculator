package dependencies.model

import kotlinx.serialization.Serializable

@Serializable
data class AnalyzerResultDto(
    val repositoryInfo: RepositoryInfoDto,
    val environmentInfo: EnvironmentInfoDto,
    val dependencyGraphDto: DependencyGraphDto
)
