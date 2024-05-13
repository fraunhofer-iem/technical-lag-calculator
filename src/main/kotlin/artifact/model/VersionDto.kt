package artifact.model

import kotlinx.serialization.Serializable
import kotlinx.serialization.Transient
import util.TimeHelper.msToDateString

enum class VersionTypes {
    Minor, Major, Patch
}

@Serializable
data class VersionDto(
    val versionNumber: String,
    val releaseDate: Long,
    @Transient
    val released: String = msToDateString(releaseDate),
    val isDefault: Boolean = false
)
