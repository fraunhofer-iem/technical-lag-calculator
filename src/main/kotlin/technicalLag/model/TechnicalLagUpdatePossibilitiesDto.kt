package technicalLag.model

import kotlinx.serialization.Serializable


@Serializable
data class TechnicalLagDto(
    val libyear: Long,
    val distance: Triple<Int, Int, Int>,
    val version: String,
    val numberOfMissedReleases: Int
)
