package dependencies.model

import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toInstant
import org.junit.jupiter.api.Test
import technicalLag.model.TechnicalLagDto
import kotlin.test.assertEquals

class ArtifactTest {

    @Test
    fun getTechLagForVersion() {

        val usedVersionDate = LocalDateTime(2024, 1, 1, 0, 0).toInstant(TimeZone.of("UTC+3")).toEpochMilliseconds()
        val patchVersionDate = LocalDateTime(2024, 1, 3, 0, 0).toInstant(TimeZone.of("UTC+3")).toEpochMilliseconds()
        val minorVersionDate = LocalDateTime(2024, 1, 9, 0, 0).toInstant(TimeZone.of("UTC+3")).toEpochMilliseconds()
        val majorVersionDate = LocalDateTime(2024, 1, 19, 0, 0).toInstant(TimeZone.of("UTC+3")).toEpochMilliseconds()

        val versions = listOf(
            ArtifactVersion.create(versionNumber = "3.11", releaseDate = usedVersionDate),
            ArtifactVersion.create(versionNumber = "3.11.3", releaseDate = patchVersionDate),
            ArtifactVersion.create(versionNumber = "3.12", releaseDate = 0L),
            ArtifactVersion.create(versionNumber = "3.12.3", releaseDate = minorVersionDate),
            ArtifactVersion.create(versionNumber = "4.12.3", releaseDate = majorVersionDate),
        )

        val artifact = Artifact(artifactId = "artifactId", groupId = "groupId", versions = versions)

        println(artifact.getTechLagForVersion(rawVersion = "3.11", versionType = VersionType.Major))
        println(artifact.getTechLagForVersion(rawVersion = "3.11", versionType = VersionType.Minor))
        println(artifact.getTechLagForVersion(rawVersion = "3.11", versionType = VersionType.Patch))
    }

    @Test
    fun getTechLagForBetaVersion() {

        val usedVersionDate = LocalDateTime(2024, 1, 1, 0, 0).toInstant(TimeZone.of("UTC+3")).toEpochMilliseconds()
        val patchVersionDate = LocalDateTime(2024, 1, 3, 0, 0).toInstant(TimeZone.of("UTC+3")).toEpochMilliseconds()
        val minorVersionDate = LocalDateTime(2024, 1, 9, 0, 0).toInstant(TimeZone.of("UTC+3")).toEpochMilliseconds()
        val majorVersionDate = LocalDateTime(2024, 1, 19, 0, 0).toInstant(TimeZone.of("UTC+3")).toEpochMilliseconds()

        // TODO: add beta releases like 2.0.0-next.0 is not newer than 2.0.0-next.5
        val versions = listOf(
            ArtifactVersion.create(versionNumber = "3.10.0", releaseDate = usedVersionDate),
            ArtifactVersion.create(versionNumber = "3.11.3-next.0", releaseDate = usedVersionDate),
            ArtifactVersion.create(versionNumber = "3.11.3-next.5", releaseDate = patchVersionDate),
            ArtifactVersion.create(versionNumber = "3.12", releaseDate = 0L),
            ArtifactVersion.create(versionNumber = "3.12.3", releaseDate = minorVersionDate),
            ArtifactVersion.create(versionNumber = "4.12.3", releaseDate = majorVersionDate),
        )

        val artifact = Artifact(artifactId = "artifactId", groupId = "groupId", versions = versions)

        val actualPatch = artifact.getTechLagForVersion(rawVersion = "3.11.3-next.0", versionType = VersionType.Patch)
        val expectedPatch = TechnicalLagDto(
            libDays = 2,
            distance = Triple(0, 0, 1),
            version = "3.11.3-next.5",
            numberOfMissedReleases = 1
        )
        assertEquals(expectedPatch, actualPatch)

        val actualMinor = artifact.getTechLagForVersion(rawVersion = "3.11.3-next.0", versionType = VersionType.Minor)
        val expectedMinor = TechnicalLagDto(
            libDays = 8,
            distance = Triple(0, 1, 1),
            version = "3.12.3",
            numberOfMissedReleases = 3
        )
        assertEquals(expectedMinor, actualMinor)

        val actualMajor = artifact.getTechLagForVersion(rawVersion = "3.11.3-next.0", versionType = VersionType.Major)
        val expectedMajor = TechnicalLagDto(
            libDays = 18,
            distance = Triple(1, 0, 0),
            version = "4.12.3",
            numberOfMissedReleases = 4
        )
        assertEquals(expectedMajor, actualMajor)

        val actualPatchStable = artifact.getTechLagForVersion(rawVersion = "3.10.0", versionType = VersionType.Patch)
        val expectedPatchStable = TechnicalLagDto(
            libDays = 0,
            distance = Triple(0, 0, 0),
            version = "3.10.0",
            numberOfMissedReleases = 0
        )
        assertEquals(expectedPatchStable, actualPatchStable)

        val actualMinorStable = artifact.getTechLagForVersion(rawVersion = "3.10.0", versionType = VersionType.Minor)
        val expectedMinorStable = TechnicalLagDto(
            libDays = 8,
            distance = Triple(0, 1, 1),
            version = "3.12.3",
            numberOfMissedReleases = 2
        )
        assertEquals(expectedMinorStable, actualMinorStable)

        val actualMajorStable = artifact.getTechLagForVersion(rawVersion = "3.10.0", versionType = VersionType.Major)
        val expectedMajorStable = TechnicalLagDto(
            libDays = 18,
            distance = Triple(1, 0, 0),
            version = "4.12.3",
            numberOfMissedReleases = 3
        )
        assertEquals(expectedMajorStable, actualMajorStable)
    }
}