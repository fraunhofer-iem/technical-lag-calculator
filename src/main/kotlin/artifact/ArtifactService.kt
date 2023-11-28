package artifact

import artifact.db.Artifact
import artifact.db.Artifacts
import artifact.db.Version
import artifact.model.ArtifactDto
import artifact.model.CreateArtifactDto
import artifact.model.MetadataDto
import artifact.model.VersionDto

import http.maven.MavenClient
import org.jetbrains.exposed.dao.with
import org.jetbrains.exposed.sql.and
import org.ossreviewtoolkit.model.PackageReference
import util.dbQuery

class ArtifactService(private val storeResults: Boolean = false) {

    private val mavenClient = MavenClient()

    suspend fun getAllTransitiveVersionInformation(
        rootPackage: PackageReference,
    ): List<ArtifactDto> {
        val infoList: MutableList<CreateArtifactDto> = mutableListOf()

        getDependencyVersionInformation(
            packageRef = rootPackage,
            infoList = infoList,
            isTransitiveDependency = false
        )

        return infoList.map { it.toArtifactDto() }
    }

    private suspend fun getDependencyVersionInformation(
        packageRef: PackageReference,
        infoList: MutableList<CreateArtifactDto>,
        isTransitiveDependency: Boolean = true,
    ) {

        mavenClient.getAllVersionsFromRepo(
            namespace = packageRef.id.namespace,
            name = packageRef.id.name
        )?.let { metadataDto ->

            val createArtifactDto = metadataToArtifact(metadataDto)
            createArtifactDto.artifactId = packageRef.id.name
            createArtifactDto.groupId = packageRef.id.namespace
            createArtifactDto.usedVersion = packageRef.id.version
            createArtifactDto.isTopLevelDependency = !isTransitiveDependency

            if (this.storeResults) {
                val storedVersions = getVersionsForArtifact(
                    namespace = packageRef.id.namespace,
                    name = packageRef.id.name
                )

                if(metadataDto.versions.count() == storedVersions.count()) {
                    createArtifactDto.addVersions(storedVersions)
                }
            }

            val versionsWithoutReleaseDate = createArtifactDto.versions.values.filter { it.releaseDate == -1L }

            // If there are any versions without a proper release date we replace the complete database model
            // with information gathered from the API.
            if (versionsWithoutReleaseDate.isNotEmpty()) {
                createArtifactDto.versions = mavenClient.getVersionsFromSearch(
                    namespace = packageRef.id.namespace,
                    name = packageRef.id.name
                ).associateBy { version -> version.versionNumber }.toMutableMap()
            }

            packageRef.dependencies.forEach {
                getDependencyVersionInformation(
                    packageRef = it,
                    infoList = createArtifactDto.transitiveDependencies
                )
            }

            infoList.add(createArtifactDto)
        }
    }

    private fun metadataToArtifact(metadataDto: MetadataDto): CreateArtifactDto {
        val versions = metadataDto.versions.associateWith { version ->
            VersionDto(versionNumber = version)
        }.toMutableMap()

        return CreateArtifactDto(
            artifactId = metadataDto.artifactId,
            groupId = metadataDto.groupId,
            versions = versions
        )
    }

    private suspend fun getVersionsForArtifact(name: String, namespace: String): List<VersionDto> = dbQuery {
        val artifactQuery = Artifact.find {
            Artifacts.artifactId eq name and (Artifacts.groupId eq namespace)
        }.with(Artifact::versions)

        return@dbQuery if (!artifactQuery.empty()) {
            artifactQuery.first().versions.map {
                VersionDto(
                    versionNumber = it.versionNumber,
                    releaseDate = it.releaseDate
                )
            }
        } else {
            emptyList()
        }
    }
}
