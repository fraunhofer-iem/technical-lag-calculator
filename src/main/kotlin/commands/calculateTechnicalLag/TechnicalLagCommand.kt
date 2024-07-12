package commands.calculateTechnicalLag


import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.parameters.options.option
import com.github.ajalt.clikt.parameters.options.required
import com.github.ajalt.clikt.parameters.types.path
import commands.calculateTechnicalLag.model.TechnicalLagStatistics
import commands.calculateTechnicalLag.visualization.Visualizer
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.json.Json
import org.apache.logging.log4j.kotlin.logger
import shared.analyzerResultDtos.AnalyzerResultDto
import shared.analyzerResultDtos.ProjectDto
import shared.project.Project
import shared.project.ProjectPaths
import shared.project.artifact.VersionType
import util.CompleteEnumMap
import util.StoreResultHelper
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

        val technicalLagStatisticsService = TechnicalLagStatisticsService()
        logger.info { "Running libyears for projects in $projectPaths and output path $outputPath" }

        outputPath.createDirectories()

        val stats: MutableMap<String, CompleteEnumMap<VersionType, TechnicalLagStatistics>> =
            mutableMapOf()
        val rawStats: MutableMap<String, MutableList<Long>> =
            mutableMapOf()
        rawStats["all"] = mutableListOf()
        projectPaths.paths.map { File(it) }.forEach { resultFile ->
            val analyzerResult = Json.decodeFromString<AnalyzerResultDto>(resultFile.readText())

            val projectsWithStats = analyzerResult.projectDtos.map { dependencyGraphsDto ->
                val project = Project(dependencyGraphsDto)
                technicalLagStatisticsService.connectDependenciesToStats(project)

                val all = technicalLagStatisticsService.getAllLibdays(project)
                val direct = technicalLagStatisticsService.getAllDirectLibdays(project)
                val trans = technicalLagStatisticsService.getAllTransitiveLibdays(project)


                all.entries.forEach { (scope, libdays) ->
                    if (!rawStats.contains(scope)) {
                        rawStats[scope] = mutableListOf()
                    }
                    rawStats[scope]?.addAll(libdays)
                }


                direct.entries.forEach { (scope, libdays) ->
                    val key = "$scope-direct"
                    if (!rawStats.contains(key)) {
                        rawStats[key] = mutableListOf()
                    }
                    rawStats[key]?.addAll(libdays)
                }

                trans.entries.forEach { (scope, libdays) ->
                    val key = "$scope-transitive"
                    if (!rawStats.contains(key)) {
                        rawStats[key] = mutableListOf()
                    }
                    rawStats[key]?.addAll(libdays)
                }

                rawStats["all"]!!.addAll(
                    project.graph.values.flatMap { graph -> graph.nodes.mapNotNull { it.getAllStats()[VersionType.Major]?.technicalLag?.libDays } }
                        .toMutableList())

                project.graph.forEach { (scope, graph) ->
                    val directStatsKey = "$scope-direct"
                    val transitiveStatsKey = "$scope-transitive"

                    if (!stats.contains(scope)) {
                        stats[scope] =
                            CompleteEnumMap(VersionType.entries) { mutableListOf() }
                        stats[directStatsKey] =
                            CompleteEnumMap(VersionType.entries) { mutableListOf() }
                        stats[transitiveStatsKey] =
                            CompleteEnumMap(VersionType.entries) { mutableListOf() }
                    }

                    println("Scope $scope")
                    println(graph.metadata)

                    val statsMap = stats[scope]!!
                    val directStats = stats[directStatsKey]!!
                    val transitiveStats = stats[transitiveStatsKey]!!

                    statsMap.addAll(graph.getAllStats())
                    directStats.addAll(graph.getDirectDependencyStats())
                    transitiveStats.addAll(graph.getTransitiveDependencyStats())
                }
                project
            }

            val result = AnalyzerResultDto(
                projectDtos = projectsWithStats.map {
                    ProjectDto(
                        project = it,
                        version = it.version,
                        artifactId = it.artifactId,
                        groupId = it.groupId
                    )
                },
                repositoryInfo = analyzerResult.repositoryInfo,
                environmentInfo = analyzerResult.environmentInfo,
            )
            // TODO: store direct and transitive stats for the graph. need to extend the serialization and DTO
            StoreResultHelper.storeAnalyzerResultInFile(outputPath.toFile(), result)
        }

        Visualizer.createAndStoreBoxplot(
            rawStats,
            outputPath
        )


        println(stats)
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
