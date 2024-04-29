import ch.qos.logback.classic.Level
import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.core.subcommands
import com.github.ajalt.clikt.parameters.options.default
import com.github.ajalt.clikt.parameters.options.flag
import com.github.ajalt.clikt.parameters.options.option
import com.github.ajalt.clikt.parameters.options.switch
import com.github.ajalt.clikt.parameters.types.path
import dependencies.GenerateDependencyTree
import libyears.Libyears
import org.apache.logging.log4j.kotlin.logger
import org.slf4j.MDC
import util.configureRootLogger
import java.util.*
import kotlin.io.path.createDirectories
import kotlin.io.path.pathString
import kotlin.time.measureTime


class Tool : CliktCommand() {

    private val logLevel: Level by option(help = "Set the verbosity level of log output.").switch(
        "--error" to Level.ERROR,
        "--warn" to Level.WARN,
        "--info" to Level.INFO,
        "--debug" to Level.DEBUG
    ).default(Level.INFO)

    private val logPath by option(
        envvar = "OUTPUT_PATH", help = "Path to the folder to store the JSON results" +
                "of the created dependency graph. If the path doesn't exist it will be created."
    )
        .path(mustExist = false, canBeFile = false)

    private val silent by option().flag(default = false)

    override fun run() {
        echo("Starting tool and setting up logging")


        if (logPath != null) {
            val outputPathWrapper = logPath!!.resolve("libyearLogs-${Date().time}")
            outputPathWrapper.createDirectories()
            val defaultLogPath = outputPathWrapper.toAbsolutePath().pathString
            MDC.put("outputFile", defaultLogPath)
            if (silent) {
                configureRootLogger(logLevel, "file")
            } else {
                configureRootLogger(logLevel)
            }
        } else {
            if (silent) {
                configureRootLogger(logLevel, "off")
            } else {
                configureRootLogger(logLevel, "console")
            }
        }

        logger.info { "Logging setup completed." }
    }
}


fun main(args: Array<String>) {
    val runtime = measureTime {
        val tool = Tool()
        tool.subcommands(GenerateDependencyTree(), Libyears()).main(args)
    }
    println("Tool run took $runtime")
}
