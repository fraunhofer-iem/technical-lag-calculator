package artifact.model

import io.github.z4kn4fein.semver.Version
import io.github.z4kn4fein.semver.toVersion
import kotlinx.serialization.Serializable
import libyears.model.TechnicalLagDto
import libyears.model.TechnicalLagResultStatus
import libyears.model.TechnicalLagUpdatePossibilitiesDto
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
    val technicalLag: TechnicalLagDto,
    val transitiveLibyears: List<Long> = emptyList(),
    val avgLibyears: Double = transitiveLibyears.average(),
    val variance: Double = transitiveLibyears.map { (it - avgLibyears).pow(2) }.average(),
    val stdDev: Double = sqrt(variance),
)

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
    val allVersions: List<VersionDto> = listOf(),
    val updatePossibilities: UpdatePossibilities? = null,
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
                "Technical Lag minor (${technicalLag.minor?.version?.versionNumber})- time lag in days: ${technicalLag.minor?.libyear}, number of missed releases: ${technicalLag.minor?.numberOfMissedReleases} \n" +
                "Transitive Lag: avg. time lag in days: ${stats.minor?.avgLibyears}. Std dev. ${stats.minor?.stdDev}. All time lag: ${stats.minor?.transitiveLibyears}.\n" +
                "Technical Lag patch (${technicalLag.patch?.version?.versionNumber})- time lag in days: ${technicalLag.patch?.libyear}, number of missed releases: ${technicalLag.patch?.numberOfMissedReleases} \n" +
                "Transitive Lag: avg. time lag in days: ${stats.patch?.avgLibyears}. Std dev. ${stats.patch?.stdDev}. All time lag: ${stats.patch?.transitiveLibyears}.\n\n" +
                "After applying major update: ${updatePossibilities?.major}\n" +
                "After applying minor update: ${updatePossibilities?.minor}\n" +
                "After applying patch update: ${updatePossibilities?.patch}\n"
    }

    private fun setSubtreeStats(): TechnicalLagStatsUpdatePossibilitiesDto {


        return if (transitiveDependencies.isEmpty()) {
            TechnicalLagStatsUpdatePossibilitiesDto(
                minor = technicalLag.minor?.let { TechnicalLagStatsDto(it) },
                patch = technicalLag.patch?.let { TechnicalLagStatsDto(it) },
                major = technicalLag.major?.let { TechnicalLagStatsDto(it) },
            )
        } else {
            val transitiveLibyears = transitiveDependencies.map { it.setSubtreeStats() }

            TechnicalLagStatsUpdatePossibilitiesDto(
                minor = technicalLag.minor?.let { technicalLag ->
                    TechnicalLagStatsDto(
                        technicalLag = technicalLag,
                        transitiveLibyears = transitiveLibyears.flatMap { transitiveLibyear ->
                            val minorLibyears =
                                transitiveLibyear.minor?.transitiveLibyears?.toMutableList() ?: mutableListOf()

                            transitiveLibyear.minor?.let { minorLag ->
                                minorLibyears.add(minorLag.technicalLag.libyear)
                            }
                            minorLibyears
                        }
                    )
                },
                patch = technicalLag.patch?.let { technicalLag ->
                    TechnicalLagStatsDto(
                        technicalLag = technicalLag,
                        transitiveLibyears = transitiveLibyears.flatMap { transitiveLibyear ->
                            val patchLibyears =
                                transitiveLibyear.patch?.transitiveLibyears?.toMutableList() ?: mutableListOf()

                            transitiveLibyear.patch?.let { minorLag ->
                                patchLibyears.add(minorLag.technicalLag.libyear)
                            }
                            patchLibyears
                        }
                    )
                },
                major = technicalLag.major?.let { technicalLag ->
                    TechnicalLagStatsDto(
                        technicalLag = technicalLag,
                        transitiveLibyears = transitiveLibyears.flatMap { transitiveLibyear ->
                            val majorLibyears =
                                transitiveLibyear.major?.transitiveLibyears?.toMutableList() ?: mutableListOf()

                            transitiveLibyear.major?.let { minorLag ->
                                majorLibyears.add(minorLag.technicalLag.libyear)
                            }
                            majorLibyears
                        }
                    )
                },

                )

        }
    }

    private val technicalLag: TechnicalLagUpdatePossibilitiesDto by lazy {
        calculateTechnicalLag()
    }

    private val stats: TechnicalLagStatsUpdatePossibilitiesDto by lazy {
        setSubtreeStats()
    }

    // TODO: This needs refactoring. we want to separate libyear calculation and release distance
    // we need a dedicated calculation based on available versions
    private fun calculateTechnicalLag(): TechnicalLagUpdatePossibilitiesDto {

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
            versions.indexOfFirst { it.first == usedVersion } - versions.indexOfFirst { it.first == newestVersion.second }

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
