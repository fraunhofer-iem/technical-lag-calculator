//package commands
//
//import com.github.ajalt.clikt.core.CliktCommand
//import com.github.ajalt.clikt.parameters.options.*
//import com.github.ajalt.clikt.parameters.types.path
//import git.GitHelper
//import kotlinx.coroutines.runBlocking
//import kotlinx.serialization.Serializable
//import kotlinx.serialization.json.Json
//import libyears.LibyearCalculator
//import libyears.LibyearConfig
//import org.apache.logging.log4j.kotlin.logger
//import org.slf4j.MDC
//import java.nio.file.Path
//import java.util.*
//import kotlin.io.path.createDirectories
//import kotlin.io.path.pathString
//import kotlin.time.measureTime
//
//
//@Serializable
//data class GitConfig(val urls: List<String>)
//
///**
// * Input: Exported analyzer results containing a dependency graph annotated with version information
// *
// * Output: Libyears annotated to the dependency graph
// */
//class Libyears : CliktCommand() {
//
//    private val inputPaths by option(
//        help = "Path to the file containing the Paths of" +
//                "the files to be analyzed."
//    )
//        .path(mustExist = true, mustBeReadable = true, canBeFile = true)
//        .required()
//
//    private val outputPath by option(
//        help = "Path in which all analyzer results are stored"
//    )
//        .path(mustExist = false, mustBeReadable = true, canBeFile = false)
//
//
//    override fun run(): Unit = runBlocking {
//
//        val projectPaths = Json.decodeFromString<ProjectPaths>(inputPaths.toFile().readText())
//
//        // Setup logging and corresponding output paths
//        val defaultLogPath = MDC.get("outputFile") ?: ""
//
//        val outputPathWrapper = outputPath.resolve("libyearResults-${Date().time}")
//        logger.info {
//            "Running libyears for projects in $gitConfigFile and output path $outputPathWrapper" +
//                    " and db url ${dbOptions?.dbUrl}"
//        }
//
//
//        val runtime = measureTime {
//
//            gits.urls.forEach { gitUrl ->
//                logger.info { "Analyzing git at url $gitUrl" }
//
//                val repoName = gitUrl.split("/").last()
//
//
//                // Setup logging to store the log file in the cloned git repository
//                val gitCheckoutPath = outputPathWrapper.resolve("$repoName-${Date().time}")
//                MDC.put("outputFile", gitCheckoutPath.toAbsolutePath().resolve(repoName).pathString)
//                println(MDC.getCopyOfContextMap())
//                gitCheckoutPath.createDirectories()
//
//                val gitHelper = GitHelper(gitUrl, outDir = gitCheckoutPath.toFile())
//                val libyearCalculator = LibyearCalculator()
//
//                try {
//                    gitHelper.forEach { commit ->
//                        try {
//                            libyearCalculator.run(
//                                LibyearConfig(projectPath = gitCheckoutPath.toFile())
//                            )
//                        } catch (e: Exception) {
//                            logger.error { "Libyear calculation failed for commit $commit with $e ${e.stackTrace}" }
//                        }
//                    }
//                } catch (e: Exception) {
//                    logger.error { "Processing git commit failed with $e" }
//                }
//
//                MDC.put("outputFile", defaultLogPath)
//
//
////                storeResults(
////                    results = libyearCalculator.getAllAnalyzerResults(),
////                    aggregatedResults = libyearCalculator.getAllLibyearResults(),
////                    config = StorageConfig(
////                        outputPath = gitCheckoutPath,
////                        storeLibyearResultsInDb = storeLibyearResultsInDb,
////                        storeLibyearResultsInFile = storeLibyearResultsInFile,
////                        storeAnalyzerResultsInDb = storeAnalyzerResultsInDb,
////                        storeAnalyzerResultsInFile = storeAnalyzerResultsInFile,
////                        storeLibyearGraphs = storeLibyearGraphs,
////                        dbConfig = dbConfig
////                    )
////                )
//                MDC.put("outputFile", outputPathWrapper.toAbsolutePath().pathString)
//            }
//        }
//
//        logger.info { "The libyear calculation took ${runtime.inWholeMinutes} minutes to execute." }
//
//    }
//}
//
//fun getConfigFromPath(path: Path): GitConfig {
//    val json = Json
//    return json.decodeFromString<GitConfig>(path.toFile().readText())
//}
