import artifact.ArtifactService
import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.parameters.groups.OptionGroup
import com.github.ajalt.clikt.parameters.groups.cooccurring
import com.github.ajalt.clikt.parameters.options.option
import com.github.ajalt.clikt.parameters.options.required
import com.github.ajalt.clikt.parameters.types.path
import dependencies.DependencyAnalyzer
import dependencies.db.DependencyGraph
import dependencies.model.DependencyGraphDto
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import libyears.LibyearCalculator
import util.DbConfig
import util.dbQuery
import util.initDatabase
import util.updateCache
import java.io.File
import java.nio.file.Path
import java.util.*
import kotlin.io.path.createDirectories
import kotlin.system.measureTimeMillis

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
    val runtime = measureTimeMillis {
        getLibYears(
            projectPath = libyearCommand.projectPath.toFile(),
            outputPath = libyearCommand.outputPath,
            dbConfig = dbConfig,
        )
    } / 60000
    println("The libyear calculation took $runtime minutes to execute.")
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


    LibyearCalculator.printDependencyGraph(dependencyAnalyzerResult.dependencyGraphDto)


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
        updateCache(dependencyAnalyzerResult.dependencyGraphDto)
    }

    return dependencyAnalyzerResult.dependencyGraphDto
}
