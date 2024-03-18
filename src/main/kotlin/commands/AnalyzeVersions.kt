package commands

import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.parameters.groups.cooccurring
import com.github.ajalt.clikt.parameters.options.option
import com.github.ajalt.clikt.parameters.options.required
import com.github.ajalt.clikt.parameters.types.path
import commands.options.DbOptions
import kotlinx.coroutines.runBlocking
import util.DbConfig
import util.initSqlLiteDb
import vulnerabilities.VulnerabilityAnalyzer

class AnalyzeVersions : CliktCommand() {

    private val dbOptions by DbOptions().cooccurring()

    private val inputPath by option(
        envvar = "INPUT_PATH",
        help = "Path to the folder in which the combined version and vulnerability information are stored."
    )
        .path(mustExist = false, canBeFile = false)
        .required()

    override fun run() = runBlocking {

        val dbConfig = dbOptions?.let {
            DbConfig(
                url = it.dbUrl,
                userName = it.userName,
                password = it.password
            )
        }
        if (dbConfig != null) {
            initSqlLiteDb(dbConfig)
            val vulnerabilityAnalyzer =
                VulnerabilityAnalyzer(inputPath)
            vulnerabilityAnalyzer.dbExport()
        }
//        val vulnerabilityAnalyzer =
//            VulnerabilityAnalyzer(inputPath)
////        vulnerabilityAnalyzer.analyze()
//        val histogram = vulnerabilityAnalyzer.histogram()
//        Visualizer.createAndStoreHistogram(
//            histogram,
//            inputPath.resolve("histogram-noOutlierGreater900.png").toString()
//        )
    }
}
