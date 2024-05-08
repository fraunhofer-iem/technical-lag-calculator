package libyears.model

import artifact.model.VersionDto
import kotlinx.serialization.Serializable

@Serializable
data class TechnicalLagUpdatePossibilitiesDto(
    val minor: TechnicalLagDto? = null,
    val patch: TechnicalLagDto? = null,
    val major: TechnicalLagDto? = null
)

@Serializable
data class TechnicalLagDto(
    val libyear: Long,
    val distance: Triple<Int, Int, Int>,
    val version: VersionDto,
    val numberOfMissedReleases: Int
)
