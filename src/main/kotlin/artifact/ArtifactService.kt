package artifact

import artifact.db.Artifact
import artifact.db.Artifacts
import artifact.db.Version
import artifact.model.ArtifactDto
import artifact.model.DependencyMetadataDto
import artifact.model.MetadataDto
import artifact.model.VersionDto
import http.maven.MavenClient
import org.jetbrains.exposed.dao.with
import org.jetbrains.exposed.sql.and
import org.ossreviewtoolkit.model.PackageReference
import util.dbQuery

class ArtifactService {

    private val mavenClient = MavenClient()

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

    suspend fun getAllTransitiveVersionInformation(
        rootPackage: PackageReference,
        scope: String,
        storeResult: Boolean = false)
    : List<Pair<DependencyMetadataDto, ArtifactDto>> {
        val infoList: MutableList<Pair<DependencyMetadataDto, ArtifactDto>> = mutableListOf()

        getDependencyVersionInformation(
            packageRef = rootPackage,
            scope = scope,
            storeResult = storeResult,
            infoList = infoList,
            isTransitiveDependency = false
        )

        return infoList
    }

    suspend fun getDependencyVersionInformation(
        packageRef: PackageReference,
        scope: String,
        infoList: MutableList<Pair<DependencyMetadataDto, ArtifactDto>>,
        storeResult: Boolean = false,
        isTransitiveDependency:Boolean = true,
    ) {

        mavenClient.getAllVersionsFromRepo(
            namespace = packageRef.id.namespace,
            name = packageRef.id.name
        )?.let { metadataDto ->

            val dependencyMetadata = DependencyMetadataDto(
                scope = scope,
                isTransitiveDependency = isTransitiveDependency,
                usedVersion = packageRef.id.version
            )

            val artifact = getArtifactFromMetadata(
                metadataDto,
                storeResult = storeResult
            )
            println("get artifact done $artifact")
            val versionsWithoutReleaseDate = artifact.versions.filter { it.releaseDate == -1L }

            if (versionsWithoutReleaseDate.isNotEmpty()) {
                val artifactDto = mavenClient.getVersionsFromSearch(
                    namespace = packageRef.id.namespace,
                    name = packageRef.id.name
                )

                if (storeResult) {
                    updateVersions(
                        artifactDto = artifactDto,
                        versions = versionsWithoutReleaseDate
                    )
                }
                infoList.add(Pair(dependencyMetadata, artifactDto))
            } else {
                infoList.add(Pair(dependencyMetadata, artifact))
            }
        }

        packageRef.dependencies.forEach {
            getDependencyVersionInformation(
                packageRef = it,
                scope = scope,
                storeResult = storeResult,
                infoList = infoList
            )
        }
    }

    private suspend fun getOrCreateArtifact(metadataDto: MetadataDto): ArtifactDto = dbQuery {

        val artifactQuery = Artifact.find {
            Artifacts.artifactId eq metadataDto.artifactId and (Artifacts.groupId eq metadataDto.groupId)
        }.with(Artifact::versions)

        val artifactModel = if (artifactQuery.empty()) {
            Artifact.new {
                artifactId = metadataDto.artifactId
                groupId = metadataDto.groupId
            }
        } else {
            if (artifactQuery.count() > 1) {
                println("WARNING: More than one matching artifact found!")
            }
            artifactQuery.first()
        }

        metadataDto.versions.forEach { currentVersion ->
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
            artifactId = metadataDto.artifactId,
            groupId = metadataDto.groupId,
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
