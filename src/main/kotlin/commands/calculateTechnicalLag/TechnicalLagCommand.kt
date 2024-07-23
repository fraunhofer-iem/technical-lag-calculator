package commands.calculateTechnicalLag


import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.parameters.options.option
import com.github.ajalt.clikt.parameters.options.required
import com.github.ajalt.clikt.parameters.types.path
import commands.calculateTechnicalLag.visualization.Visualizer
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.json.Json
import org.apache.logging.log4j.kotlin.logger
import org.jetbrains.kotlinx.dataframe.api.toDataFrame
import shared.analyzerResultDtos.AnalyzerResultDto
import shared.analyzerResultDtos.ProjectDto
import shared.project.Project
import reporter.Reporter
import java.nio.file.Files
import kotlin.io.path.createDirectories
import kotlin.io.path.extension


/**
 * Input: Exported analyzer results containing a dependency graph annotated with version information
 *
 * Output: Technical lag annotated to the dependency graph
 */
class CalculateTechnicalLag : CliktCommand() {

    private val dependencyGraphDirs by option(
        help = "Path to the file containing the Paths of" +
                "the files to be analyzed."
    )
        .path(mustExist = true, mustBeReadable = true, canBeFile = false)
        .required()

    private val outputPath by option(
        help = "Path in which all analyzer results are stored"
    )
        .path(mustExist = false, mustBeReadable = false, canBeFile = false)
        .required()

    override fun run(): Unit = runBlocking {
        outputPath.createDirectories()
        val reporter = Reporter(outputPath)
        val technicalLagStatisticsService = TechnicalLagStatisticsService()
        logger.info { "Running libyears for projects in $dependencyGraphDirs and output path $outputPath" }


        val techLagExport = mutableListOf<Visualizer.TechnicalLagExport>()

        val files = Files.walk(dependencyGraphDirs).toList().toMutableList()
        // the first element is the base path which we can't analyze
        files.removeFirst()
        files.filter { !Files.isDirectory(it) && it.extension == "json" }
            .map { it.toAbsolutePath().toFile() }
            .forEach { file ->

                val analyzerResult = try {
                    Json.decodeFromString<AnalyzerResultDto>(file.readText())
                } catch (e: Exception) {
                    null
                }
                if(analyzerResult == null) {
                    return@forEach
                }

                val projectsWithStats = analyzerResult.projectDtos.map { dependencyGraphsDto ->
                    val project = Project(dependencyGraphsDto)

                    technicalLagStatisticsService.connectDependenciesToStats(project)
                    techLagExport.addAll(technicalLagStatisticsService.getTechnicalLagExport(project))

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
                reporter.storeAnalyzerResultInFile(result)
            }

        val df = techLagExport.toDataFrame()
        Visualizer.createAndStoreBoxplotFromTechLag(df, outputPath = outputPath)
        reporter.storeCsvExport(df)
        reporter.generateHtmlReport()
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
//        }

}
//
//fun getConfigFromPath(path: Path): GitConfig {
//    val json = Json
//    return json.decodeFromString<GitConfig>(path.toFile().readText())
//}
