import artifact.ArtifactService
import artifact.model.ArtifactDto
import dependencies.DependencyAnalyzer
import http.maven.MavenClient
import kotlinx.coroutines.runBlocking
import libyears.LibyearCalculator
import util.initDatabase
import java.nio.file.Paths

fun main(args: Array<String>) = runBlocking {

    //TODO: Those variables will be command line parameters
    val storeResultsInDb = true
    val dbPath = "jdbc:sqlite:identifier.sqlite"
    val projectPath = Paths.get(args[0]).toFile()

    if (storeResultsInDb) {
        initDatabase(dbPath)
    }


    val dependencyAnalyzer = DependencyAnalyzer()
    val dependencyGraphs = dependencyAnalyzer.getDependencyPackagesForProject(projectPath)

    val allPackages = dependencyGraphs.values.map { it.packages }.flatten()

    val artifactService = ArtifactService()
    val mavenClient = MavenClient()

    val artifacts: List<Pair<String, ArtifactDto>> = allPackages.mapNotNull { pkg ->
        mavenClient.getAllVersionsFromRepo(
            namespace = pkg.namespace,
            name = pkg.name
        )?.let { mavenMetadata ->
            val artifact = artifactService.getArtifactFromMetadata(mavenMetadata, storeResult = storeResultsInDb)
            println("get artifact done $artifact")
            val versionsWithoutReleaseDate = artifact.versions.filter { it.releaseDate == -1L }

            return@mapNotNull if (versionsWithoutReleaseDate.isNotEmpty()) {
                val artifactDto = mavenClient.getVersionsFromSearch(namespace = pkg.namespace, name = pkg.name)
                if (storeResultsInDb) {
                    artifactService.updateVersions(artifactDto = artifactDto, versions = versionsWithoutReleaseDate)
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
