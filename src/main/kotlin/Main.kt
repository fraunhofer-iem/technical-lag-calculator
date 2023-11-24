import artifact.ArtifactService
import artifact.db.Artifact
import artifact.db.Artifacts
import artifact.db.Version
import artifact.model.ArtifactDto
import artifact.model.DependencyGraphDto
import artifact.model.ScopedDependencyDto
import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.parameters.options.option
import com.github.ajalt.clikt.parameters.options.required
import com.github.ajalt.clikt.parameters.types.path
import dependencies.DependencyAnalyzer
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import libyears.LibyearCalculator
import org.jetbrains.exposed.dao.with
import org.jetbrains.exposed.sql.and
import util.dbQuery
import util.initDatabase
import java.io.File
import java.nio.file.Path
import java.util.*
import kotlin.io.path.createDirectories


class Libyears : CliktCommand() {
    val dbUrl by option(
        envvar = "DB_URL", help = "Optional path to store a file based database which contains" +
                " version numbers and their release dates." +
                "This database is used as a cache and the application works seamlessly without it." +
                "If the path doesn't exist it will be created."
    ).path(mustExist = false, mustBeReadable = true, mustBeWritable = true, canBeFile = false)

    val projectPath by option(envvar = "PROJECT_PATH", help = "Path to the analyzed project's root.")
        .path(mustExist = true, mustBeReadable = true, canBeFile = false)
        .required()

    val outputPath by option(
        envvar = "OUTPUT_PATH", help = "Path to the folder to store the JSON results" +
                "of the created dependency graph. If the path doesn't exist it will be created."
    )
        .path(mustExist = false, canBeFile = false)

    override fun run() {
        echo("Running libyears for project at $projectPath and output path $outputPath" +
                " and optional db URL $dbUrl")
        dbUrl?.createDirectories()
        outputPath?.createDirectories()
    }
}

suspend fun main(args: Array<String>) {
    val libyearCommand = Libyears()
    libyearCommand.main(args)

    val dbString = if (libyearCommand.dbUrl != null) {
        "jdbc:sqlite:${libyearCommand.dbUrl.toString()}/versionsCache.sqlite"
    } else {
        null
    }

    getLibYears(
        projectPath = libyearCommand.projectPath.toFile(),
        outputPath = libyearCommand.outputPath,
        dbUrl = dbString
    )
}


suspend fun getLibYears(projectPath: File, outputPath: Path?, dbUrl: String?) {
    if (!dbUrl.isNullOrBlank()) {
        initDatabase(dbUrl)
    }

    val dependencyAnalyzer = DependencyAnalyzer()
    val dependencyGraphs = dependencyAnalyzer.getDependencyPackagesForProject(projectPath)

    val artifactService = ArtifactService()

    //TODO: move this to a better places
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

        packageManager to ScopedDependencyDto(transformedScope)
    }.toMap()

    val dependencyGraphDto = DependencyGraphDto(transformedGraph)

    val libyearCalculator = LibyearCalculator()
    libyearCalculator.printDependencyGraph(dependencyGraphDto)


    if (outputPath != null) {
        val outputFile = outputPath.resolve("${Date().time}-graphResult.json").toFile()
        withContext(Dispatchers.IO) {
            outputFile.createNewFile()
            val json = Json { prettyPrint = false }
            val jsonString = json.encodeToString(DependencyGraphDto.serializer(), dependencyGraphDto)
            outputFile.writeText(jsonString)
        }
    }


    if (!dbUrl.isNullOrBlank()) {
        dependencyGraphDto.packageManagerToScopes.values.forEach {
            it.scopesToDependencies.values.forEach {
                it.forEach { artifact ->
                    recursivelyUpdateCache(artifact)
                }
            }
        }
    }
}

suspend fun recursivelyUpdateCache(artifactDto: ArtifactDto) {
    dbQuery {
        val artifacts = Artifact.find {
            Artifacts.artifactId eq artifactDto.artifactId and (Artifacts.groupId eq artifactDto.groupId)
        }.with(Artifact::versions)

        if (artifacts.count() > 1) {
            println(
                "The cache should only contain a single entry for every groupId and " +
                        "artifact id combo. Clearing cache for ${artifactDto.groupId}/${artifactDto.artifactId}"
            )
            // TODO: we need to test if the artifacts iterable is empty after delete
            artifacts.forEach { it.delete() }
        }

        val updateNeededForArtifact: Pair<Boolean, Artifact> = if (artifacts.empty()) {
            val artifactDb = Artifact.new {
                artifactId = artifactDto.artifactId
                groupId = artifactDto.groupId
            }
            Pair(true, artifactDb)
        } else {
            val artifactDb = artifacts.first()
            if (artifactDb.versions.count().toInt() == artifactDto.versions.count()) {
                Pair(false, artifactDb)
            } else {
                Pair(true, artifactDb)
            }
        }

        if (updateNeededForArtifact.first) {
            artifactDto.versions.forEach { version ->
                if (version.releaseDate != -1L) {
                    Version.new {
                        versionNumber = version.versionNumber
                        releaseDate = version.releaseDate
                        artifact = updateNeededForArtifact.second
                    }
                } else {
                    println("Not storing version with missing release date $version")
                }
            }
        }
    }
        artifactDto.transitiveDependencies.forEach { transitiveDependency ->
            recursivelyUpdateCache(artifactDto = transitiveDependency)
        }

}
