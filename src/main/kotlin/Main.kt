import artifact.ArtifactService
import artifact.model.ArtifactDto
import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.parameters.options.*
import dependencies.DependencyAnalyzer
import http.maven.MavenClient
import kotlinx.coroutines.runBlocking
import libyears.LibyearCalculator
import util.initDatabase
import java.io.File
import java.nio.file.Paths

class Libyears : CliktCommand() {
    val dbUrl by option(help="Optional database path to store version numbers and their release dates. " +
            "Expected format: jdbc:sqlite:identifier.sqlite")
    val projectPath by option(help="Path to the analyzed project's root.").required()

    override fun run() {
        echo("Running libyears for project at $projectPath and optional db URL $dbUrl")
        if (isValidSQLiteUrl(dbUrl) && isValidDirectoryPath(projectPath)) {
            getLibYears(Paths.get(projectPath).toFile(), dbUrl)
        } else {
            echo("Program start failed due to invalid db URL or project path.")
        }
    }

    private fun isValidDirectoryPath(path: String): Boolean {
        return try {
            val directory = File(path)
            directory.isDirectory
        } catch (e: Exception) {
            echo("Given directory path is invalid due to exception $e")
            false
        }
    }

    private fun isValidSQLiteUrl(url: String?): Boolean {
        // as the db url is an optional setting it is valid to have a null value here
        if(url == null) {
            return true
        }
        return try {
            val parsedUrl = java.net.URL(url)
            parsedUrl.protocol == "jdbc" &&
                    parsedUrl.path.matches(Regex("^/([a-zA-Z0-9_]+\\.sqlite)$"))
        } catch (e: Exception) {
            echo("Given db URL is invalid due to exception $e")
            false
        }
    }
}

fun main(args: Array<String>) = Libyears().main(args)

fun getLibYears(projectPath: File, dbUrl: String?) = runBlocking {
    if (!dbUrl.isNullOrBlank()) {
        initDatabase(dbUrl)
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
            val artifact = artifactService.getArtifactFromMetadata(mavenMetadata, storeResult = !dbUrl.isNullOrBlank())
            println("get artifact done $artifact")
            val versionsWithoutReleaseDate = artifact.versions.filter { it.releaseDate == -1L }

            return@mapNotNull if (versionsWithoutReleaseDate.isNotEmpty()) {
                val artifactDto = mavenClient.getVersionsFromSearch(namespace = pkg.namespace, name = pkg.name)
                if (!dbUrl.isNullOrBlank()) {
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
