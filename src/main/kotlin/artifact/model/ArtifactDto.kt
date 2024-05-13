package artifact.model

import io.github.z4kn4fein.semver.Version
import io.github.z4kn4fein.semver.toVersion
import kotlinx.serialization.Serializable
import technicalLag.model.TechnicalLagDto
import technicalLag.model.TechnicalLagResultStatus
import technicalLag.model.TechnicalLagUpdatePossibilitiesDto
import util.TimeHelper
import kotlin.math.pow
import kotlin.math.sqrt


@Serializable
data class TechnicalLagStatsUpdatePossibilitiesDto(
    val minor: TechnicalLagStatsDto? = null,
    val patch: TechnicalLagStatsDto? = null,
    val major: TechnicalLagStatsDto? = null
)

@Serializable
data class TechnicalLagStatsDto(
    val technicalLag: TechnicalLagDto? = null,
    val transitiveLibyears: List<Long> = emptyList(),
    val transitiveMissedReleases: List<Int> = emptyList(),
    val transitiveDistance: List<Triple<Int, Int, Int>> = emptyList(),
    val avgMissedReleases: Double = if(transitiveMissedReleases.isNotEmpty()) transitiveMissedReleases.average() else 0.0,
    val avgDistance: Triple<Int, Int, Int> = calculateAvgReleaseDistance(transitiveDistance),
    val avgLibyears: Double = if (transitiveLibyears.isNotEmpty()) transitiveLibyears.average() else 0.0,
    val variance: Double = if (transitiveLibyears.isNotEmpty()) transitiveLibyears.map { (it - avgLibyears).pow(2) }.average() else 0.0,
    val stdDev: Double = sqrt(variance),
) {

    companion object {
        private fun calculateAvgReleaseDistance(distances: List<Triple<Int, Int, Int>>): Triple<Int, Int, Int> {
            return if (distances.isNotEmpty()) {
                val sumOfDistances = distances.reduce { acc, triple ->
                    Triple(
                        acc.first + triple.first,
                        acc.second + triple.second,
                        acc.third + triple.third
                    )
                }
                Triple(
                    sumOfDistances.first / distances.size,
                    sumOfDistances.second / distances.size,
                    sumOfDistances.third / distances.size,
                )
            } else {
                Triple(0, 0, 0)
            }
        }
    }

}

@Serializable
data class UpdatePossibilities(
    val minor: ArtifactDto? = null,
    val major: ArtifactDto? = null,
    val patch: ArtifactDto? = null
)

