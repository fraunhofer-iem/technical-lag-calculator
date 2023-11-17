package artifact

import artifact.db.Artifact
import artifact.db.Artifacts
import artifact.db.Version
import artifact.model.ArtifactDto
import artifact.model.MetadataDto
import artifact.model.VersionDto
import org.jetbrains.exposed.dao.with
import org.jetbrains.exposed.sql.and
import util.dbQuery

class ArtifactService {

    suspend fun getArtifactFromMetadata(metadataDto: MetadataDto, storeResult: Boolean = false): ArtifactDto {
        return if (storeResult) {
            getOrCreateArtifact(metadataDto)
        } else {
            metadataToArtifact(metadataDto)
        }
    }

    suspend fun updateVersions(versions: List<VersionDto>, artifactDto: ArtifactDto) = dbQuery {
        versions.forEach { versionWithoutReleaseDate ->
            val releaseDate = artifactDto.versions.find {
                it.versionNumber == versionWithoutReleaseDate.versionNumber
            }

            releaseDate?.releaseDate?.let {
                val version = Version.findById(versionWithoutReleaseDate.dbId)
                version?.releaseDate = it
            }
        }
    }

    private suspend fun getOrCreateArtifact(mavenMetadata: MetadataDto): ArtifactDto = dbQuery {

        val artifactQuery = Artifact.find {
            Artifacts.artifactId eq mavenMetadata.artifactId and (Artifacts.groupId eq mavenMetadata.groupId)
        }.with(Artifact::versions)

        val artifactModel = if (artifactQuery.empty()) {
            Artifact.new {
                artifactId = mavenMetadata.artifactId
                groupId = mavenMetadata.groupId
            }
        } else {
            if (artifactQuery.count() > 1) {
                println("WARNING: More than one matching artifact found!")
            }
            artifactQuery.first()
        }

        mavenMetadata.versions.forEach { currentVersion ->
            if (artifactModel.versions.find { it.versionNumber == currentVersion } == null) {
                println("Creating new version $currentVersion")
                Version.new {
                    versionNumber = currentVersion
                    artifact = artifactModel
                }
            } else {
                println("Version already exists.")
            }
        }


        return@dbQuery ArtifactDto(
            dbId = artifactModel.id.value,
            artifactId = mavenMetadata.artifactId,
            groupId = mavenMetadata.groupId,
            versions = artifactModel.versions.map {
                VersionDto(
                    versionNumber = it.versionNumber,
                    releaseDate = it.releaseDate,
                    dbId = it.id.value
                )
            }
        )
    }

    private fun metadataToArtifact(metadataDto: MetadataDto): ArtifactDto {
        return ArtifactDto(
            artifactId = metadataDto.artifactId,
            groupId = metadataDto.groupId,
            versions = metadataDto.versions.map {
                VersionDto(versionNumber = it)
            }
        )
    }
}
