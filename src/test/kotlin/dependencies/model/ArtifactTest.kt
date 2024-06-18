package dependencies.model

import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toInstant
import org.junit.jupiter.api.Test

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
            ArtifactVersion.create(versionNumber = "3.11.3-next.0", releaseDate = usedVersionDate),
            ArtifactVersion.create(versionNumber = "3.11.3-next.5", releaseDate = patchVersionDate),
            ArtifactVersion.create(versionNumber = "3.12", releaseDate = 0L),
            ArtifactVersion.create(versionNumber = "3.12.3", releaseDate = minorVersionDate),
            ArtifactVersion.create(versionNumber = "4.12.3", releaseDate = majorVersionDate),
        )

        val artifact = Artifact(artifactId = "artifactId", groupId = "groupId", versions = versions)

//        println(artifact.getTechLagForVersion(rawVersion = "3.11.3-next.0", versionType = VersionType.Major))
//        println(artifact.getTechLagForVersion(rawVersion = "3.11.3-next.0", versionType = VersionType.Minor))
        println(artifact.getTechLagForVersion(rawVersion = "3.11.3-next.0", versionType = VersionType.Patch))
    }
}