package dependencies.model

data class AnalyzerResultDto(
    val repositoryInfo: RepositoryInfoDto,
    val environmentInfo: EnvironmentInfoDto,
    val dependencyGraphDto: DependencyGraphDto
)
