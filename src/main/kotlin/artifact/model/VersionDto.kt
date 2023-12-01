package artifact.model

import kotlinx.serialization.Serializable

@Serializable
data class VersionDto(
    val versionNumber: String,
    val releaseDate: Long = -1,
    val isDefault: Boolean = false
)
