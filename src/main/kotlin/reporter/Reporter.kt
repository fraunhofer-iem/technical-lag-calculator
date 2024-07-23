package reporter

import commands.calculateTechnicalLag.visualization.Visualizer
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import org.jetbrains.kotlinx.dataframe.DataFrame
import org.jetbrains.kotlinx.dataframe.io.writeCSV
import shared.analyzerResultDtos.AnalyzerResultDto
import shared.project.ProjectPaths
import java.io.File
import java.nio.file.Path
import java.util.*


class Reporter(outputPath: Path) {

    private val json = Json { prettyPrint = true }
    private val outputFile = outputPath.toAbsolutePath().toFile()

    private val analyzerResultFile by lazy {
        createFileInOutputPath("${Date().time}-analyzerResult.json")
    }
    private val htmlReportFile by lazy {
        createFileInOutputPath("index.html")
    }
    private val csvFileName = "csvReport.csv"
    private val csvReportFile by lazy {
        outputPath.resolve(csvFileName)
    }

    private fun createFileInOutputPath(fileName: String): File {
        val file = outputFile.resolve(fileName)
        file.createNewFile()
        return file
    }

    fun storeAnalyzerResultInFile(
        result: AnalyzerResultDto,
    ) {
        val jsonString =
            json.encodeToString(AnalyzerResultDto.serializer(), result)
        analyzerResultFile.writeText(jsonString)
    }

    fun storeCsvExport(df: DataFrame<Visualizer.TechnicalLagExport>) {
        df.writeCSV(csvReportFile.toString())
    }

    fun generateHtmlReport(
    ) {
        val resource =
            this.javaClass.classLoader.getResource("html-report-template.html") ?: return

        val htmlReportTemplate = resource.readText(Charsets.UTF_8)
        val htmlReport = htmlReportTemplate.replace("{{ PATH_TO_STATISTICS }}", "\"$csvFileName\"")

        htmlReportFile.writeText(htmlReport, Charsets.UTF_8)
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
