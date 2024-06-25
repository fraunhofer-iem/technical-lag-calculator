package artifact

import shared.project.artifact.Artifact
import shared.project.artifact.ArtifactVersion
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import org.junit.jupiter.api.Test
import util.TimeHelper.dateToMs
import kotlin.io.path.Path

@Serializable
data class VersionDto(val releaseDate: String, val version: String)

class ReleaseFrequencyDtoTest {

    @Test
    fun getReleaseFrequency() {
        val versions = Json.decodeFromString<Array<VersionDto>>(
            Path("src/test/resources/githubReleaseVersions.json").toFile().readText()
        ).map {
            ArtifactVersion.create(
                versionNumber = it.version,
                releaseDate = dateToMs(it.releaseDate),

                )
        }

        val artifact = Artifact(versions = versions, artifactId = "horizon", groupId = "laravel")

        println(artifact.releaseFrequency)

    }
}