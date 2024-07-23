package commands.createDependencyGraph

import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.parameters.options.option
import com.github.ajalt.clikt.parameters.options.required
import com.github.ajalt.clikt.parameters.types.path
import kotlinx.coroutines.runBlocking
import org.apache.logging.log4j.kotlin.logger
import shared.analyzerResultDtos.AnalyzerResultDto
import shared.analyzerResultDtos.ProjectDto
import reporter.Reporter
import java.io.File
import java.nio.file.Files
import kotlin.io.path.createDirectories


/**
 * Input: local paths to git repositories
 * Output: AnalyzerResultDto(s)
 */
class CreateDependencyGraph : CliktCommand() {

    private val projectsDir by option(
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

        logger.info { "Running ORT on projects $projectsDir" }

        val dependencyAnalyzer = DependencyAnalyzer()
        val dependencyGraphService = DependencyGraphService()
        outputPath.createDirectories()

        val subDirs = Files.walk(projectsDir).toList().toMutableList()
        // the first element is the base path which we can't analyze
        subDirs.removeFirst()
        subDirs.filter { Files.isDirectory(it) }
            .map { it.toAbsolutePath().toFile() }
            .forEach { subDir ->
                processProject(subDir, dependencyGraphService, dependencyAnalyzer)
            }

        dependencyGraphService.close()
    }

    private suspend fun processProject(
        projectDir: File,
        dependencyGraphService: DependencyGraphService,
        dependencyAnalyzer: DependencyAnalyzer
    ) {
        try {
            if (!projectDir.exists() || !projectDir.isDirectory) {
                logger.error("Given path $projectDir is not a directory or doesn't exist.")
            }
            // TODO: check if we correctly generate the tree if there is no version information for
            //  intermediate nodes, but for their children. This can happen if intermediate nodes are
            //  developed internally but use OSS dependencies.
            val rawResult = dependencyAnalyzer.run(projectDir)
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
            val reporter = Reporter(outputPath)
            reporter.storeAnalyzerResultInFile(result)
        } catch (error: Exception) {
            logger.error("Dependency Analyzer failed with error $error")
        }
    }
}
