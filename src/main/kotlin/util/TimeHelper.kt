package util

import org.apache.logging.log4j.kotlin.logger
import java.time.OffsetDateTime
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit
import java.util.*
import java.util.concurrent.TimeUnit
import kotlin.math.abs


object TimeHelper {
    fun dateToMs(dateString: String): Long {
        val formatter: DateTimeFormatter = DateTimeFormatter.ISO_OFFSET_DATE_TIME
        val dateTime: OffsetDateTime = OffsetDateTime.parse(dateString, formatter)
        return dateTime.toInstant().toEpochMilli()
    }

    fun msToDateString(ms: Long): String {
        return Date(ms).toInstant().toString()
    }

    fun getDifferenceInDays(currentVersion: Long, newestVersion: Long): Long {

        val currentVersionTime = Date(currentVersion).toInstant()
        val newestVersionTime = Date(newestVersion).toInstant()


        logger.debug { "Library Difference $currentVersionTime $newestVersionTime" }

        val differenceInDays = ChronoUnit.DAYS.between(newestVersionTime, currentVersionTime)
        logger.debug { "Differences in days: $differenceInDays" }
        return differenceInDays
    }

    fun isWithinOneYear(date1: Long, date2: Long): Boolean {
        val oneYearInMilliseconds = TimeUnit.DAYS.toMillis(365)
        return abs(date1 - date2) <= oneYearInMilliseconds
    }
}