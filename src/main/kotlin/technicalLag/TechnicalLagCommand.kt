package technicalLag


import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.parameters.options.option
import com.github.ajalt.clikt.parameters.options.required
import com.github.ajalt.clikt.parameters.types.path
import dependencies.ProjectPaths
import dependencies.model.AnalyzerResultDto
import dependencies.model.DependencyGraphs
import dependencies.model.VersionType
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.json.Json
import org.apache.logging.log4j.kotlin.logger
import org.slf4j.MDC
import java.io.File
import kotlin.io.path.createDirectories


/**
 * Input: Exported analyzer results containing a dependency graph annotated with version information
 *
 * Output: Technical lag annotated to the dependency graph
 */
class TechnicalLag : CliktCommand() {

    private val inputPath by option(
        help = "Path to the file containing the Paths of" +
                "the files to be analyzed."
    )
        .path(mustExist = true, mustBeReadable = true, canBeFile = true)
        .required()

    private val outputPath by option(
        help = "Path in which all analyzer results are stored"
    )
        .path(mustExist = false, mustBeReadable = false, canBeFile = false)
        .required()


    override fun run(): Unit = runBlocking {
        outputPath.createDirectories()
        val projectPaths = Json.decodeFromString<ProjectPaths>(inputPath.toFile().readText())

        // Setup logging and corresponding output paths
        val defaultLogPath = MDC.get("outputFile") ?: ""
        val technicalLagService = TechnicalLagService()
        logger.info { "Running libyears for projects in $projectPaths and output path $outputPath" }

        outputPath.createDirectories()

        projectPaths.paths.map { File(it) }.forEach { resultFile ->
            val analyzerResult = Json.decodeFromString<AnalyzerResultDto>(resultFile.readText())
            analyzerResult.dependencyGraphDtos.forEach { dependencyGraphsDto ->
                val graphs = DependencyGraphs(dependencyGraphsDto)
                technicalLagService.connectDependenciesToStats(graphs)

                graphs.graph.forEach { (scope, graph) ->
                    println("Scope $scope")
                    println(graph.metadata)
                    val root = graph.rootDependency
                    VersionType.entries.forEach { versionType ->
                        println("Stats for version type $versionType")
                        println(root.getStatForVersionType(versionType))
                    }

                }
            }
        }

    }


//
//        gits.urls.forEach { gitUrl ->
//            logger.info { "Analyzing git at url $gitUrl" }
//
//            val repoName = gitUrl.split("/").last()
//
//
//            // Setup logging to store the log file in the cloned git repository
//            val gitCheckoutPath = outputPathWrapper.resolve("$repoName-${Date().time}")
//            MDC.put("outputFile", gitCheckoutPath.toAbsolutePath().resolve(repoName).pathString)
//            println(MDC.getCopyOfContextMap())
//            gitCheckoutPath.createDirectories()
//
//            val gitHelper = GitHelper(gitUrl, outDir = gitCheckoutPath.toFile())
//            val libyearCalculator = LibyearCalculator()
//
//            try {
//                gitHelper.forEach { commit ->
//                    try {
//                        libyearCalculator.run(
//                            LibyearConfig(projectPath = gitCheckoutPath.toFile())
//                        )
//                    } catch (e: Exception) {
//                        logger.error { "Libyear calculation failed for commit $commit with $e ${e.stackTrace}" }
//                    }
//                }
//            } catch (e: Exception) {
//                logger.error { "Processing git commit failed with $e" }
//            }
//
//            MDC.put("outputFile", defaultLogPath)
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
//            MDC.put("outputFile", outputPathWrapper.toAbsolutePath().pathString)
//
//        }

}
//
//fun getConfigFromPath(path: Path): GitConfig {
//    val json = Json
//    return json.decodeFromString<GitConfig>(path.toFile().readText())
//}
