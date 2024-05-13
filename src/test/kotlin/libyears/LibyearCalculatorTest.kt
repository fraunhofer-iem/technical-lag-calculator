//package libyears
//
//import artifact.model.VersionDto
//import org.junit.jupiter.api.Test
//import technicalLag.LibyearCalculator
//import util.TimeHelper.dateToMs
//
//import kotlin.test.assertEquals
//
//class LibyearCalculatorTest {
//
//    @Test
//    fun calculateDifferenceForPackage() {
//        val currentVersion = "5.0.4"
//
//        val libyearNull = LibyearCalculator.calculateDifferenceForPackage(
//            currentVersion = VersionDto(
//                versionNumber = currentVersion,
//                isDefault = true
//            ),
//            packageList = emptyList()
//        )
//
//        assertEquals(expected = null, actual = libyearNull.libyear)
//
//        val currentVersionRelease = "2023-05-03T19:52:39Z"
//        val packageList = mutableListOf(
//            VersionDto(
//                versionNumber = currentVersion,
//                releaseDate = dateToMs(currentVersionRelease),
//                isDefault = true
//            )
//        )
//
//        val libyear0 = LibyearCalculator.calculateDifferenceForPackage(
//            currentVersion = VersionDto(
//                versionNumber = currentVersion,
//                releaseDate = dateToMs(currentVersionRelease),
//                isDefault = true
//            ),
//            packageList = packageList
//        )
//
//        assertEquals(expected = 0, actual = libyear0.libyear)
//
//        val libyearVersionNotFound = LibyearCalculator.calculateDifferenceForPackage(
//            currentVersion = VersionDto("InvalidVersion"),
//            packageList = packageList
//        )
//
//        assertEquals(expected = null, actual = libyearVersionNotFound.libyear)
//
//        packageList.clear()
//        val newerRelease = "2023-05-04T19:52:39Z"
//        packageList.addAll(
//            listOf(
//                VersionDto(
//                    versionNumber = currentVersion,
//                    releaseDate = dateToMs(currentVersionRelease),
//                    isDefault = false
//                ),
//                VersionDto(
//                    versionNumber = "5.0.5",
//                    releaseDate = dateToMs(newerRelease),
//                    isDefault = false
//                )
//            )
//        )
//
//        val libyearNewerNoDefault = LibyearCalculator.calculateDifferenceForPackage(
//            currentVersion = VersionDto(
//                versionNumber = currentVersion,
//                releaseDate = dateToMs(currentVersionRelease),
//                isDefault = false
//            ),
//            packageList = packageList
//        )
//
//        assertEquals(expected = 1, actual = libyearNewerNoDefault.libyear)
//    }
//}