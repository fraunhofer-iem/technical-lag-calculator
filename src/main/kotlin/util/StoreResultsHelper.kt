package util

import dependencies.db.AnalyzerResult
import dependencies.model.AnalyzerResultDto
import dependencies.model.DependencyGraphDto
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import libyears.model.AggregatedResults
import java.nio.file.Path
import java.util.*

data class StorageConfig(
    val dbConfig: DbConfig?,
    val outputPath: Path?,
    val storeAnalyzerResultsInFile: Boolean,
    val storeAnalyzerResultsInDb: Boolean,
    val storeLibyearResultsInFile: Boolean,
    val storeLibyearResultsInDb: Boolean,
) {
    val storeAnalyzerResults = storeAnalyzerResultsInFile || storeAnalyzerResultsInDb
}


//TODO: implement db storage for libyear results
suspend fun storeResults(
    results: List<AnalyzerResultDto> = listOf(),
    aggregatedResults: AggregatedResults,
    config: StorageConfig
) {

    if (config.dbConfig != null) {
        initDatabase(config.dbConfig)
    }

    if (config.storeAnalyzerResultsInFile) {
        results.forEach { analyzerResult ->
            if (config.storeAnalyzerResults) {
                val outputFile = config.outputPath?.resolve("${Date().time}-graphResult.json")?.toFile()
                withContext(Dispatchers.IO) {
                    outputFile?.createNewFile()
                    val json = Json { prettyPrint = false }
                    val jsonString =
                        json.encodeToString(DependencyGraphDto.serializer(), analyzerResult.dependencyGraphDto)
                    outputFile?.writeText(jsonString)
                }
            }
            if (config.storeAnalyzerResultsInDb) {
                dbQuery {
                    AnalyzerResult.new {
                        result = analyzerResult
                    }
                }
            }
        }
    }

    if (config.storeLibyearResultsInFile) {
        val outputFileAggregate =
            config.outputPath?.resolve("graphResultAggregate-${Date().time}.json")?.toFile()

        withContext(Dispatchers.IO) {
            outputFileAggregate?.createNewFile()
            val json = Json { prettyPrint = false }

            val jsonString =
                json.encodeToString(
                    AggregatedResults.serializer(),
                    aggregatedResults
                )
            outputFileAggregate?.writeText(jsonString)
        }

    }
}

