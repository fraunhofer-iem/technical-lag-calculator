package shared.analyzerResultDtos

import kotlinx.serialization.Serializable

@Serializable
data class AnalyzerResultDto(
    val repositoryInfo: RepositoryInfoDto,
    val environmentInfo: EnvironmentInfoDto,
    val projectDtos: List<ProjectDto>,
)
