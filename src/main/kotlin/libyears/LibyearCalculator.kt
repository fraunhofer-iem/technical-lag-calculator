package libyears


import artifact.model.VersionDto
import java.time.ZoneId
import java.time.temporal.ChronoUnit
import java.util.*

class LibyearCalculator {

    fun calculateDifferenceForPackage(currentVersion: String, packageList: List<VersionDto>): Long {
        return if (packageList.isNotEmpty()) {
            val currentPackage = packageList.filter { it.versionNumber == currentVersion }
            val newestVersion = packageList.maxByOrNull { it.releaseDate }

            val currentVersionTime = Date(currentPackage.first().releaseDate)
            val newestVersionTime = Date(newestVersion?.releaseDate ?: 0)


            println("Library Difference $currentVersionTime $newestVersionTime")
            val startLocalDate = newestVersionTime.toInstant().atZone(ZoneId.systemDefault()).toLocalDate()
            val endLocalDate = currentVersionTime.toInstant().atZone(ZoneId.systemDefault()).toLocalDate()

            val differenceInDays = ChronoUnit.DAYS.between(startLocalDate, endLocalDate)
            println("Differences in days: $differenceInDays")

            differenceInDays
        } else {
            0
        }
    }
}