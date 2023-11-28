package dependencies.model

import artifact.model.DependencyGraphDto

data class AnalyzerResultDto(
    val repositoryInfo: RepositoryInfoDto,
    val environmentInfo: EnvironmentInfoDto,
    val dependencyGraphDto: DependencyGraphDto
)
