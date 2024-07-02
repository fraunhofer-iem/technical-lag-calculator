package commands.createDependencyGraph

import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.parameters.options.option
import com.github.ajalt.clikt.parameters.options.required
import com.github.ajalt.clikt.parameters.types.path
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.json.Json
import org.apache.logging.log4j.kotlin.logger
import shared.analyzerResultDtos.AnalyzerResultDto
import shared.analyzerResultDtos.ProjectDto
import shared.project.ProjectPaths
import util.StoreResultHelper
import java.io.File
import kotlin.io.path.createDirectories


/**
 * Input: local paths to git repositories
 * Output: AnalyzerResultDto(s)
 */
class CreateDependencyGraph : CliktCommand() {

    private val projectListPath by option(
        help = "Path to the file containing the Paths of" +
                "the repositories which will be analyzed."
    )
        .path(mustExist = true, mustBeReadable = true, canBeFile = true)
        .required()

    private val outputPath by option(
        help = "Path in which all analyzer results are stored"
    )
        .path(mustExist = false, canBeFile = false)
        .required()

    override fun run(): Unit = runBlocking {

        val projectPaths = Json.decodeFromString<ProjectPaths>(projectListPath.toFile().readText())
        logger.info { "Running ORT on projects $projectPaths" }

        val dependencyAnalyzer = DependencyAnalyzer()
        val dependencyGraphService = DependencyGraphService()
        outputPath.createDirectories()

        val resultFiles = projectPaths.paths.map { File(it) }.mapNotNull { file ->
            try {
                if (!file.exists() || !file.isDirectory) {
                    logger.error("Given path $file is not a directory or doesn't exist.")
                    return@mapNotNull null
                }
                // TODO: check if we correctly generate the tree if there is no version information for
                //  intermediate nodes, but for their children. This can happen if intermediate nodes are
                //  developed internally but use OSS dependencies.
                val rawResult = dependencyAnalyzer.run(file)
                val projects = dependencyGraphService.createProjectsFromGraphs(rawResult.dependencyGraphs)
                val mainProject = rawResult.repositoryInfo.projects.first()

                val result = AnalyzerResultDto(
                    projectDtos = projects.map {
                        ProjectDto(
                            project = it,
                            version = mainProject.version,
                            artifactId = mainProject.name,
                            groupId = mainProject.namespace
                        )
                    },
                    repositoryInfo = rawResult.repositoryInfo,
                    environmentInfo = rawResult.environmentInfo,
                )


                StoreResultHelper.storeAnalyzerResultInFile(outputPath.toFile(), result)


            } catch (error: Error) {
                logger.error("Dependency Analyzer failed with error $error")
                null
            }
        }.map { it.path }

        dependencyGraphService.close()
        StoreResultHelper.storeResultFilePathsInFile(outputPath.toFile(), ProjectPaths(resultFiles))
    }
}
