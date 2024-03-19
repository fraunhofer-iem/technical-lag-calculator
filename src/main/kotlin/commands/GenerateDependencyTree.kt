package commands

import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.parameters.options.option
import com.github.ajalt.clikt.parameters.options.required
import com.github.ajalt.clikt.parameters.types.path
import dependencies.DependencyAnalyzer
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import org.apache.logging.log4j.kotlin.logger
import util.storeAnalyzerResultInFile
import java.io.File


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
        .path(mustExist = false, mustBeReadable = true, canBeFile = false)

    override fun run(): Unit = runBlocking {

        val projectPaths = Json.decodeFromString<ProjectPaths>(projectListPath.toFile().readText())
        logger.info { "Running ORT on projects $projectPaths" }
        val dependencyAnalyzer = DependencyAnalyzer()

        projectPaths.paths.forEach { path ->
            try {
                val file = File(path)
                if (file.exists() && file.isDirectory) {
                    val result = dependencyAnalyzer.getAnalyzerResult(file)

                    if (result != null) {
                        val outputPath = if (outputPath != null) {
                            outputPath!!.toFile()
                        } else {
                            file
                        }

                        storeAnalyzerResultInFile(outputPath, result)
                    } else {
                        logger.warn("Couldn't retrieve result for $path")
                    }
                } else {
                    logger.error("Given path $path is not a directory or doesn't exist.")
                }
            } catch (error: Error) {
                logger.error("Dependency Analyzer failed with error $error")
            }
        }
    }

}