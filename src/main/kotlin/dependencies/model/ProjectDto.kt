package dependencies.model

import kotlinx.serialization.Serializable

@Serializable
data class ProjectDto(
    val type: String,
    val namespace: String,
    val name: String,
    val version: String
)
