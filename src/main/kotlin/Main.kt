import artifact.ArtifactService
import artifact.db.Artifact
import artifact.db.Artifacts
import artifact.db.Version
import artifact.model.ArtifactDto
import artifact.model.DependencyGraphDto
import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.parameters.groups.OptionGroup
import com.github.ajalt.clikt.parameters.groups.cooccurring
import com.github.ajalt.clikt.parameters.options.option
import com.github.ajalt.clikt.parameters.options.required
import com.github.ajalt.clikt.parameters.types.path
import dependencies.DependencyAnalyzer
import dependencies.db.DependencyGraph
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import libyears.LibyearCalculator
import org.jetbrains.exposed.dao.with
import org.jetbrains.exposed.sql.and
import util.DbConfig
import util.dbQuery
import util.initDatabase
import java.io.File
import java.nio.file.Path
import java.util.*
import kotlin.io.path.createDirectories

class DbOptions : OptionGroup() {
    val dbUrl by option(
        envvar = "DB_URL", help = "Optional path to store a file based database which contains" +
                " version numbers and their release dates." +
                "This database is used as a cache and the application works seamlessly without it." +
                "If the path doesn't exist it will be created."
    ).required()

    val userName by option(envvar = "DB_USER", help = "Username of database user").required()
    val password by option(envvar = "DB_PW", help = "Password for given database user").required()
}

class Libyears : CliktCommand() {
    val dbOptions by DbOptions().cooccurring()

    val projectPath by option(envvar = "PROJECT_PATH", help = "Path to the analyzed project's root.")
        .path(mustExist = true, mustBeReadable = true, canBeFile = false)
        .required()

    val outputPath by option(
        envvar = "OUTPUT_PATH", help = "Path to the folder to store the JSON results" +
                "of the created dependency graph. If the path doesn't exist it will be created."
    )
        .path(mustExist = false, canBeFile = false)

    override fun run() {
        echo(
            "Running libyears for project at $projectPath and output path $outputPath" +
                    " and db url ${dbOptions?.dbUrl}"
        )
        outputPath?.createDirectories()
    }
}

suspend fun main(args: Array<String>) {
    val libyearCommand = Libyears()
    libyearCommand.main(args)
    val dbConfig = libyearCommand.dbOptions?.let {
        DbConfig(
            url = it.dbUrl,
            userName = it.userName,
            password = it.password
        )
    }
    getLibYears(
        projectPath = libyearCommand.projectPath.toFile(),
        outputPath = libyearCommand.outputPath,
        dbConfig = dbConfig,
    )
}


suspend fun getLibYears(projectPath: File, outputPath: Path?, dbConfig: DbConfig?): DependencyGraphDto {
    val storeResults = dbConfig != null
    if (storeResults) {
        initDatabase(dbConfig!!)
    }

    val dependencyAnalyzer = DependencyAnalyzer(
        ArtifactService(storeResults)
    )

    val dependencyAnalyzerResult = dependencyAnalyzer.getDependencyPackagesForProject(projectPath)

    val libyearCalculator = LibyearCalculator()
    libyearCalculator.printDependencyGraph(dependencyAnalyzerResult.dependencyGraphDto)


    if (outputPath != null) {
        val outputFile = outputPath.resolve("${Date().time}-graphResult.json").toFile()
        withContext(Dispatchers.IO) {
            outputFile.createNewFile()
            val json = Json { prettyPrint = false }
            val jsonString =
                json.encodeToString(DependencyGraphDto.serializer(), dependencyAnalyzerResult.dependencyGraphDto)
            outputFile.writeText(jsonString)
        }
    }


    if (storeResults) {
        dbQuery {
            DependencyGraph.new {
                graph = dependencyAnalyzerResult.dependencyGraphDto
            }
        }
        dependencyAnalyzerResult.dependencyGraphDto.packageManagerToScopes.values.forEach {
            it.scopesToDependencies.values.forEach {
                it.forEach { artifact ->
                    recursivelyUpdateCache(artifact)
                }
            }
        }
    }

    return dependencyAnalyzerResult.dependencyGraphDto
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
