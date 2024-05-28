//package artifact.model
//
//import io.github.z4kn4fein.semver.Version
//import io.github.z4kn4fein.semver.toVersion
//import kotlinx.serialization.Serializable
//import kotlinx.serialization.Transient
//import technicalLag.model.TechnicalLagDto
//import technicalLag.model.TechnicalLagUpdatePossibilitiesDto
//import util.TimeHelper
//import kotlin.math.pow
//import kotlin.math.sqrt
//
//
//
//
//@Serializable
//data class TechnicalLagStatsUpdatePossibilitiesDto(
//    val minor: TechnicalLagStatsDto,
//    val patch: TechnicalLagStatsDto,
//    val major: TechnicalLagStatsDto
//)
//
//@Serializable
//data class TechnicalLagStatsDto(
//    val technicalLag: TechnicalLagDto? = null,
//    val transitiveLibyears: List<Long> = emptyList(),
//    val transitiveMissedReleases: List<Int> = emptyList(),
//    val transitiveDistance: List<Triple<Int, Int, Int>> = emptyList(),
//    val avgTransitiveMissedReleases: Double = if (transitiveMissedReleases.isNotEmpty()) transitiveMissedReleases.average() else 0.0,
//    val avgTransitiveDistance: Triple<Double, Double, Double> = calculateAvgReleaseDistance(transitiveDistance),
//    val avgTransitiveLibyears: Double = if (transitiveLibyears.isNotEmpty()) transitiveLibyears.average() else 0.0,
//    val variance: Double = if (transitiveLibyears.isNotEmpty()) transitiveLibyears.map {
//        (it - avgTransitiveLibyears).pow(
//            2
//        )
//    }
//        .average() else 0.0,
//    val stdDev: Double = sqrt(variance),
//) {
//
//    companion object {
//        private fun calculateAvgReleaseDistance(distances: List<Triple<Int, Int, Int>>): Triple<Double, Double, Double> {
//            return if (distances.isNotEmpty()) {
//                val sumOfDistances = distances.reduce { acc, triple ->
//                    Triple(
//                        acc.first + triple.first,
//                        acc.second + triple.second,
//                        acc.third + triple.third
//                    )
//                }
//                Triple(
//                    sumOfDistances.first.toDouble() / distances.size,
//                    sumOfDistances.second.toDouble() / distances.size,
//                    sumOfDistances.third.toDouble() / distances.size,
//                )
//            } else {
//                Triple(0.0, 0.0, 0.0)
//            }
//        }
//    }
//
//}
//
//@Serializable
//data class UpdatePossibilities(
//    val minor: ArtifactDto? = null,
//    val major: ArtifactDto? = null,
//    val patch: ArtifactDto? = null
//)
//
//@Serializable
//data class UpdatePossibilitiesWithStats(
//    val minor: ArtifactWithStatsDto? = null,
//    val major: ArtifactWithStatsDto? = null,
//    val patch: ArtifactWithStatsDto? = null
//)
//
//@Serializable
//data class ArtifactWithStatsDto(
//    val artifactId: String,
//    val groupId: String,
////    val usedVersion: VersionDto,
////    val allVersions: List<VersionDto>,
//    val updatePossibilitiesWithStats: UpdatePossibilitiesWithStats,
//    val transitiveDependencies: List<ArtifactWithStatsDto>,
//    val stats: TechnicalLagStatsUpdatePossibilitiesDto
//) {
//    constructor(artifact: ArtifactDto) : this(
//        artifactId = artifact.artifactId,
//        groupId = artifact.groupId,
////        usedVersion = artifact.usedVersion,
////        allVersions = artifact.allVersions,
//        updatePossibilitiesWithStats = UpdatePossibilitiesWithStats(
//            minor = artifact.updatePossibilities.minor?.let { ArtifactWithStatsDto(it) },
//            major = artifact.updatePossibilities.major?.let { ArtifactWithStatsDto(it) },
//            patch = artifact.updatePossibilities.patch?.let { ArtifactWithStatsDto(it) },
//        ),
//        transitiveDependencies = artifact.transitiveDependencies.map { ArtifactWithStatsDto(it) },
//        stats = artifact.stats,
//    )
//}
//
//@Serializable
//data class ArtifactDto(
//    val artifactId: String,
//    val groupId: String,
//    val usedVersion: String,
//    val updatePossibilities: UpdatePossibilities = UpdatePossibilities(),
//    val transitiveDependencies: List<ArtifactDto> = listOf()
//) {
//
////    private val validVersions = allVersions.mapNotNull {
////        try {
////            it.versionNumber.toVersion(strict = false)
////            it
////        } catch (e: Exception) {
////            null
////        }
////    }
//
////    override fun toString(): String {
////        return "$groupId:$artifactId@${usedVersion} \n" +
////                "Technical Lag major (${stats.major.technicalLag?.version?.versionNumber})- time lag in days: ${stats.major.technicalLag?.libyear}, number of missed releases: ${stats.major.technicalLag?.numberOfMissedReleases} \n" +
////                "Transitive Lag: avg. time lag in days: ${stats.major.avgTransitiveLibyears}. Std dev. ${stats.major.stdDev}. All time lag: ${stats.major.transitiveLibyears}.\n" +
////                "version lag: avg. # of missed releases ${stats.major.avgTransitiveMissedReleases}. avg. release distance ${stats.major.avgTransitiveDistance}\n\n" +
////                "Technical Lag minor (${stats.minor.technicalLag?.version?.versionNumber})- time lag in days: ${stats.minor.technicalLag?.libyear}, number of missed releases: ${stats.minor.technicalLag?.numberOfMissedReleases} \n" +
////                "Transitive Lag: avg. time lag in days: ${stats.minor.avgTransitiveLibyears}. Std dev. ${stats.minor.stdDev}. All time lag: ${stats.minor.transitiveLibyears}.\n" +
////                "version lag: avg. # of missed releases ${stats.minor.avgTransitiveMissedReleases}. avg. release distance ${stats.minor.avgTransitiveDistance}\n\n" +
////                "Technical Lag patch (${stats.patch.technicalLag?.version?.versionNumber})- time lag in days: ${stats.patch.technicalLag?.libyear}, number of missed releases: ${stats.patch.technicalLag?.numberOfMissedReleases} \n" +
////                "Transitive Lag: avg. time lag in days: ${stats.patch.avgTransitiveLibyears}. Std dev. ${stats.patch.stdDev}. All time lag: ${stats.patch.transitiveLibyears}.\n\n" +
////                "version lag: avg. # of missed releases ${stats.patch.avgTransitiveMissedReleases}. avg. release distance ${stats.patch.avgTransitiveDistance}\n\n" +
////                updatePossibilities.let {
////                    "After applying major update: ${it.major}\n" +
////                            "After applying minor update: ${it.minor}\n" +
////                            "After applying patch update: ${it.patch}\n"
////                } +
////                "\n\n"
////    }
//
//
//
//
//
//
////    private fun calculateTechnicalLag(
////        usedVersion: VersionDto,
////        versions: List<Pair<VersionDto, Version>>
////    ): TechnicalLagDto? {
////
////        if (versions.isEmpty()) {
////            return null
////        }
////        val newestVersion =
////            getNewestApplicableVersion(usedVersion, versions)
////
////
////        val differenceInDays = TimeHelper.getDifferenceInDays(
////            currentVersion = usedVersion.releaseDate,
////            newestVersion = newestVersion.second.releaseDate
////        )
////
////        val missedReleases =
////            versions.indexOfFirst { it.first == newestVersion.second } - versions.indexOfFirst { it.first == usedVersion }
////
////        val current = usedVersion.versionNumber.toVersion(strict = false)
////        val newest = newestVersion.second.versionNumber.toVersion(strict = false)
////
////        val distance: Triple<Int, Int, Int> =
////            Triple(newest.major - current.major, newest.minor - current.minor, newest.patch - current.patch)
////
////        return TechnicalLagDto(
////            libyear = -1 * differenceInDays,
////            version = newestVersion.second,
////            distance = distance,
////            numberOfMissedReleases = missedReleases
////        )
////    }
//
//
//}
