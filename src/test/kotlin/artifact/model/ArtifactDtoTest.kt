package artifact.model

import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toInstant
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class ArtifactDtoTest {

    @Test
    fun testMinimalArtifact() {
        val artifact = ArtifactDto(
            artifactId = "artifactId",
            groupId = "groupId",
            usedVersion = VersionDto(versionNumber = "1.0.0", releaseDate = 2L),
            allVersions = listOf(VersionDto(versionNumber = "1.0.0", releaseDate = 2L))
        )

        assertEquals(Triple(0.0, 0.0, 0.0), artifact.stats.major.avgDistance)
        assertEquals(Triple(0.0, 0.0, 0.0), artifact.stats.minor.avgDistance)
        assertEquals(Triple(0.0, 0.0, 0.0), artifact.stats.patch.avgDistance)


        assertEquals(0.0, artifact.stats.major.avgLibyears)
        assertEquals(0.0, artifact.stats.minor.avgLibyears)
        assertEquals(0.0, artifact.stats.patch.avgLibyears)

        assertEquals(0.0, artifact.stats.major.avgMissedReleases)
        assertEquals(0.0, artifact.stats.minor.avgMissedReleases)
        assertEquals(0.0, artifact.stats.patch.avgMissedReleases)

        assertEquals(0L, artifact.technicalLag.major?.libyear)
        assertEquals(Triple(0, 0, 0), artifact.technicalLag.major?.distance)
        assertEquals(0, artifact.technicalLag.major?.numberOfMissedReleases)

        assertEquals(0L, artifact.technicalLag.minor?.libyear)
        assertEquals(Triple(0, 0, 0), artifact.technicalLag.minor?.distance)
        assertEquals(0, artifact.technicalLag.minor?.numberOfMissedReleases)

        assertEquals(0L, artifact.technicalLag.patch?.libyear)
        assertEquals(Triple(0, 0, 0), artifact.technicalLag.patch?.distance)
        assertEquals(0, artifact.technicalLag.patch?.numberOfMissedReleases)

        assertTrue(artifact.stats.major.transitiveLibyears.isEmpty())
        assertTrue(artifact.stats.major.transitiveDistance.isEmpty())
        assertTrue(artifact.stats.major.transitiveMissedReleases.isEmpty())

        assertTrue(artifact.stats.minor.transitiveLibyears.isEmpty())
        assertTrue(artifact.stats.minor.transitiveDistance.isEmpty())
        assertTrue(artifact.stats.minor.transitiveMissedReleases.isEmpty())

        assertTrue(artifact.stats.patch.transitiveLibyears.isEmpty())
        assertTrue(artifact.stats.patch.transitiveDistance.isEmpty())
        assertTrue(artifact.stats.patch.transitiveMissedReleases.isEmpty())
    }

    @Test
    fun testMissingUsedVersionArtifact() {
        val artifact = ArtifactDto(
            artifactId = "artifactId",
            groupId = "groupId",
            usedVersion = VersionDto(versionNumber = "1.0.0", releaseDate = 2L),
            allVersions = emptyList()
        )

        assertEquals(Triple(0.0, 0.0, 0.0), artifact.stats.major.avgDistance)
        assertEquals(Triple(0.0, 0.0, 0.0), artifact.stats.minor.avgDistance)
        assertEquals(Triple(0.0, 0.0, 0.0), artifact.stats.patch.avgDistance)

        assertEquals(0.0, artifact.stats.major.avgLibyears)
        assertEquals(0.0, artifact.stats.minor.avgLibyears)
        assertEquals(0.0, artifact.stats.patch.avgLibyears)

        assertEquals(0.0, artifact.stats.major.avgMissedReleases)
        assertEquals(0.0, artifact.stats.minor.avgMissedReleases)
        assertEquals(0.0, artifact.stats.patch.avgMissedReleases)

        assertEquals(null, artifact.technicalLag.major?.libyear)
        assertEquals(null, artifact.technicalLag.major?.distance)
        assertEquals(null, artifact.technicalLag.major?.numberOfMissedReleases)

        assertEquals(null, artifact.technicalLag.minor?.libyear)
        assertEquals(null, artifact.technicalLag.minor?.distance)
        assertEquals(null, artifact.technicalLag.minor?.numberOfMissedReleases)

        assertEquals(null, artifact.technicalLag.patch?.libyear)
        assertEquals(null, artifact.technicalLag.patch?.distance)
        assertEquals(null, artifact.technicalLag.patch?.numberOfMissedReleases)

        assertTrue(artifact.stats.major.transitiveLibyears.isEmpty())
        assertTrue(artifact.stats.major.transitiveDistance.isEmpty())
        assertTrue(artifact.stats.major.transitiveMissedReleases.isEmpty())

        assertTrue(artifact.stats.minor.transitiveLibyears.isEmpty())
        assertTrue(artifact.stats.minor.transitiveDistance.isEmpty())
        assertTrue(artifact.stats.minor.transitiveMissedReleases.isEmpty())


        assertTrue(artifact.stats.patch.transitiveLibyears.isEmpty())
        assertTrue(artifact.stats.patch.transitiveDistance.isEmpty())
        assertTrue(artifact.stats.patch.transitiveMissedReleases.isEmpty())
        println(artifact.stats)
        println(artifact.technicalLag)
    }

    @Test
    fun testMinimalArtifactWithSingleDepNoLag() {

        val transitiveDeps = listOf(
            ArtifactDto(
                artifactId = "artifactId",
                groupId = "groupId",
                usedVersion = VersionDto(versionNumber = "1.0.0", releaseDate = 2L),
                allVersions = listOf(VersionDto(versionNumber = "1.0.0", releaseDate = 2L)),
                transitiveDependencies = listOf()
            )
        )

        val root = ArtifactDto(
            artifactId = "artifactId",
            groupId = "groupId",
            usedVersion = VersionDto(versionNumber = "1.0.0", releaseDate = 2L),
            allVersions = listOf(VersionDto(versionNumber = "1.0.0", releaseDate = 2L)),
            transitiveDependencies = transitiveDeps
        )

        assertEquals(Triple(0.0, 0.0, 0.0), root.stats.major.avgDistance)
        assertEquals(Triple(0.0, 0.0, 0.0), root.stats.minor.avgDistance)
        assertEquals(Triple(0.0, 0.0, 0.0), root.stats.patch.avgDistance)


        assertEquals(0.0, root.stats.major.avgLibyears)
        assertEquals(0.0, root.stats.minor.avgLibyears)
        assertEquals(0.0, root.stats.patch.avgLibyears)

        assertEquals(0.0, root.stats.major.avgMissedReleases)
        assertEquals(0.0, root.stats.minor.avgMissedReleases)
        assertEquals(0.0, root.stats.patch.avgMissedReleases)

        assertEquals(0L, root.technicalLag.major?.libyear)
        assertEquals(Triple(0, 0, 0), root.technicalLag.major?.distance)
        assertEquals(0, root.technicalLag.major?.numberOfMissedReleases)

        assertEquals(0L, root.technicalLag.minor?.libyear)
        assertEquals(Triple(0, 0, 0), root.technicalLag.minor?.distance)
        assertEquals(0, root.technicalLag.minor?.numberOfMissedReleases)

        assertEquals(0L, root.technicalLag.patch?.libyear)
        assertEquals(Triple(0, 0, 0), root.technicalLag.patch?.distance)
        assertEquals(0, root.technicalLag.patch?.numberOfMissedReleases)

        assertEquals(listOf(0L), root.stats.major.transitiveLibyears)
        assertEquals(
            0, root.stats.major.transitiveDistance.first().first
        )
        assertEquals(
            0, root.stats.major.transitiveDistance.first().second
        )
        assertEquals(
            0, root.stats.major.transitiveDistance.first().third
        )
        assertEquals(
            listOf(0), root.stats.major.transitiveMissedReleases
        )
        assertEquals(listOf(0L), root.stats.patch.transitiveLibyears)
        assertEquals(
            0, root.stats.patch.transitiveDistance.first().first
        )
        assertEquals(
            0, root.stats.patch.transitiveDistance.first().second
        )
        assertEquals(
            0, root.stats.patch.transitiveDistance.first().third
        )
        assertEquals(
            listOf(0), root.stats.patch.transitiveMissedReleases
        )
        assertEquals(listOf(0L), root.stats.minor.transitiveLibyears)
        assertEquals(
            0, root.stats.minor.transitiveDistance.first().first
        )
        assertEquals(
            0, root.stats.minor.transitiveDistance.first().second
        )
        assertEquals(
            0, root.stats.minor.transitiveDistance.first().third
        )
        assertEquals(
            listOf(0), root.stats.minor.transitiveMissedReleases
        )
    }

    @Test
    fun testMinimalArtifactWithSingleDepWithLag() {

        val transitiveDeps = listOf(
            ArtifactDto(
                artifactId = "artifactId",
                groupId = "groupId",
                usedVersion = VersionDto(versionNumber = "1.0.0", releaseDate = 1000000000000L),
                allVersions = listOf(
                    VersionDto(versionNumber = "1.0.0", releaseDate = 1000000000000L),
                    VersionDto(versionNumber = "1.0.1", releaseDate = 1001010000000L),
                    VersionDto(versionNumber = "1.1.0", releaseDate = 1002020000000L),
                    VersionDto(versionNumber = "2.2.1", releaseDate = 1003030000000L)
                ),
                transitiveDependencies = listOf()
            )
        )

        val root = ArtifactDto(
            artifactId = "artifactId",
            groupId = "groupId",
            usedVersion = VersionDto(versionNumber = "1.0.0", releaseDate = 2L),
            allVersions = listOf(VersionDto(versionNumber = "1.0.0", releaseDate = 2L)),
            transitiveDependencies = transitiveDeps
        )
        println(root)
        root.transitiveDependencies.forEach { println(it) }
        println(root.stats) // TODO: this needs fixed, we don't report the libyear from the transitive deps

        assertEquals(Triple(1.0, 2.0, 1.0), root.stats.major.avgDistance)
        assertEquals(Triple(0.0, 1.0, 0.0), root.stats.minor.avgDistance)
        assertEquals(Triple(0.0, 0.0, 1.0), root.stats.patch.avgDistance)


        assertEquals(35.0, root.stats.major.avgLibyears)
        assertEquals(23.0, root.stats.minor.avgLibyears)
        assertEquals(11.0, root.stats.patch.avgLibyears)

        assertEquals(3.0, root.stats.major.avgMissedReleases)
        assertEquals(2.0, root.stats.minor.avgMissedReleases)
        assertEquals(1.0, root.stats.patch.avgMissedReleases)

        assertEquals(0L, root.technicalLag.major?.libyear)
        assertEquals(Triple(0, 0, 0), root.technicalLag.major?.distance)
        assertEquals(0, root.technicalLag.major?.numberOfMissedReleases)

        assertEquals(0L, root.technicalLag.minor?.libyear)
        assertEquals(Triple(0, 0, 0), root.technicalLag.minor?.distance)
        assertEquals(0, root.technicalLag.minor?.numberOfMissedReleases)

        assertEquals(0L, root.technicalLag.patch?.libyear)
        assertEquals(Triple(0, 0, 0), root.technicalLag.patch?.distance)
        assertEquals(0, root.technicalLag.patch?.numberOfMissedReleases)

        assertEquals(listOf(35L), root.stats.major.transitiveLibyears)
        assertEquals(
            1, root.stats.major.transitiveDistance.first().first
        )
        assertEquals(
            2, root.stats.major.transitiveDistance.first().second
        )
        assertEquals(
            1, root.stats.major.transitiveDistance.first().third
        )
        assertEquals(
            listOf(3), root.stats.major.transitiveMissedReleases
        )
        assertEquals(listOf(11L), root.stats.patch.transitiveLibyears)
        assertEquals(
            0, root.stats.patch.transitiveDistance.first().first
        )
        assertEquals(
            0, root.stats.patch.transitiveDistance.first().second
        )
        assertEquals(
            1, root.stats.patch.transitiveDistance.first().third
        )
        assertEquals(
            listOf(1), root.stats.patch.transitiveMissedReleases
        )
        assertEquals(listOf(23L), root.stats.minor.transitiveLibyears)
        assertEquals(
            0, root.stats.minor.transitiveDistance.first().first
        )
        assertEquals(
            1, root.stats.minor.transitiveDistance.first().second
        )
        assertEquals(
            0, root.stats.minor.transitiveDistance.first().third
        )
        assertEquals(
            listOf(2), root.stats.minor.transitiveMissedReleases
        )
    }

    @Test
    fun testMinimalArtifactWithSingleDepWithLagWithIntermediateVersions() {

        val transitiveDeps = listOf(
            ArtifactDto(
                artifactId = "artifactId",
                groupId = "groupId",
                usedVersion = VersionDto(versionNumber = "1.0.0", releaseDate = 1000000000000L),
                allVersions = listOf(
                    VersionDto(versionNumber = "1.0.0", releaseDate = 1000000000000L),
                    VersionDto(versionNumber = "1.0.1", releaseDate = 1001010000000L),
                    VersionDto(versionNumber = "1.0.2", releaseDate = 1001110000000L),
                    VersionDto(versionNumber = "1.1.0", releaseDate = 1002020000000L),
                    VersionDto(versionNumber = "1.1.1", releaseDate = 1002220000000L),
                    VersionDto(versionNumber = "2.2.1", releaseDate = 1003030000000L),
                    VersionDto(versionNumber = "2.3.2", releaseDate = 1004030000000L)
                ),
                transitiveDependencies = listOf()
            )
        )

        val root = ArtifactDto(
            artifactId = "artifactId",
            groupId = "groupId",
            usedVersion = VersionDto(versionNumber = "1.0.0", releaseDate = 2L),
            allVersions = listOf(VersionDto(versionNumber = "1.0.0", releaseDate = 2L)),
            transitiveDependencies = transitiveDeps
        )
        println(root)
        root.transitiveDependencies.forEach { println(it) }
        println(root.stats) // TODO: this needs fixed, we don't report the libyear from the transitive deps

        assertEquals(Triple(1.0, 3.0, 2.0), root.stats.major.avgDistance)
        assertEquals(Triple(0.0, 1.0, 1.0), root.stats.minor.avgDistance)
        assertEquals(Triple(0.0, 0.0, 2.0), root.stats.patch.avgDistance)


        assertEquals(46.0, root.stats.major.avgLibyears)
        assertEquals(25.0, root.stats.minor.avgLibyears)
        assertEquals(12.0, root.stats.patch.avgLibyears)

        assertEquals(6.0, root.stats.major.avgMissedReleases)
        assertEquals(4.0, root.stats.minor.avgMissedReleases)
        assertEquals(2.0, root.stats.patch.avgMissedReleases)

        assertEquals(0L, root.technicalLag.major?.libyear)
        assertEquals(Triple(0, 0, 0), root.technicalLag.major?.distance)
        assertEquals(0, root.technicalLag.major?.numberOfMissedReleases)

        assertEquals(0L, root.technicalLag.minor?.libyear)
        assertEquals(Triple(0, 0, 0), root.technicalLag.minor?.distance)
        assertEquals(0, root.technicalLag.minor?.numberOfMissedReleases)

        assertEquals(0L, root.technicalLag.patch?.libyear)
        assertEquals(Triple(0, 0, 0), root.technicalLag.patch?.distance)
        assertEquals(0, root.technicalLag.patch?.numberOfMissedReleases)
    }

    @Test
    fun testMinimalArtifactWithMultipleDepsWithLagWithIntermediateVersions() {

        val startDate = LocalDateTime(2024, 1, 1, 0, 0).toInstant(TimeZone.of("UTC+3")).toEpochMilliseconds()
        val majorVersionDate = LocalDateTime(2024, 1, 3, 0, 0).toInstant(TimeZone.of("UTC+3")).toEpochMilliseconds()
        val minorVersionDate = LocalDateTime(2024, 1, 9, 0, 0).toInstant(TimeZone.of("UTC+3")).toEpochMilliseconds()
        val patchVersionDate = LocalDateTime(2024, 1, 19, 0, 0).toInstant(TimeZone.of("UTC+3")).toEpochMilliseconds()
        val secondMajorVersionDate =
            LocalDateTime(2024, 2, 1, 0, 0).toInstant(TimeZone.of("UTC+3")).toEpochMilliseconds()
        val secondMinorVersionDate =
            LocalDateTime(2024, 2, 9, 0, 0).toInstant(TimeZone.of("UTC+3")).toEpochMilliseconds()
        val secondPatchVersionDate =
            LocalDateTime(2024, 2, 19, 0, 0).toInstant(TimeZone.of("UTC+3")).toEpochMilliseconds()

        val transitiveDeps = listOf(
            ArtifactDto(
                artifactId = "artifactId",
                groupId = "groupId",
                usedVersion = VersionDto(versionNumber = "1.0.0", releaseDate = startDate),
                allVersions = listOf(
                    VersionDto(versionNumber = "1.0.0", releaseDate = startDate),
                    VersionDto(versionNumber = "1.0.1", releaseDate = 1001010000000L),
                    VersionDto(versionNumber = "1.0.2", releaseDate = patchVersionDate),
                    VersionDto(versionNumber = "1.1.0", releaseDate = 1002020000000L),
                    VersionDto(versionNumber = "1.1.1", releaseDate = minorVersionDate),
                    VersionDto(versionNumber = "2.2.1", releaseDate = 1003030000000L),
                    VersionDto(versionNumber = "2.3.2", releaseDate = majorVersionDate)
                ),
                transitiveDependencies = listOf(),
            ),
            ArtifactDto(
                artifactId = "artifactId",
                groupId = "groupId",
                usedVersion = VersionDto(versionNumber = "1.0.0", releaseDate = startDate),
                allVersions = listOf(
                    VersionDto(versionNumber = "1.0.0", releaseDate = startDate),
                    VersionDto(versionNumber = "1.0.1", releaseDate = 1002010000000L),
                    VersionDto(versionNumber = "1.0.2", releaseDate = secondPatchVersionDate),
                    VersionDto(versionNumber = "1.1.0", releaseDate = 1003020000000L),
                    VersionDto(versionNumber = "1.1.1", releaseDate = secondMinorVersionDate),
                    VersionDto(versionNumber = "2.2.1", releaseDate = 1004030000000L),
                    VersionDto(versionNumber = "3.3.2", releaseDate = secondMajorVersionDate)
                ),
                transitiveDependencies = listOf(),
            )
        )

        val root = ArtifactDto(
            artifactId = "artifactId",
            groupId = "groupId",
            usedVersion = VersionDto(versionNumber = "1.0.0", releaseDate = 2L),
            allVersions = listOf(VersionDto(versionNumber = "1.0.0", releaseDate = 2L)),
            transitiveDependencies = transitiveDeps
        )
        println(root)
        root.transitiveDependencies.forEach { println(it) }
        println(root.stats)

        assertEquals(Triple(1.5, 3.0, 2.0), root.stats.major.avgDistance)
        assertEquals(Triple(0.0, 1.0, 1.0), root.stats.minor.avgDistance)
        assertEquals(Triple(0.0, 0.0, 2.0), root.stats.patch.avgDistance)


        assertEquals(16.5, root.stats.major.avgLibyears)
        assertEquals(23.5, root.stats.minor.avgLibyears)
        assertEquals(33.5, root.stats.patch.avgLibyears)

        assertEquals(6.0, root.stats.major.avgMissedReleases)
        assertEquals(4.0, root.stats.minor.avgMissedReleases)
        assertEquals(2.0, root.stats.patch.avgMissedReleases)
    }

    @Test
    fun testMinimalArtifactWithMultipleTransitiveDepsWithLagWithIntermediateVersions() {

        val startDate = LocalDateTime(2024, 1, 1, 0, 0).toInstant(TimeZone.of("UTC+3")).toEpochMilliseconds()
        val majorVersionDate = LocalDateTime(2024, 1, 3, 0, 0).toInstant(TimeZone.of("UTC+3")).toEpochMilliseconds()
        val minorVersionDate = LocalDateTime(2024, 1, 9, 0, 0).toInstant(TimeZone.of("UTC+3")).toEpochMilliseconds()
        val patchVersionDate = LocalDateTime(2024, 1, 19, 0, 0).toInstant(TimeZone.of("UTC+3")).toEpochMilliseconds()
        val secondMajorVersionDate =
            LocalDateTime(2024, 2, 1, 0, 0).toInstant(TimeZone.of("UTC+3")).toEpochMilliseconds()
        val secondMinorVersionDate =
            LocalDateTime(2024, 2, 9, 0, 0).toInstant(TimeZone.of("UTC+3")).toEpochMilliseconds()
        val secondPatchVersionDate =
            LocalDateTime(2024, 2, 19, 0, 0).toInstant(TimeZone.of("UTC+3")).toEpochMilliseconds()

        val transitiveDeps = listOf(
            ArtifactDto(
                artifactId = "artifactId",
                groupId = "groupId",
                usedVersion = VersionDto(versionNumber = "1.0.0", releaseDate = startDate),
                allVersions = listOf(
                    VersionDto(versionNumber = "1.0.0", releaseDate = startDate),
                    VersionDto(versionNumber = "1.0.1", releaseDate = 1001010000000L),
                    VersionDto(versionNumber = "1.0.2", releaseDate = patchVersionDate),
                    VersionDto(versionNumber = "1.1.0", releaseDate = 1002020000000L),
                    VersionDto(versionNumber = "1.1.1", releaseDate = minorVersionDate),
                    VersionDto(versionNumber = "2.2.1", releaseDate = 1003030000000L),
                    VersionDto(versionNumber = "2.3.2", releaseDate = majorVersionDate)
                ),
                transitiveDependencies = listOf(),
            ),
            ArtifactDto(
                artifactId = "levelOneArtifactId",
                groupId = "levelOneGroupId",
                usedVersion = VersionDto(versionNumber = "1.0.0", releaseDate = startDate),
                allVersions = listOf(
                    VersionDto(versionNumber = "1.0.0", releaseDate = startDate),
                    VersionDto(versionNumber = "1.0.1", releaseDate = 1002010000000L),
                    VersionDto(versionNumber = "1.0.2", releaseDate = secondPatchVersionDate),
                    VersionDto(versionNumber = "1.1.0", releaseDate = 1003020000000L),
                    VersionDto(versionNumber = "1.1.1", releaseDate = secondMinorVersionDate),
                    VersionDto(versionNumber = "2.2.1", releaseDate = 1004030000000L),
                    VersionDto(versionNumber = "3.3.2", releaseDate = secondMajorVersionDate)
                ),
                transitiveDependencies = listOf(
                    ArtifactDto(
                        artifactId = "levelTwoArtifactId",
                        groupId = "levelTwoGroupId",
                        usedVersion = VersionDto(versionNumber = "1.0.0", releaseDate = startDate),
                        allVersions = listOf(
                            VersionDto(versionNumber = "1.0.0", releaseDate = startDate),
                            VersionDto(versionNumber = "1.0.1", releaseDate = 1001010000000L),
                            VersionDto(versionNumber = "1.0.2", releaseDate = patchVersionDate + 86800000L),
                            VersionDto(versionNumber = "1.1.0", releaseDate = 1002020000000L),
                            VersionDto(versionNumber = "1.1.1", releaseDate = minorVersionDate + 86800000L),
                            VersionDto(versionNumber = "2.2.1", releaseDate = 1003030000000L),
                            VersionDto(versionNumber = "2.3.2", releaseDate = majorVersionDate + 86800000L)
                        ),
                        transitiveDependencies = listOf(),
                    )
                ),
            )
        )

        val root = ArtifactDto(
            artifactId = "rootArtifactId",
            groupId = "rootGroupId",
            usedVersion = VersionDto(versionNumber = "1.0.0", releaseDate = 2L),
            allVersions = listOf(VersionDto(versionNumber = "1.0.0", releaseDate = 2L)),
            transitiveDependencies = transitiveDeps
        )
        println(root)
        root.transitiveDependencies.forEach { println(it) }
        println(root.stats)

        assertEquals(Triple((1 + 1 + 2).toDouble() / 3, 3.0, 2.0), root.stats.major.avgDistance)
        assertEquals(Triple(0.0, 1.0, 1.0), root.stats.minor.avgDistance)
        assertEquals(Triple(0.0, 0.0, 2.0), root.stats.patch.avgDistance)


        assertEquals(12.0, root.stats.major.avgLibyears)
        assertEquals(23.5, root.stats.minor.avgLibyears)
        assertEquals(33.5, root.stats.patch.avgLibyears)

        assertEquals(6.0, root.stats.major.avgMissedReleases)
        assertEquals(4.0, root.stats.minor.avgMissedReleases)
        assertEquals(2.0, root.stats.patch.avgMissedReleases)
    }
}
