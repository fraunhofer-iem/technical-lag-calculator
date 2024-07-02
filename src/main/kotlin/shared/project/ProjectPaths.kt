package shared.project

import kotlinx.serialization.Serializable

@Serializable
data class ProjectPaths(val paths: List<String>)
