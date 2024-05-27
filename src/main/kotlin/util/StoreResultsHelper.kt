package util

import artifact.model.ArtifactWithStatsDto
import dependencies.ProjectPaths
import dependencies.model.AnalyzerResultDto
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import java.io.File
import java.util.*


class StoreResultHelper {
    companion object {
        private val json = Json { prettyPrint = true }

        suspend fun storeAnalyzerResultInFile(
            outputDirectory: File,
            result: AnalyzerResultDto,
            dispatcher: CoroutineDispatcher = Dispatchers.IO
        ): File {
            val outputFile = outputDirectory.resolve("${Date().time}-analyzerResult.json")
            return withContext(dispatcher) {
                outputFile.createNewFile()

                val jsonString =
                    json.encodeToString(AnalyzerResultDto.serializer(), result)
                outputFile.writeText(jsonString)
                return@withContext outputFile
            }
        }

        suspend fun storeStatsInFile(
            outputDirectory: File, scope: String, artifactDto: ArtifactWithStatsDto,
            dispatcher: CoroutineDispatcher = Dispatchers.IO
        ): File {
            val outputFile = outputDirectory.resolve("${Date().time}-${scope}-stats.json")
            return withContext(dispatcher) {
                outputFile.createNewFile()

                val jsonString =
                    json.encodeToString(ArtifactWithStatsDto.serializer(), artifactDto)
                outputFile.writeText(jsonString)
                return@withContext outputFile
            }
        }

        suspend fun storeResultFilePathsInFile(
            outputDirectory: File,
            paths: ProjectPaths,
            dispatcher: CoroutineDispatcher = Dispatchers.IO
        ): File {
            val outputFile = outputDirectory.resolve("dependencyGraphs.json")
            return withContext(dispatcher) {
                outputFile.createNewFile()
                val jsonString =
                    json.encodeToString(ProjectPaths.serializer(), paths)
                outputFile.writeText(jsonString)
                return@withContext outputFile
            }
        }
    }
}
