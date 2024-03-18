package libyears.model

import kotlinx.serialization.Serializable

@Serializable
data class LibyearResultDto(
    val libyear: Long? = null, val status: LibyearStatus
)
