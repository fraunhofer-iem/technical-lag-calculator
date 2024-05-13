package util

import dependencies.ProjectPaths
import dependencies.db.AnalyzerResult
import dependencies.model.AnalyzerResultDto
import dependencies.model.DependencyGraphDto
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import technicalLag.model.AggregatedResults
import visualization.Visualizer
import java.io.File
import java.nio.file.Path
import java.util.*

data class StorageConfig(
    val dbConfig: DbConfig?,
    val outputPath: Path?,
    val storeAnalyzerResultsInFile: Boolean,
    val storeAnalyzerResultsInDb: Boolean,
    val storeLibyearResultsInFile: Boolean,
    val storeLibyearResultsInDb: Boolean,
    val storeLibyearGraphs: Boolean,
) {
    val storeAnalyzerResults = storeAnalyzerResultsInFile || storeAnalyzerResultsInDb
}


class StoreResultHelper {
    companion object {
        private val json = Json { prettyPrint = false }

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

    if (config.storeLibyearGraphs) {
        val graphResults: MutableMap<String, MutableList<Long>> = mutableMapOf()
        aggregatedResults.results.forEach {
            it.packageManagerToScopes.forEach { (packageManager, scopeToLibyears) ->
                scopeToLibyears.forEach { (scope, libyearResult) ->
                    val keyTransitive = "$packageManager-$scope-transitive"
                    val keyDirect = "$packageManager-$scope-direct"
                    val keyDirectPerDependency = "$packageManager-$scope-direct-per-dependency"
                    val keyTransitivePerDependency = "$packageManager-$scope-transitive-per-dependency"
                    val keyDirectDependencies = "$packageManager-$scope-direct-number-dependencies"
                    val keyTransitiveDependencies = "$packageManager-$scope-transitive-number-dependencies"
                    fun addToResult(key: String, value: Long) {
                        if (!graphResults.contains(key)) {
                            graphResults[key] = mutableListOf(value)
                        } else {
                            graphResults[key]?.add(value)
                        }
                    }
                    addToResult(keyDirect, libyearResult.direct.libyears)
                    addToResult(keyTransitive, libyearResult.transitive.libyears)
                    addToResult(
                        keyDirectPerDependency,
                        libyearResult.direct.libyears / libyearResult.direct.numberOfDependencies
                    )
                    addToResult(
                        keyTransitivePerDependency,
                        libyearResult.transitive.libyears / libyearResult.transitive.numberOfDependencies
                    )
                    addToResult(keyTransitiveDependencies, libyearResult.transitive.numberOfDependencies.toLong())
                    addToResult(keyDirectDependencies, libyearResult.direct.numberOfDependencies.toLong())
                }
            }
        }

        graphResults.forEach { (name, values) ->
            Visualizer.createAndStoreLineDiagram(
                outputFilePath = config.outputPath?.resolve("$name.png")?.toAbsolutePath().toString(),
                name = name,
                values = values.toList(),
            )
        }

    }
}

