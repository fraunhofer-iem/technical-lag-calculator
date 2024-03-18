import ch.qos.logback.classic.Level
import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.core.subcommands
import com.github.ajalt.clikt.parameters.groups.OptionGroup
import com.github.ajalt.clikt.parameters.groups.cooccurring
import com.github.ajalt.clikt.parameters.options.*
import com.github.ajalt.clikt.parameters.types.choice
import com.github.ajalt.clikt.parameters.types.path
import git.GitHelper
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import libyears.LibyearCalculator
import libyears.LibyearConfig
import org.apache.logging.log4j.kotlin.logger
import org.slf4j.MDC
import util.*
import vulnerabilities.VulnerabilityAnalyzer
import vulnerabilities.VulnerabilityVersionDownloader
import java.nio.file.Path
import java.util.*
import kotlin.io.path.createDirectories
import kotlin.io.path.pathString
import kotlin.time.measureTime

class DbOptions : OptionGroup() {
    val dbUrl by option(
        envvar = "DB_URL", help = "Optional path to store a file based database which contains" +
                " version numbers and their release dates." +
                "This database is used as a cache and the application works seamlessly without it." +
                "If the path doesn't exist it will be created."
    ).required()

    val userName by option(envvar = "DB_USER", help = "Username of database user")
    val password by option(envvar = "DB_PW", help = "Password for given database user")
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

    private val storeAnalyzerResultsInFile by option().flag(default = false)
    private val storeAnalyzerResultsInDb by option().flag(default = false)
    private val storeLibyearResultsInFile by option().flag(default = true)
    private val storeLibyearResultsInDb by option().flag(default = false)
    private val storeLibyearGraphs by option().flag(default = true)

    private val logLevel: Level by option(help = "Set the verbosity level of log output.").switch(
        "--error" to Level.ERROR,
        "--warn" to Level.WARN,
        "--info" to Level.INFO,
        "--debug" to Level.DEBUG
    ).default(Level.INFO)

    private val logMode by option().choice("console", "file", "consoleAndFile").default("console")


    override fun run(): Unit = runBlocking {

        // Setup logging and corresponding output paths
        configureRootLogger(logLevel, logMode)
        val outputPathWrapper = outputPath.resolve("libyearResults-${Date().time}")
        outputPathWrapper.createDirectories()
        val defaultLogPath = outputPathWrapper.toAbsolutePath().resolve("libyear").pathString
        MDC.put("outputFile", defaultLogPath)
        logger.info {
            "Running libyears for projects in $gitConfigFile and output path $outputPathWrapper" +
                    " and db url ${dbOptions?.dbUrl}"
        }

        // Load configs
        val dbConfig = dbOptions?.let {
            DbConfig(
                url = it.dbUrl,
                userName = it.userName ?: "",
                password = it.password ?: ""
            )
        }
        val gits = getConfigFromPath(gitConfigFile)


        val runtime = measureTime {

            gits.urls.forEach { gitUrl ->
                logger.info { "Analyzing git at url $gitUrl" }

                val repoName = gitUrl.split("/").last()


                // Setup logging to store the log file in the cloned git repository
                val gitCheckoutPath = outputPathWrapper.resolve("$repoName-${Date().time}")
                MDC.put("outputFile", gitCheckoutPath.toAbsolutePath().resolve(repoName).pathString)
                println(MDC.getCopyOfContextMap())
                gitCheckoutPath.createDirectories()

                val gitHelper = GitHelper(gitUrl, outDir = gitCheckoutPath.toFile())
                val libyearCalculator = LibyearCalculator()

                try {
                    gitHelper.forEach { commit ->
                        try {
                            libyearCalculator.run(
                                LibyearConfig(projectPath = gitCheckoutPath.toFile())
                            )
                        } catch (e: Exception) {
                            logger.error { "Libyear calculation failed for commit $commit with $e ${e.stackTrace}" }
                        }
                    }
                } catch (e: Exception) {
                    logger.error { "Processing git commit failed with $e" }
                }

                MDC.put("outputFile", defaultLogPath)


//                storeResults(
//                    results = libyearCalculator.getAllAnalyzerResults(),
//                    aggregatedResults = libyearCalculator.getAllLibyearResults(),
//                    config = StorageConfig(
//                        outputPath = gitCheckoutPath,
//                        storeLibyearResultsInDb = storeLibyearResultsInDb,
//                        storeLibyearResultsInFile = storeLibyearResultsInFile,
//                        storeAnalyzerResultsInDb = storeAnalyzerResultsInDb,
//                        storeAnalyzerResultsInFile = storeAnalyzerResultsInFile,
//                        storeLibyearGraphs = storeLibyearGraphs,
//                        dbConfig = dbConfig
//                    )
//                )
                MDC.put("outputFile", outputPathWrapper.toAbsolutePath().pathString)
            }
        }

        logger.info { "The libyear calculation took ${runtime.inWholeMinutes} minutes to execute." }

    }
}

class GetVersions : CliktCommand() {
    private val inputPath by option(
        envvar = "INPUT_PATH", help = "Path to the folder in which the vulnerability information are stored."
    )
        .path(mustExist = false, canBeFile = false)
        .required()

    override fun run() = runBlocking {
        val vulnerabilityVersionDownloader = VulnerabilityVersionDownloader()
        vulnerabilityVersionDownloader.storeVersionsForVulnerablePackages(inputPath)
    }
}

class AnalyzeVersions : CliktCommand() {

    private val dbOptions by DbOptions().cooccurring()

    private val inputPath by option(
        envvar = "INPUT_PATH",
        help = "Path to the folder in which the combined version and vulnerability information are stored."
    )
        .path(mustExist = false, canBeFile = false)
        .required()

    override fun run() = runBlocking {

        val dbConfig = dbOptions?.let {
            DbConfig(
                url = it.dbUrl,
                userName = it.userName,
                password = it.password
            )
        }
        if (dbConfig != null) {
            initSqlLiteDb(dbConfig)
            val vulnerabilityAnalyzer =
                VulnerabilityAnalyzer(inputPath)
            vulnerabilityAnalyzer.dbExport()
        }
//        val vulnerabilityAnalyzer =
//            VulnerabilityAnalyzer(inputPath)
////        vulnerabilityAnalyzer.analyze()
//        val histogram = vulnerabilityAnalyzer.histogram()
//        Visualizer.createAndStoreHistogram(
//            histogram,
//            inputPath.resolve("histogram-noOutlierGreater900.png").toString()
//        )
    }
}

class Tool : CliktCommand() {

    override fun run() {
        echo("Starting tool and setting up logging")

    }
}


fun main(args: Array<String>) {
    val tool = Tool()
    tool.subcommands(Libyears(), GetVersions(), AnalyzeVersions()).main(args)
}

fun getConfigFromPath(path: Path): GitConfig {
    val json = Json
    return json.decodeFromString<GitConfig>(path.toFile().readText())
}



