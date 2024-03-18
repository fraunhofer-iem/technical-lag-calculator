package dependencies.model

import artifact.model.ArtifactDto
import kotlinx.serialization.Serializable

@Serializable
data class DependencyGraphDto(
    val packageManagerToScopes: Map<String, ScopedDependencyDto>
)

@Serializable
data class ScopedDependencyDto(
    val scopesToDependencies: Map<String, List<ArtifactDto>>,
)
