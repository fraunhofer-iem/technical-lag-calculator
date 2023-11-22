import artifact.ArtifactService
import artifact.model.ArtifactDto
import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.parameters.options.option
import com.github.ajalt.clikt.parameters.options.required
import com.github.ajalt.clikt.parameters.types.path
import dependencies.DependencyAnalyzer
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

    val artifactService = ArtifactService()

    // package manager -> scope -> directDependency ->-> transitiveDependencies
    val transformedGraph = dependencyGraphs.map { (packageManager, graph) ->
        val transformedScope = graph.createScopes().associate { scope ->

            val transformedDependencies = scope.dependencies.flatMap { packageRef ->
                artifactService.getAllTransitiveVersionInformation(
                    rootPackage = packageRef,
                    storeResult = !dbUrl.isNullOrBlank()
                )
            }

            scope.name to transformedDependencies
        }
        packageManager to transformedScope
    }.toMap()

    transformedGraph.forEach { (packageManager, scopes) ->
        println("Libyears for $packageManager")
        scopes.forEach { (scope, artifacts) ->
            println("Libyears in scope $scope")
            val directDependencies = artifacts.sumOf { it.libyear }
            println(
                "Direct dependency libyears: $directDependencies Days " +
                        "(equals to roughly ${directDependencies / 365.25} years)"
            )

            // Here we loose the
            val transitiveDependencySum = artifacts.sumOf {
                it.transitiveDependencies.sumOf { transitive -> calculateTransitiveLibyears(transitive) }
            }
            println(
                "Transitive dependency libyears: $transitiveDependencySum Days " +
                        "(equals to roughly ${transitiveDependencySum / 365.25} years)"
            )
        }
    }

    println("Warnings for dependencies older than 180 days:")
    transformedGraph.values.forEach {
        it.values.forEach {
            it.forEach { artifact ->
                printLibyearWarning(artifact)
            }
        }
    }

    if (!dbUrl.isNullOrBlank()) {
        //TODO store

    }
}

fun printLibyearWarning(artifact: ArtifactDto) {
    if (artifact.libyear < -180) {
        println(
            "Dependency ${artifact.groupId}/${artifact.artifactId}" +
                    "is ${artifact.libyear} days old."
        )
        val newestVersion = artifact.versions.maxByOrNull { it.releaseDate }
        println(
            "The used version is ${artifact.usedVersion} and " +
                    "the newest version ${newestVersion?.versionNumber}"
        )
    }
    artifact.transitiveDependencies.forEach { printLibyearWarning(it) }
}

fun calculateTransitiveLibyears(artifact: ArtifactDto): Long {
    var sumLibyears = artifact.libyear

    for (dependency in artifact.transitiveDependencies) {
        sumLibyears += calculateTransitiveLibyears(dependency)
    }

    return sumLibyears
}
