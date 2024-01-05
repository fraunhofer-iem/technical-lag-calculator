package util

import ch.qos.logback.classic.Level
import ch.qos.logback.classic.Logger
import org.slf4j.LoggerFactory

const val CONSOLE_APPENDER = "STDOUT"
const val FILE_APPENDER = "SIFT"

fun configureRootLogger(logLevel: Level, logMode: String) {
    val rootLogger = LoggerFactory.getLogger(Logger.ROOT_LOGGER_NAME) as Logger
    rootLogger.level = logLevel

    when(logMode) {
        "console" -> rootLogger.detachAppender(FILE_APPENDER)
        "file" -> rootLogger.detachAppender(CONSOLE_APPENDER)
    }
}