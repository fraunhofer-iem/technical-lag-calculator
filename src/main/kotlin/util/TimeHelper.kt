package util

import java.time.OffsetDateTime
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit
import java.util.*


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


        println("Library Difference $currentVersionTime $newestVersionTime")

        val differenceInDays = ChronoUnit.DAYS.between(newestVersionTime, currentVersionTime)
        println("Differences in days: $differenceInDays")
        return differenceInDays
    }
}