@Serializable
data class ArtifactDto(
    val artifactId: String,
    val groupId: String,
    val usedVersion: VersionDto,
    val allVersions: List<VersionDto>,
    val updatePossibilities: UpdatePossibilities? = null, //TODO: for the transitive libyears we have duplicate entries
    val transitiveDependencies: List<ArtifactDto> = listOf()
) {

    private val validVersions = allVersions.mapNotNull {
        try {
            it.versionNumber.toVersion(strict = false)
            it
        } catch (e: Exception) {
            null
        }
    }

    override fun toString(): String {
        return "$groupId:$artifactId@${usedVersion.versionNumber} \n" +
                "Technical Lag major (${technicalLag.major?.version?.versionNumber})- time lag in days: ${technicalLag.major?.libyear}, number of missed releases: ${technicalLag.major?.numberOfMissedReleases} \n" +
                "Transitive Lag: avg. time lag in days: ${stats.major?.avgLibyears}. Std dev. ${stats.major?.stdDev}. All time lag: ${stats.major?.transitiveLibyears}.\n" +
                "version lag: avg. # of missed releases ${stats.major?.avgMissedReleases}. avg. release distance ${stats.major?.avgDistance}\n\n" +
                "Technical Lag minor (${technicalLag.minor?.version?.versionNumber})- time lag in days: ${technicalLag.minor?.libyear}, number of missed releases: ${technicalLag.minor?.numberOfMissedReleases} \n" +
                "Transitive Lag: avg. time lag in days: ${stats.minor?.avgLibyears}. Std dev. ${stats.minor?.stdDev}. All time lag: ${stats.minor?.transitiveLibyears}.\n" +
                "version lag: avg. # of missed releases ${stats.minor?.avgMissedReleases}. avg. release distance ${stats.minor?.avgDistance}\n\n" +
                "Technical Lag patch (${technicalLag.patch?.version?.versionNumber})- time lag in days: ${technicalLag.patch?.libyear}, number of missed releases: ${technicalLag.patch?.numberOfMissedReleases} \n" +
                "Transitive Lag: avg. time lag in days: ${stats.patch?.avgLibyears}. Std dev. ${stats.patch?.stdDev}. All time lag: ${stats.patch?.transitiveLibyears}.\n\n" +
                "version lag: avg. # of missed releases ${stats.patch?.avgMissedReleases}. avg. release distance ${stats.patch?.avgDistance}\n\n" +
                updatePossibilities?.let {
                    "After applying major update: ${it.major}\n" +
                            "After applying minor update: ${it.minor}\n" +
                            "After applying patch update: ${it.patch}\n"
                } + "\n\n"
    }

    private fun calculateTechnicalLagStats(
        technicalLag: TechnicalLagDto?,
        transitiveStats: List<TechnicalLagStatsDto>
    ): TechnicalLagStatsDto {

        val transitiveLibyears: MutableList<Long> = mutableListOf()
        val transitiveMissedReleases: MutableList<Int> = mutableListOf()
        val transitiveReleaseDistance: MutableList<Triple<Int, Int, Int>> = mutableListOf()

        transitiveStats.forEach { stat ->
            transitiveLibyears.addAll(stat.transitiveLibyears)
            transitiveMissedReleases.addAll(stat.transitiveMissedReleases)
            transitiveReleaseDistance.addAll(stat.transitiveDistance)
        }

        if (technicalLag != null) {
            transitiveLibyears.add(technicalLag.libyear)
            transitiveMissedReleases.add(technicalLag.numberOfMissedReleases)
            transitiveReleaseDistance.add(technicalLag.distance)
        }

        return TechnicalLagStatsDto(
            technicalLag = technicalLag,
            transitiveLibyears = transitiveLibyears,
            transitiveMissedReleases = transitiveMissedReleases,
            transitiveDistance = transitiveReleaseDistance
        )
    }

    private fun setSubtreeStats(): TechnicalLagStatsUpdatePossibilitiesDto {


        return if (transitiveDependencies.isEmpty()) {
            TechnicalLagStatsUpdatePossibilitiesDto(
                minor = technicalLag.minor?.let { TechnicalLagStatsDto(it) },
                patch = technicalLag.patch?.let { TechnicalLagStatsDto(it) },
                major = technicalLag.major?.let { TechnicalLagStatsDto(it) },
            )
        } else {

            val minorStats: MutableList<TechnicalLagStatsDto> = mutableListOf()
            val patchStats: MutableList<TechnicalLagStatsDto> = mutableListOf()
            val majorStats: MutableList<TechnicalLagStatsDto> = mutableListOf()

            transitiveDependencies.forEach {
                val subtreeStats = it.setSubtreeStats()
                subtreeStats.minor?.let { it1 -> minorStats.add(it1) }
                subtreeStats.major?.let { it1 -> majorStats.add(it1) }
                subtreeStats.patch?.let { it1 -> patchStats.add(it1) }
            }

            TechnicalLagStatsUpdatePossibilitiesDto(
                minor = calculateTechnicalLagStats(technicalLag.minor, minorStats),
                major = calculateTechnicalLagStats(technicalLag.major, majorStats),
                patch = calculateTechnicalLagStats(technicalLag.patch, patchStats)

            )

        }
    }

    val technicalLag: TechnicalLagUpdatePossibilitiesDto by lazy {
        setTechnicalLag()
    }

    val stats: TechnicalLagStatsUpdatePossibilitiesDto by lazy {
        setSubtreeStats()
    }

    private fun setTechnicalLag(): TechnicalLagUpdatePossibilitiesDto {

        if (validVersions.contains(usedVersion) && usedVersion.releaseDate != -1L) {


            val current = usedVersion.versionNumber.toVersion(strict = false)
            val filteredVersion = if (current.isStable) {
                getSortedSemVersions(validVersions).filter { it.second.isStable && !it.second.isPreRelease }
            } else {
                getSortedSemVersions(validVersions).filter { !it.second.isPreRelease }
            }

            val major = calculateTechnicalLag(usedVersion, filteredVersion)
            val minor = calculateTechnicalLag(usedVersion, filteredVersion.filter { it.second.major == current.major })
            val patch = calculateTechnicalLag(
                usedVersion,
                filteredVersion.filter { it.second.major == current.major && it.second.minor == current.minor })
            return TechnicalLagUpdatePossibilitiesDto(
                minor = minor,
                patch = patch,
                major = major,
            )
        }

        return TechnicalLagUpdatePossibilitiesDto()
    }

    private fun calculateTechnicalLag(
        usedVersion: VersionDto,
        versions: List<Pair<VersionDto, Version>>
    ): TechnicalLagDto? {

        val newestVersion = try {
            getNewestApplicableVersion(usedVersion, versions)
        } catch (exception: Exception) {
            return null
        }

        val differenceInDays = TimeHelper.getDifferenceInDays(
            currentVersion = usedVersion.releaseDate,
            newestVersion = newestVersion.second.releaseDate
        )

        val missedReleases =
             versions.indexOfFirst { it.first == newestVersion.second } - versions.indexOfFirst { it.first == usedVersion }

        val current = usedVersion.versionNumber.toVersion(strict = false)
        val newest = newestVersion.second.versionNumber.toVersion(strict = false)

        val distance: Triple<Int, Int, Int> =
            Triple(newest.major - current.major, newest.minor - current.minor, newest.patch - current.patch)

        return TechnicalLagDto(
            libyear = -1 * differenceInDays,
            version = newestVersion.second,
            distance = distance,
            numberOfMissedReleases = missedReleases
        )
    }

    /**
     * Returns the newest applicable, stable version compared to the given current version.
     * If a version is explicitly tagged as default this version is used for the comparison.
     * If not the stable version with the highest version number is used.
     * Throws if the current version doesn't follow the semver format.
     */
    private fun getNewestApplicableVersion(
        currentVersion: VersionDto,
        versions: List<Pair<VersionDto, Version>>
    ): Pair<TechnicalLagResultStatus, VersionDto> {

        val current = currentVersion.versionNumber.toVersion(strict = false)
        val newestVersion = versions.last()

        versions.find { it.first.isDefault }?.let { defaultVersion ->
            return if (defaultVersion.second > current) {
                Pair(TechnicalLagResultStatus.SEM_VERSION_WITH_DEFAULT, defaultVersion.first)
            } else {
                Pair(TechnicalLagResultStatus.SEM_VERSION_WITH_DEFAULT, currentVersion)
            }
        }

        if (newestVersion.second > current) {
            return Pair(TechnicalLagResultStatus.SEM_VERSION_WITHOUT_DEFAULT, newestVersion.first)
        }
        return Pair(TechnicalLagResultStatus.SEM_VERSION_WITHOUT_DEFAULT, currentVersion)
    }

    private fun getSortedSemVersions(packageList: List<VersionDto>): List<Pair<VersionDto, Version>> {
        return packageList.map {
            Pair(it, it.versionNumber.toVersion(strict = false))
        }.sortedBy { it.second }
    }
}
