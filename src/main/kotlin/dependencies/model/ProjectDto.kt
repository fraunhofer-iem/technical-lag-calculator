package dependencies.model

data class ProjectDto(
    val type: String,
    val namespace: String,
    val name: String,
    val version: String
)
