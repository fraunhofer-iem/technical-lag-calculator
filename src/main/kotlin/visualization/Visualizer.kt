package visualization

import org.jetbrains.kotlinx.kandy.dsl.plot
import org.jetbrains.kotlinx.kandy.letsplot.export.save
import org.jetbrains.kotlinx.kandy.letsplot.feature.layout
import org.jetbrains.kotlinx.kandy.letsplot.layers.line

object Visualizer {


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