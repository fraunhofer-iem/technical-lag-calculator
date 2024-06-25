package shared.analyzerResultDtos

import kotlinx.serialization.Serializable

@Serializable
data class RepositoryInfoDto(val url: String, val revision: String, val projects: List<ProjectInfoDto>)

@Serializable
data class ProjectInfoDto(
    val type: String,
    val namespace: String,
    val name: String,
    val version: String
)

