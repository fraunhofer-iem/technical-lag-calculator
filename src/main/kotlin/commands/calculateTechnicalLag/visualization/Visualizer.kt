package commands.calculateTechnicalLag.visualization

import org.jetbrains.kotlinx.dataframe.api.toDataFrame
import org.jetbrains.kotlinx.kandy.dsl.plot
import org.jetbrains.kotlinx.kandy.letsplot.export.save
import org.jetbrains.kotlinx.kandy.letsplot.feature.layout
import org.jetbrains.kotlinx.kandy.letsplot.layers.bars
import org.jetbrains.kotlinx.kandy.letsplot.layers.line
import org.jetbrains.kotlinx.kandy.util.color.Color
import org.jetbrains.kotlinx.statistics.kandy.layers.boxplot
import java.nio.file.Path
import java.util.*

object Visualizer {

    fun createAndStoreHistogram(histogram: Map<Long, Int>, outputFilePath: String) {
        plot {
            bars {
                x(histogram.keys)
                y(histogram.values)
            }
            layout.title = "Vulnerability Published Date over Time"
        }.save(outputFilePath)
    }

    data class ScopeLibday(val scope: String, val libday: Long)
    data class TechnicalLagVisualization(
        val scope: String,
        val libday: Long,
        val repository: String,
        val packageIdent: String,
        val version: String
    )

    fun createAndStoreBoxplot(data: Map<String, List<Long>>, outputPath: Path) {

        val df = data.flatMap { (key, value) ->
            value.map { ScopeLibday(key, it) }
        }.toDataFrame()

        val outputFilePath = outputPath.toAbsolutePath().resolve("${Date().time}-boxpot-test.png").toString()
        df.plot {
            boxplot("scope", "libday") {
                boxes {
                    borderLine.color = Color.RED
                }
            }
        }.save(outputFilePath)
    }

    fun createAndStoreLineDiagram(
        outputFilePath: String,
        name: String,
        months: List<String> = listOf(
            "Jan",
            "Feb",
            "Mar",
            "Apr",
            "May",
            "Jun",
            "Jul",
            "Aug",
            "Sep",
            "Oct",
            "Nov",
            "Dec"
        ),
        values: List<Long>
    ) {
        val mutableValues = values.toMutableList()
        val mutableMonths = months.toMutableList()

        fun makeListsEqual() {
            while (mutableValues.size < mutableMonths.size) {
                mutableMonths.removeLast()
            }

            while (mutableMonths.size < mutableValues.size) {
                mutableMonths.add("")
            }
        }

        makeListsEqual()
        plot {
            line {
                x(mutableMonths)
                y(mutableValues)
            }
            layout.title = name
        }.save(outputFilePath)
    }
}
