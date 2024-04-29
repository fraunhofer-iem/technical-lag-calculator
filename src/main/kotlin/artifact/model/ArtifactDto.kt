package artifact.model

import io.github.z4kn4fein.semver.Version
import io.github.z4kn4fein.semver.toVersion
import kotlinx.serialization.Serializable
import libyears.model.LibyearResultDto
import libyears.model.LibyearStatus
import util.TimeHelper
import kotlin.math.pow
import kotlin.math.sqrt


@Serializable
data class LibyearStats(
    val libyear: LibyearResultDto,
    val transitiveLibyears: List<Long> = emptyList(),
    val avgLibyears: Double = transitiveLibyears.average(),
    val variance: Double = transitiveLibyears.map { (it - avgLibyears).pow(2) }.average(),
    val stdDev: Double = sqrt(variance)
)

@Serializable
data class ArtifactDto(
    val artifactId: String,
    val groupId: String,
    val usedVersion: VersionDto,
    val versions: List<VersionDto> = listOf(),
    val updatePossibilities: UpdatePossibilities? = null,
    val transitiveDependencies: List<ArtifactDto> = listOf(),
) {

    private fun setSubtreeStats(): LibyearStats {

        return if (transitiveDependencies.isEmpty()) {
            LibyearStats(libyearResult)
        } else {
            val transitiveLibyears = transitiveDependencies.map { it.setSubtreeStats() }

            LibyearStats(
                libyearResult,
                transitiveLibyears = transitiveLibyears.flatMap {
                    val transitiveLibyears = it.transitiveLibyears.toMutableList()
                    if (it.libyear.libyear != null) {
                        transitiveLibyears.add(it.libyear.libyear)
                    }
                    transitiveLibyears
                }
            )

        }
    }

    private val libyearResult: LibyearResultDto by lazy {
        calculateLibyear()
    }

    val stats: LibyearStats by lazy {
        setSubtreeStats()
    }

    private fun calculateLibyear(): LibyearResultDto {

        if (versions.contains(usedVersion) && usedVersion.releaseDate != -1L) {
            val newestVersion = try {
                getNewestApplicableVersion(usedVersion, versions)
            } catch (exception: Exception) {
                getNewestVersion(versions)
            }

            val differenceInDays = TimeHelper.getDifferenceInDays(
                currentVersion = usedVersion.releaseDate,
                newestVersion = newestVersion.second.releaseDate
            )

            return if (differenceInDays <= 0) {
                LibyearResultDto(libyear = -1 * differenceInDays, status = newestVersion.first)
            } else {
                LibyearResultDto(libyear = 0, status = LibyearStatus.NEWER_THAN_DEFAULT)
            }
        }

        return LibyearResultDto(status = LibyearStatus.NO_RESULT)
    }

    /**
     * Returns the newest applicable, stable version compared to the given current version.
     * If a version is explicitly tagged as default this version is used for the comparison.
     * If not the stable version with the highest version number is used.
     * Throws if the current version doesn't follow the semver format.
     */
    private fun getNewestApplicableVersion(
        currentVersion: VersionDto,
        packageList: List<VersionDto>
    ): Pair<LibyearStatus, VersionDto> {
        val current = currentVersion.versionNumber.toVersion(strict = false)
        current.isPreRelease
        val versions = if (current.isStable) {
            getSortedSemVersions(packageList).filter { it.second.isStable && !it.second.isPreRelease }
        } else {
            getSortedSemVersions(packageList).filter { !it.second.isPreRelease }
        }

        val newestVersion = versions.last()

        versions.find { it.first.isDefault }?.let { defaultVersion ->
            return if (defaultVersion.second > current) {
                Pair(LibyearStatus.SEM_VERSION_WITH_DEFAULT, defaultVersion.first)
            } else {
                Pair(LibyearStatus.SEM_VERSION_WITH_DEFAULT, currentVersion)
            }
        }

        if (newestVersion.second > current) {
            return Pair(LibyearStatus.SEM_VERSION_WITHOUT_DEFAULT, newestVersion.first)
        }
        return Pair(LibyearStatus.SEM_VERSION_WITHOUT_DEFAULT, currentVersion)
    }

    private fun getNewestVersion(packageList: List<VersionDto>): Pair<LibyearStatus, VersionDto> {
        // If available we use the release date of the default version for comparison
        // as this is the recommended version of the maintainers
        val newestVersionByDate = packageList.maxBy { it.releaseDate }
        val defaultVersion = packageList.filter { it.isDefault }

        return if (defaultVersion.count() == 1) {
            Pair(LibyearStatus.DATE_WITH_DEFAULT, defaultVersion.first())
        } else {
            Pair(LibyearStatus.DATE_WITHOUT_DEFAULT, newestVersionByDate)
        }
    }

    private fun getSortedSemVersions(packageList: List<VersionDto>): List<Pair<VersionDto, Version>> {
        return packageList.mapNotNull {
            try {
                Pair(it, it.versionNumber.toVersion(strict = false))
            } catch (exception: Exception) {
                null
            }
        }.sortedBy { it.second }
    }
}

@Serializable
data class UpdatePossibilities(
    val minor: ArtifactDto? = null,
    val major: ArtifactDto? = null,
    val patch: ArtifactDto? = null
)

@Serializable
data class ArtifactWithStats(
    val artifact: ArtifactDto,
    val stats: LibyearStats
)
