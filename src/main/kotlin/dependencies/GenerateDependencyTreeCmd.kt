package dependencies

import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.parameters.options.option
import com.github.ajalt.clikt.parameters.options.required
import com.github.ajalt.clikt.parameters.types.path
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import org.apache.logging.log4j.kotlin.logger
import util.StoreResultHelper
import java.io.File
import kotlin.io.path.createDirectories

@Serializable
data class ProjectPaths(val paths: List<String>)

/**
 * Input: local paths to git repositories
 * Output: AnalyzerResultDto(s)
 */
class GenerateDependencyTree : CliktCommand() {

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
        outputPath.createDirectories()

        val resultFiles = projectPaths.paths.map { File(it) }.mapNotNull { file ->
            try {
                if (file.exists() && file.isDirectory) {
                    // TODO: check if we correctly generate the tree if there is no version information for
                    //  intermediate nodes, but for their children. This can happen if intermediate nodes are
                    //  developed internally but use OSS dependencies.
                    val result = dependencyAnalyzer.getAnalyzerResult(file)

                    if (result != null) {
                        StoreResultHelper.storeAnalyzerResultInFile(outputPath.toFile(), result)
                    } else {
                        logger.warn("Couldn't retrieve result for $file")
                        null
                    }
                } else {
                    logger.error("Given path $file is not a directory or doesn't exist.")
                    null
                }
            } catch (error: Error) {
                logger.error("Dependency Analyzer failed with error $error")
                null
            }
        }.map { it.path }

        dependencyAnalyzer.close()
        StoreResultHelper.storeResultFilePathsInFile(outputPath.toFile(), ProjectPaths(resultFiles))
    }
}
