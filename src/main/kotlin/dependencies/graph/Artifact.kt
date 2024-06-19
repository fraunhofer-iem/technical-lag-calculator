package dependencies.graph

import technicalLag.model.TechnicalLagDto
import util.TimeHelper

/**
 * A data class representing a package used in a dependency.
 * An artifact can be referenced from multiple dependency graphs' nodes.
 * As the versions list can grow very large it's recommended to make sure to create only
 * one artifact for each package and reuse it properly.
 */
class Artifact(
    val artifactId: String,
    val groupId: String,
    versions: List<ArtifactVersion> = listOf(),
    val sortedVersions: List<ArtifactVersion> = versions.sorted(),
    private val versionToVersionTypeToTechLag: MutableMap<String, TechnicalLagDto> =
        mutableMapOf()
) {

    override fun toString(): String {
        return "${groupId}:${artifactId} with ${sortedVersions.size} versions."
    }

    fun getTechLagMap(): Map<String, TechnicalLagDto> {
        return versionToVersionTypeToTechLag
    }

    /**
     * Returns the technical lag between the given rawVersion and the target version defined by
     * versionType (major, minor, patch).
     */
    fun getTechLagForVersion(rawVersion: String, versionType: VersionType): TechnicalLagDto? {
        val version = ArtifactVersion.validateAndHarmonizeVersionString(rawVersion)
        val ident = "$version-$versionType"

        return if (versionToVersionTypeToTechLag.contains(ident)) {
            versionToVersionTypeToTechLag[ident]
        } else {
            val techLag = calculateTechnicalLag(version, versionType)
            if (techLag != null) {
                versionToVersionTypeToTechLag[ident] = techLag
            }
            techLag
        }
    }

    private fun calculateTechnicalLag(version: String, versionType: VersionType): TechnicalLagDto? {

        return if (sortedVersions.isEmpty()) {
            null
        } else {
            val newestVersion =
                ArtifactVersion.findHighestApplicableVersion(version, sortedVersions, versionType)
            val currentVersion = sortedVersions.find { it.versionNumber == version }
            if (newestVersion != null && currentVersion != null) {

                val differenceInDays = TimeHelper.getDifferenceInDays(
                    currentVersion = currentVersion.releaseDate,
                    newestVersion = newestVersion.releaseDate
                )

                val filteredVersions =
                    sortedVersions.filter { it.semver.isStable || currentVersion.semver.isPreRelease == it.semver.isPreRelease }

                val missedReleases =
                    filteredVersions.indexOfFirst { it.versionNumber == newestVersion.versionNumber } - filteredVersions.indexOfFirst { it.versionNumber == currentVersion.versionNumber }

                TechnicalLagDto(
                    libDays = -1 * differenceInDays,
                    version = newestVersion.versionNumber,
                    distance = calculateReleaseDistance(newestVersion, currentVersion),
                    numberOfMissedReleases = missedReleases
                )
            } else {
                null
            }
        }
    }

    private fun calculateReleaseDistance(
        newer: ArtifactVersion,
        older: ArtifactVersion
    ): Triple<Int, Int, Int> {
        val oldSemVer = older.semver
        val newSemVer = newer.semver

        if (oldSemVer == newSemVer) {
            return Triple(0, 0, 0)
        }

        val newerIdx = sortedVersions.indexOf(newer)
        val olderIdx = sortedVersions.indexOf(older)

        var majorCounter = 0
        var minorCounter = 0
        var patchCounter = 0

        var maxMajor = oldSemVer.major
        var maxMinor = if (maxMajor == newSemVer.major) oldSemVer.minor else -1

        for (i in (olderIdx + 1)..newerIdx) {
            val current = sortedVersions[i].semver

            if (current.isStable || current.isPreRelease == oldSemVer.isPreRelease) {
                when {
                    current.major > maxMajor -> {
                        maxMajor = current.major
                        majorCounter += 1
                        maxMinor = current.minor
                    }

                    current.major == newSemVer.major -> {
                        when {
                            current.minor > maxMinor -> {
                                maxMinor = current.minor
                                minorCounter += 1
                            }

                            current.minor == newSemVer.minor -> patchCounter += 1
                        }
                    }
                }
            }
        }

        return Triple(majorCounter, minorCounter, patchCounter)
    }
}