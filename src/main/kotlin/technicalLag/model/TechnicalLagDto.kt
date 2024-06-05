package technicalLag.model

import kotlinx.serialization.Serializable


@Serializable
data class TechnicalLagDto(
    val libDays: Long,
    val distance: Triple<Int, Int, Int>,
    val version: String,
    val numberOfMissedReleases: Int
) {
    override fun toString(): String {
        return "Technical Lag: libDays: $libDays, " +
                "target version: $version," +
                " # missed releases: $numberOfMissedReleases, " +
                "Version distance ${distance.first}.${distance.second}.${distance.third}"
    }
}

data class Statistics(
    val average: Double,
    val variance: Double,
    val stdDev: Double
)

data class TechnicalLagStatistics(
    val technicalLag: TechnicalLagDto? = null,
//    val score: Double,
    val libDays: Statistics?,
    val missedReleases: Statistics?,
    val distance: Triple<Statistics, Statistics, Statistics>,
)