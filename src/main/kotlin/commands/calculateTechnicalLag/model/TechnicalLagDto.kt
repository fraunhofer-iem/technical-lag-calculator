package commands.calculateTechnicalLag.model

import kotlinx.serialization.Serializable


@Serializable
data class TechnicalLagDto(
    val libDays: Long,
    val distance: Triple<Int, Int, Int>,
    val version: String,
    val releaseFrequency: ReleaseFrequencyDto,
    val numberOfMissedReleases: Int
) {
    override fun toString(): String {
        return "Technical Lag: libDays: $libDays, " +
                "target version: $version," +
                " # missed releases: $numberOfMissedReleases, " +
                "Version distance ${distance.first}.${distance.second}.${distance.third} " +
                "Release frequency: $releaseFrequency"
    }
}

data class Statistics(
    val average: Double,
    val variance: Double,
    val stdDev: Double
) {
    override fun toString(): String {
        return "(avg: $average, std dev: $stdDev)"
    }
}

data class TechnicalLagStatistics(
    val technicalLag: TechnicalLagDto? = null,
//    val score: Double,
    val libDays: Statistics?,
    val missedReleases: Statistics?,
    val distance: Triple<Statistics, Statistics, Statistics>?,
    val releaseFrequency: Statistics?,
) {
    override fun toString(): String {
        val properties = listOfNotNull(
            technicalLag?.let { "technicalLag=$it" },
            libDays?.let { "libDays=$it" },
            missedReleases?.let { "missedReleases=$it" },
            distance?.let { "distance=$it" },
            releaseFrequency?.let { "release frequency=$it" }
        ).joinToString(", ")

        return "TechnicalLagStatistics($properties)"
    }
}