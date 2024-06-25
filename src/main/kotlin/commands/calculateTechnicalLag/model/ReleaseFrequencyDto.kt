package commands.calculateTechnicalLag.model

import kotlinx.serialization.Serializable

@Serializable
data class ReleaseFrequencyDto(
//    val avgReleaseTime: Triple<Double, Double, Double>,
    val releasesPerDay: Double,
    val releasesPerWeek: Double,
    val releasesPerMonth: Double
)
