import ch.qos.logback.classic.Level
import ch.qos.logback.classic.Logger
import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.parameters.groups.OptionGroup
import com.github.ajalt.clikt.parameters.groups.cooccurring
import com.github.ajalt.clikt.parameters.options.default
import com.github.ajalt.clikt.parameters.options.option
import com.github.ajalt.clikt.parameters.options.required
import com.github.ajalt.clikt.parameters.options.switch
import com.github.ajalt.clikt.parameters.types.path
import dependencies.DependencyAnalyzer
import dependencies.db.AnalyzerResult
import dependencies.model.DependencyGraphDto
import git.GitHelper
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import libyears.LibyearCalculator
import libyears.model.LibyearSumsForPackageManagerAndScopes
import org.apache.logging.log4j.kotlin.logger
import org.slf4j.LoggerFactory
import util.DbConfig
import util.dbQuery
import util.initDatabase
import java.io.File
import java.nio.file.Path
import java.util.*
import kotlin.io.path.createDirectories
import kotlin.time.measureTime

//TODO: Add file based logging
//TODO: update result file and folder names for easier usability (use last part of url + time)
//TODO: ignore dependencies in "test" folders

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

@Serializable
data class GitConfig(val urls: List<String>)

class Libyears : CliktCommand() {
    private val dbOptions by DbOptions().cooccurring()

    private val gitConfigFile by option(
        envvar = "GIT_CONFIG_PATH", help = "Path to the file containing the URLs of" +
                "the repositories which should be analyzed."
    )
        .path(mustExist = true, mustBeReadable = true, canBeFile = true)
        .required()

    private val outputPath by option(
        envvar = "OUTPUT_PATH", help = "Path to the folder to store the JSON results" +
                "of the created dependency graph. If the path doesn't exist it will be created."
    )
        .path(mustExist = false, canBeFile = false)
        .required()

    private val logLevel: Level by option(help = "Set the verbosity level of log output.").switch(
        "--error" to Level.ERROR,
        "--warn" to Level.WARN,
        "--info" to Level.INFO,
        "--debug" to Level.DEBUG
    ).default(Level.INFO)

    override fun run(): Unit = runBlocking {
        val rootLogger = LoggerFactory.getLogger(Logger.ROOT_LOGGER_NAME) as Logger
        rootLogger.level = logLevel

        logger.info {
            "Running libyears for projects in $gitConfigFile and output path $outputPath" +
                    " and db url ${dbOptions?.dbUrl}"
        }
        outputPath.createDirectories()

        val dbConfig = dbOptions?.let {
            DbConfig(
                url = it.dbUrl,
                userName = it.userName,
                password = it.password
            )
        }

        val gits = getConfigFromPath(gitConfigFile)


        val runtime = measureTime {
            gits.urls.forEachIndexed { idx, gitUrl ->
                logger.info { "Analyzing git at url $gitUrl" }
                val outputPath = outputPath.resolve("${Date().time}-$idx")
                outputPath.createDirectories()
                val gitHelper = GitHelper(gitUrl, outDir = outputPath.toFile())
                val libyearResultForPackageManagerAndScopes: MutableList<LibyearSumsForPackageManagerAndScopes> =
                    mutableListOf()
                try {
                    gitHelper.forEach { _ ->
                        try {
                            getLibYears(
                                projectPath = outputPath.toFile(),
                                outputPath = outputPath,
                                dbConfig = dbConfig,
                            )?.let { libyears ->
                                libyearResultForPackageManagerAndScopes.add(
                                    libyears
                                )
                            }
                        } catch (e: Exception) {
                            logger.error { "Libyear calculation failed with $e ${e.stackTrace}" }
                        }
                    }
                } catch (e: Exception) {
                    logger.error { "Processing git commit failed with $e" }
                }

                val outputFileAggregate =
                    outputPath.resolve("${Date().time}-graphResultAggregate.json").toFile()
                withContext(Dispatchers.IO) {
                    outputFileAggregate.createNewFile()
                    val json = Json { prettyPrint = false }
                    val transitiveDeps =
                        libyearResultForPackageManagerAndScopes.flatMap { it.packageManagerToScopes.flatMap { it.value.map { it.value.transitive } } }
                    val directDeps =
                        libyearResultForPackageManagerAndScopes.flatMap { it.packageManagerToScopes.flatMap { it.value.map { it.value.direct } } }
                    val jsonString =
                        json.encodeToString(
                            AggregatedResults.serializer(),
                            AggregatedResults(
                                libyearResultForPackageManagerAndScopes,
                                // TODO: these values seem to be somehow off. Need to investigate this
                                cvsDirectLibyears = directDeps.map { it.libyears },
                                csvTransitiveLibyears = transitiveDeps.map { it.libyears },
                                csvDirectNumberOfDeps = directDeps.map { it.numberOfDependencies },
                                csvTransitiveNumberOfDeps = transitiveDeps.map { it.numberOfDependencies }
                            )
                        )
                    outputFileAggregate.writeText(jsonString)
                }

            }
        }
        logger.info { "The libyear calculation took ${runtime.inWholeMinutes} minutes to execute." }

    }
}

@Serializable
data class AggregatedResults(
    val results: List<LibyearSumsForPackageManagerAndScopes>,
    val csvTransitiveLibyears: List<Long>,
    val csvTransitiveNumberOfDeps: List<Int>,
    val csvDirectNumberOfDeps: List<Int>,
    val cvsDirectLibyears: List<Long>
)

fun main(args: Array<String>) {
    val libyearCommand = Libyears()
    libyearCommand.main(args)
}

fun getConfigFromPath(path: Path): GitConfig {
    val json = Json
    return json.decodeFromString<GitConfig>(path.toFile().readText())
}


suspend fun getLibYears(
    projectPath: File,
    outputPath: Path?,
    dbConfig: DbConfig?
): LibyearSumsForPackageManagerAndScopes? {
    val useDb = dbConfig != null

    if (useDb) {
        initDatabase(dbConfig!!)
    }

    val dependencyAnalyzer = DependencyAnalyzer()

    dependencyAnalyzer.getAnalyzerResult(projectPath)?.let { dependencyAnalyzerResult ->
        // TODO: maven currently doesn't work without fixed versions. Need to check ORT if this can be circumvented
        // through configuration

        val libyearAggregates = LibyearCalculator.printDependencyGraph(dependencyAnalyzerResult.dependencyGraphDto)


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


        if (useDb) {
            dbQuery {
                AnalyzerResult.new {
                    result = dependencyAnalyzerResult
                }
            }
        }

        return libyearAggregates
    }
    return null
}
