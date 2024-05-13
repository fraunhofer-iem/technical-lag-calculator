package artifact.model

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

        println(artifact.stats)
        println(artifact.technicalLag)

    }


}