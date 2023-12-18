package util

import java.time.OffsetDateTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit
import java.util.*

object TimeHelper {
    fun dateToMs(dateString: String): Long {
        val formatter: DateTimeFormatter = DateTimeFormatter.ISO_OFFSET_DATE_TIME
        val dateTime: OffsetDateTime = OffsetDateTime.parse(dateString, formatter)
        return dateTime.toInstant().toEpochMilli()
    }

    fun getDifferenceInDays(currentVersion: Long, newestVersion: Long): Long {
        val currentVersionTime = Date(currentVersion)
        val newestVersionTime = Date(newestVersion)


        println("Library Difference $currentVersionTime $newestVersionTime")
        val startLocalDate = newestVersionTime.toInstant().atZone(ZoneId.systemDefault()).toLocalDate()
        val endLocalDate = currentVersionTime.toInstant().atZone(ZoneId.systemDefault()).toLocalDate()

        val differenceInDays = ChronoUnit.DAYS.between(startLocalDate, endLocalDate)
        println("Differences in days: $differenceInDays")
        return differenceInDays
    }
}