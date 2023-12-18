package artifact.model

import kotlinx.serialization.Required
import kotlinx.serialization.Serializable
import kotlinx.serialization.Transient
import util.TimeHelper.msToDateString

@Serializable
data class VersionDto(
    val versionNumber: String,
    @Transient
    val releaseDate: Long = -1,
    @Required
    val released: String = msToDateString(releaseDate),
    val isDefault: Boolean = false
)
