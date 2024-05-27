package dependencies.model

import kotlinx.serialization.Serializable

@Serializable
data class RepositoryInfoDto(val url: String, val revision: String, val projects: List<ProjectDto>)

@Serializable
data class ProjectDto(
    val type: String,
    val namespace: String,
    val name: String,
    val version: String
)

