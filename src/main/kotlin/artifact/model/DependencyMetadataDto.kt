package artifact.model

data class DependencyMetadataDto(val usedVersion: String, val scope: String, val isTransitiveDependency: Boolean)
