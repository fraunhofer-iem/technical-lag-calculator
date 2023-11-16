import dependencies.DependencyAnalyzer
import dependencies.model.Artifact
import dependencies.model.Artifacts
import dependencies.model.Version
import dependencies.model.Versions
import http.maven.MavenClient
import http.model.MetadataDto
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import libyears.LibyearCalculator
import org.jetbrains.exposed.dao.with
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.transactions.experimental.newSuspendedTransaction
import org.jetbrains.exposed.sql.transactions.transaction
import java.nio.file.Paths

fun main(args: Array<String>) = runBlocking {

    val projectPath = Paths.get(args[0]).toFile()
    val dependencyAnalyzer = DependencyAnalyzer()
    val dependencyGraphs = dependencyAnalyzer.getDependencyPackagesForProject(projectPath)
    val storeResultsInDb = true
    Database.connect("jdbc:sqlite:identifier.sqlite", "org.sqlite.JDBC")

    transaction {
        addLogger(StdOutSqlLogger)
        SchemaUtils.create(Artifacts, Versions)
    }
    val mavenClient = MavenClient()

    val allPackages = dependencyGraphs.values.map { it.packages }.flatten()

    val artifacts: List<Pair<String, ArtifactDto>> = allPackages.mapNotNull { pkg ->
        mavenClient.getAllVersionsFromRepo(namespace = pkg.namespace, name = pkg.name)
            ?.let { mavenMetadata ->
                val artifact = getArtifactFromMetadata(mavenMetadata, storeResult = storeResultsInDb)
                println("get artifact done $artifact")
                val versionsWithoutReleaseDate = artifact.versions.filter { it.releaseDate == -1L }

                return@mapNotNull if (versionsWithoutReleaseDate.isNotEmpty()) {
                    val artifactDto = mavenClient.getVersionsFromSearch(namespace = pkg.namespace, name = pkg.name)
                    if(storeResultsInDb) {
                        updateVersions(artifactDto = artifactDto, versions = versionsWithoutReleaseDate)
                    }
                    Pair(pkg.version, artifactDto)
                } else {
                    Pair(pkg.version, artifact)
                }
            }
        null
    }

    val libyearCalculator = LibyearCalculator()
    val libyears = artifacts.sumOf { libyearCalculator.calculateDifferenceForPackage(it.first, it.second.versions) }

    println("Days behind: $libyears, Years behind: ${libyears / 365.25}")
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
suspend fun getArtifactFromMetadata(mavenMetadata: MetadataDto, storeResult: Boolean = false): ArtifactDto {
    return if(storeResult) {
        getOrCreateArtifact(mavenMetadata)
    } else {
        metadataToArtifact(mavenMetadata)
    }
}

fun metadataToArtifact(mavenMetadata: MetadataDto): ArtifactDto {
        return ArtifactDto(
            artifactId = mavenMetadata.artifactId,
            groupId = mavenMetadata.groupId,
            versions = mavenMetadata.versions.map {
                VersionDto(versionNumber = it)
            }
        )
}


suspend fun <T> dbQuery(block: suspend () -> T): T =
    newSuspendedTransaction(Dispatchers.IO) { block() }

suspend fun getOrCreateArtifact(mavenMetadata: MetadataDto): ArtifactDto = dbQuery {

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

data class ArtifactDto(val dbId: Int = -1, val artifactId: String, val groupId: String, val versions: List<VersionDto>)
data class VersionDto(val dbId: Int = -1, val versionNumber: String, val releaseDate: Long = -1)
