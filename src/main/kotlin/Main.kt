import artifact.ArtifactService
import artifact.model.ArtifactDto
import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.parameters.options.option
import com.github.ajalt.clikt.parameters.options.required
import com.github.ajalt.clikt.parameters.types.path
import dependencies.DependencyAnalyzer
import http.maven.MavenClient
import libyears.LibyearCalculator
import org.ossreviewtoolkit.model.PackageReference
import util.initDatabase
import java.io.File


class Libyears : CliktCommand() {
    val dbUrl by option(
        envvar = "DB_URL", help = "Optional database path to store version numbers and their release dates. " +
                "Expected format: jdbc:sqlite:identifier.sqlite"
    )
    val projectPath by option(envvar = "PROJECT_PATH", help = "Path to the analyzed project's root.")
        .path(mustExist = true, mustBeReadable = true, canBeFile = false)
        .required()

    override fun run() {
        echo("Running libyears for project at $projectPath and optional db URL $dbUrl")
        isValidSQLiteUrl(dbUrl)
    }

    private fun isValidSQLiteUrl(url: String?): Boolean {
//        // as the db url is an optional setting it is valid to have a null value here
//        if (url == null) {
//            return true
//        }
//        return try {
//            val parsedUrl = java.net.URL(url)
//            println(parsedUrl)
//            parsedUrl.protocol == "jdbc" &&
//                    parsedUrl.path.matches(Regex("^/([a-zA-Z0-9_]+\\.sqlite)$"))
//        } catch (e: Exception) {
//            throw Exception("Given db URL is invalid due to exception $e")
//        }
        return true
    }
}

suspend fun main(args: Array<String>) {
    val libyearCommand = Libyears()
    libyearCommand.main(args)
    getLibYears(projectPath = libyearCommand.projectPath.toFile(), dbUrl = libyearCommand.dbUrl)
}





suspend fun getLibYears(projectPath: File, dbUrl: String?) {
    if (!dbUrl.isNullOrBlank()) {
        initDatabase(dbUrl)
    }

    val dependencyAnalyzer = DependencyAnalyzer()
    val dependencyGraphs = dependencyAnalyzer.getDependencyPackagesForProject(projectPath)
    // TODO: we want to get a map from scope to a pair<directDependencies, transitiveDependencies>
    // TODO: we need to identify the different scopes for different package managers (create enums)
    val allPackages = dependencyGraphs.values.map { it.packages }.flatten()

    val artifactService = ArtifactService()
    val mavenClient = MavenClient()

    dependencyGraphs.map { (packageManager, graph) ->
        graph.createScopes().map { scope ->
            val transformedDependencies = scope.dependencies.map { packageRef ->
                artifactService.getAllTransitiveVersionInformation(
                    rootPackage = packageRef,
                    scope = scope.name,
                    storeResult = !dbUrl.isNullOrBlank()
                )
            }

            println("here I am")
            println(transformedDependencies)
        }
    }




    // List of used version of all artifacts mapped to an object storing all versions and their release date
    // of the same artifact.
    val artifacts: List<Pair<String, ArtifactDto>> = allPackages.mapIndexedNotNull { idx, pkg ->
        mavenClient.getAllVersionsFromRepo(
            namespace = pkg.namespace,
            name = pkg.name
        )?.let { mavenMetadata ->
            val artifact = artifactService.getArtifactFromMetadata(mavenMetadata, storeResult = !dbUrl.isNullOrBlank())
            println("get artifact done $artifact")
            val versionsWithoutReleaseDate = artifact.versions.filter { it.releaseDate == -1L }

            return@mapIndexedNotNull if (versionsWithoutReleaseDate.isNotEmpty()) {
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
