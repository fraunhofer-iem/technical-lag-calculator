package dependencies.model

import artifact.model.DependencyGraphs
import kotlinx.serialization.Serializable

@Serializable
data class AnalyzerResultDto(
    val repositoryInfo: RepositoryInfoDto,
    val environmentInfo: EnvironmentInfoDto,
    val dependencyGraphDtos: List<DependencyGraphsDto>,
)
