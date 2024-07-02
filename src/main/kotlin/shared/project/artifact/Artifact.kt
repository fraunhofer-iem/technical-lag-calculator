package shared.project.artifact

import commands.calculateTechnicalLag.model.ReleaseFrequencyDto
import commands.calculateTechnicalLag.model.TechnicalLagDto
import util.TimeHelper.getDifferenceInDays
import util.TimeHelper.getDifferenceInMonths
import util.TimeHelper.getDifferenceInWeeks

/**
 * A class representing a package used in a dependency.
 * An artifact can be referenced from multiple dependency graphs' nodes.
 * As the versions list can grow very large it's recommended to make sure to create only
 * one artifact for each package and reuse it properly.
 */
class Artifact(
    versions: List<ArtifactVersion> = listOf(),
    val artifactId: String,
    val groupId: String,
    val sortedVersions: List<ArtifactVersion> = versions.sorted(),
    private val versionToVersionTypeToTechLag: MutableMap<String, TechnicalLagDto> =
        mutableMapOf()
) {

    override fun toString(): String {
        return "${groupId}:${artifactId} with ${sortedVersions.size} versions."
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

    val releaseFrequency by lazy { calculateReleaseFrequency(sortedVersions) }


    private fun calculateReleaseFrequency(versions: List<ArtifactVersion>): ReleaseFrequencyDto {

        val versionByDate = versions.filter { !it.semver.isPreRelease }.sortedBy { it.releaseDate }

        return if (versionByDate.isNotEmpty()) {
            val firstVersion = versionByDate.first()
            val latestVersion = versionByDate.last()

            val diffInDays = getDifferenceInDays(firstVersion.releaseDate, latestVersion.releaseDate)
            val diffInWeeks = getDifferenceInWeeks(firstVersion.releaseDate, latestVersion.releaseDate)
            val diffInMonths = getDifferenceInMonths(firstVersion.releaseDate, latestVersion.releaseDate)

            val frequencyDay = if (diffInDays == 0L) 0.0 else versionByDate.count().toDouble() / diffInDays.toDouble()

            val frequencyWeek =
                if (diffInWeeks == 0.0) 0.0 else versionByDate.count().toDouble() / diffInWeeks

            val frequencyMonth =
                if (diffInMonths == 0.0) 0.0 else versionByDate.count().toDouble() / diffInMonths


            ReleaseFrequencyDto(frequencyDay, frequencyWeek, frequencyMonth)
        } else {
            ReleaseFrequencyDto(0.0, 0.0, 0.0)
        }

//        if (versions.isEmpty()) {
//            return ReleaseFrequency(avgReleaseTime = Triple(0.0, 0.0, 0.0))
//        }

//        var counterMajorReleases = 0
//        var timeMajorAggregate = 0L
//        val majorReleaseDistanceDays: MutableList<Long> = mutableListOf()
//        val minorReleaseDistanceDays: MutableList<Long> = mutableListOf()
//        val patchReleaseDistanceDays: MutableList<Long> = mutableListOf()
//
//        var counterMinorReleases = 0
//        var timeMinorAggregate = 0L
//
//        var counterPatchReleases = 0
//        var timePatchAggregate = 0L
//
//        var lastVersion = versions[0]
//
//        versions.forEach { version ->
//            when {
//                lastVersion.semver.major == version.semver.major && lastVersion.semver.minor == version.semver.minor -> {
//                    // this is a patch update
//                    patchReleaseDistanceDays.add(getDifferenceInDays(lastVersion.releaseDate, version.releaseDate))
//                    counterPatchReleases += 1
//                    timePatchAggregate += getDifferenceInDays(lastVersion.releaseDate, version.releaseDate)
//                }
//
//                lastVersion.semver.major == version.semver.major && version.semver.minor > lastVersion.semver.minor -> {
//                    // this is a minor update
//                    minorReleaseDistanceDays.add(getDifferenceInDays(lastVersion.releaseDate, version.releaseDate))
//                    counterMinorReleases += 1
//                    timeMinorAggregate += getDifferenceInDays(lastVersion.releaseDate, version.releaseDate)
//                }
//
//                version.semver.major > lastVersion.semver.major -> {
//                    // this is a major update
//                    majorReleaseDistanceDays.add(getDifferenceInDays(lastVersion.releaseDate, version.releaseDate))
//                    counterMajorReleases += 1
//                    timeMajorAggregate += getDifferenceInDays(lastVersion.releaseDate, version.releaseDate)
//                }
//            }
//
//            lastVersion = version
//        }
//
//        return ReleaseFrequency(
//            avgReleaseTime = Triple(
//                if (majorReleaseDistanceDays.isEmpty()) 0.0 else majorReleaseDistanceDays.average(),
//                if (minorReleaseDistanceDays.isEmpty()) 0.0 else minorReleaseDistanceDays.average(),
//                if (patchReleaseDistanceDays.isEmpty()) 0.0 else patchReleaseDistanceDays.average()
//            )
//        )

    }


    private fun calculateTechnicalLag(version: String, versionType: VersionType): TechnicalLagDto? {

        return if (sortedVersions.isEmpty()) {
            null
        } else {
            val newestVersion =
                ArtifactVersion.findHighestApplicableVersion(version, sortedVersions, versionType)
            val currentVersion = sortedVersions.find { it.versionNumber == version }
            if (newestVersion != null && currentVersion != null) {

                val differenceInDays = getDifferenceInDays(
                    currentVersion = currentVersion.releaseDate,
                    newestVersion = newestVersion.releaseDate
                )

                val filteredVersions =
                    sortedVersions.filter { it.semver.isStable || currentVersion.semver.isPreRelease == it.semver.isPreRelease }

                val missedReleases =
                    filteredVersions.indexOfFirst { it.versionNumber == newestVersion.versionNumber } - filteredVersions.indexOfFirst { it.versionNumber == currentVersion.versionNumber }

                TechnicalLagDto(
                    libDays = differenceInDays,
                    version = newestVersion.versionNumber,
                    distance = calculateReleaseDistance(newestVersion, currentVersion),
                    numberOfMissedReleases = missedReleases,
                    releaseFrequency = releaseFrequency
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
