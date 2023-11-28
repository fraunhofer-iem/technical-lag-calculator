package artifact.model

import kotlinx.serialization.Serializable

@Serializable
data class DependencyGraphGroupDto(val graphs: List<DependencyGraphDto>)
