package dependencies.model

data class RepositoryInfoDto(val url: String, val revision: String, val projects: List<ProjectDto>)
