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

class ArtifactService {

    private val mavenClient = MavenClient()

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

    suspend fun getAllTransitiveVersionInformation(
        rootPackage: PackageReference,
        storeResult: Boolean = false
    ): List<ArtifactDto> {
        val infoList: MutableList<CreateArtifactDto> = mutableListOf()

        getDependencyVersionInformation(
            packageRef = rootPackage,
            storeResult = storeResult,
            infoList = infoList,
            isTransitiveDependency = false
        )

        return infoList.map { it.toArtifactDto() }
    }

    private suspend fun getDependencyVersionInformation(
        packageRef: PackageReference,
        infoList: MutableList<CreateArtifactDto>,
        storeResult: Boolean = false,
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

            if (storeResult) {
                val storedVersions = getVersionsForArtifact(
                    namespace = packageRef.id.namespace,
                    name = packageRef.id.name
                )

                createArtifactDto.addVersions(storedVersions)
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
                    storeResult = storeResult,
                    infoList = createArtifactDto.transitiveDependencies
                )
            }

            infoList.add(createArtifactDto)
        }
    }

    //    private suspend fun getOrCreateArtifact(metadataDto: MetadataDto): ArtifactDto = dbQuery {
//
//        val artifactQuery = Artifact.find {
//            Artifacts.artifactId eq metadataDto.artifactId and (Artifacts.groupId eq metadataDto.groupId)
//        }.with(Artifact::versions)
//
//        val artifactModel = if (artifactQuery.empty()) {
//            Artifact.new {
//                artifactId = metadataDto.artifactId
//                groupId = metadataDto.groupId
//            }
//        } else {
//            if (artifactQuery.count() > 1) {
//                println("WARNING: More than one matching artifact found!")
//            }
//            artifactQuery.first()
//        }
//
//        metadataDto.versions.forEach { currentVersion ->
//            if (artifactModel.versions.find { it.versionNumber == currentVersion } == null) {
//                println("Creating new version $currentVersion")
//                Version.new {
//                    versionNumber = currentVersion
//                    artifact = artifactModel
//                }
//            } else {
//                println("Version already exists.")
//            }
//        }
//
//
//        return@dbQuery ArtifactDto(
//            dbId = artifactModel.id.value,
//            artifactId = metadataDto.artifactId,
//            groupId = metadataDto.groupId,
//            versions = artifactModel.versions.map {
//                VersionDto(
//                    versionNumber = it.versionNumber,
//                    releaseDate = it.releaseDate,
//                    dbId = it.id.value
//                )
//            }
//        )
//    }
//
    private fun metadataToArtifact(metadataDto: MetadataDto): CreateArtifactDto {
        val versions = metadataDto.versions.associateWith {
            version -> VersionDto(versionNumber = version)
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
                    releaseDate = it.releaseDate,
                    dbId = it.id.value
                )
            }
        } else {
            emptyList()
        }
    }
}
