package libyears


import artifact.ArtifactService
import artifact.model.ArtifactDto
import artifact.model.VersionDto
import artifact.model.VersionTypes
import dependencies.DependencyAnalyzer
import dependencies.model.AnalyzerResultDto
import io.github.z4kn4fein.semver.Version
import io.github.z4kn4fein.semver.toVersion
import kotlinx.serialization.Serializable
import libyears.model.LibyearResultDto
import libyears.model.LibyearStatus
import util.TimeHelper.getDifferenceInDays
import java.io.File
import kotlin.math.pow
import kotlin.math.sqrt


data class LibyearConfig(
    val projectPath: File,
)

@Serializable
data class LibyearStatsSimulation(
    val current: LibyearStats,
    val minor: LibyearStats?,
    val patch: LibyearStats?,
    val major: LibyearStats?
)

@Serializable
data class LibyearStats(
    val libyear: Long,
    val transitiveLibyears: Long = 0L,
    val numberOfTransitiveDependencies: Int = 0,
    val avgLibyears: Double = libyear.toDouble(),
    val stdDev: Double = 0.0,
    val variance: Double = 0.0,
    val libyearSum: Long = 0L
)

class LibyearCalculator {

    private val dependencyAnalyzer = DependencyAnalyzer()
    private val artifactService = ArtifactService()
//    suspend fun run(
//        config: LibyearConfig
//    ) {
//        dependencyAnalyzer.getAnalyzerResult(config.projectPath)?.let { dependencyAnalyzerResult ->
//            dependencyAnalyzerResult.dependencyGraphDto.packageManagerToScopes.forEach { (pkgManager, scope) ->
//                scope.scopesToDependencies.forEach { (scopeName, directDependencies) ->
//
//                    val libyearsFlatted = directDependencies.flatMap { flattenTree(it) }
//                    val flatFiltered = libyearsFlatted.filter { it != 0L }
//
//
//
//
//
//                    val aggregated = if (flatFiltered.isEmpty()) {
//                        LibyearStats(
//                        libyear = 0,
//                        transitiveLibyears = flatFiltered.sum(),
//                        numberOfTransitiveDependencies = libyearsFlatted.count()
//                    )
//                    } else {
//                        val mean = flatFiltered.average()
//                        val variance = flatFiltered.map { (it - mean).pow(2) }.average()
//                        val stdDev = sqrt(variance)
//                        println("Overall scores for scope $scopeName: Avg: $mean, stdDev: $stdDev, no elements: ${flatFiltered.count()}")
//                        LibyearStats(
//                            libyear = 0,
//                            avgLibyears = mean,
//                            variance = variance,
//                            stdDev = stdDev,
//                            transitiveLibyears = flatFiltered.sum(),
//                            numberOfTransitiveDependencies = libyearsFlatted.count()
//                        )
//                    }
//
//                    println(aggregated)
//
//                    val simulatedUpdates = directDependencies.map { dep ->
//                        val highestPossibleMinor =
//                            getApplicableVersion(dep.usedVersion, dep.versions, VersionTypes.Minor)
//                        val highestPossibleMajor =
//                            getApplicableVersion(dep.usedVersion, dep.versions, VersionTypes.Major)
//                        val highestPossiblePatch =
//                            getApplicableVersion(dep.usedVersion, dep.versions, VersionTypes.Patch)
//                        println("Update possibilities $highestPossiblePatch, $highestPossibleMajor, $highestPossibleMinor")
//
//                        val current = getSubtreeLibyearStats(dep)
//
//                        val updatedSubTreeMinor = artifactService.getDependencyTreeForPkg(
//                            pkgManager,
//                            dep.groupId,
//                            dep.artifactId,
//                            highestPossibleMinor.toString()
//                        )
//
//                        val minor = updatedSubTreeMinor?.let {
//                            getSubtreeLibyearStats(it)
//                        }
//
//                        val updatedSubTreePatch = artifactService.getDependencyTreeForPkg(
//                            pkgManager,
//                            dep.groupId,
//                            dep.artifactId,
//                            highestPossiblePatch.toString()
//                        )
//                        val patch = updatedSubTreePatch?.let {
//                            getSubtreeLibyearStats(it)
//                        }
//
//                        val updatedSubTreeMajor = artifactService.getDependencyTreeForPkg(
//                            pkgManager,
//                            dep.groupId,
//                            dep.artifactId,
//                            highestPossibleMajor.toString()
//                        )
//
//                        val major = updatedSubTreeMajor?.let {
//                            getSubtreeLibyearStats(it)
//                        }
//
//                        Pair(
//                            dep,
//                            LibyearStatsSimulation(
//                                current = current,
//                                minor = minor,
//                                major = major,
//                                patch = patch,
//                            )
//                        )
//                    }
//
//                    // updating which direct dependency provides the greatest improvement?
//                    val highestImprovement = simulatedUpdates.maxBy { simulation ->
//
//                        listOf(simulation.second.patch, simulation.second.minor, simulation.second.major).mapNotNull { it }.maxOf {
//                            simulation.second.current.libyearSum - it.libyearSum
//                        }
//                    }
//                    println("Highest improvement $highestImprovement")
//                }
//            }
//
//        }
//    }


//    private fun getSubtreeLibyearStats(artifact: ArtifactDto): LibyearStats {
//        val libyear = artifact.libyearResult.libyear ?: 0
//        // This is important ! We decided to filter out every current library with no libyear attached
//        // to get a better idea of the std. dev. of the libraries which have libyears
//        val flattenTree = flattenTree(artifact = artifact)
//        val flatFiltered = flattenTree.filter { it != 0L }
//        if (flatFiltered.isEmpty()) {
//            return LibyearStats(
//                libyear = libyear,
//                numberOfTransitiveDependencies = flattenTree.count(),
//            )
//        }
//        val mean = flatFiltered.average()
//        val variance = flatFiltered.map { (it - mean).pow(2) }.average()
//        val stdDev = sqrt(variance)
//
//        println("Libyears for Subtree with version: ${artifact.usedVersion.versionNumber}")
//        println("Values $flatFiltered")
//        println("Avg: $mean, variance: $variance, stdDev: $stdDev, no elements: ${flatFiltered.count()}")
//
//        val transitiveLibyears = flatFiltered.sum()
//        return LibyearStats(
//            libyear = libyear,
//            avgLibyears = mean,
//            variance = variance,
//            stdDev = stdDev,
//            transitiveLibyears = transitiveLibyears,
//            numberOfTransitiveDependencies = flattenTree.count(),
//            libyearSum = libyear + transitiveLibyears
//        )
//    }
//
//    private fun flattenTree(
//        artifact: ArtifactDto,
//        libyears: MutableList<Long> = mutableListOf()
//    ): MutableList<Long> {
//        artifact.libyearResult.libyear?.let { libyears.add(it) }
//        artifact.transitiveDependencies.forEach { flattenTree(it, libyears) }
//        return libyears
//    }

    /**
     * Returns the highest matching version from the given versions array with the same version type as the
     * given version's type.
     */
    private fun getApplicableVersion(version: VersionDto, versions: List<VersionDto>, type: VersionTypes): String? {
        val semvers = versions.map { it.versionNumber.toVersion(strict = false) }
        val semver = version.versionNumber.toVersion(strict = false)

        val highestVersion = when (type) {
            VersionTypes.Minor -> {
                semvers.filter { it.isStable && it.major == semver.major }
                    .maxWithOrNull(compareBy({ it.minor }, { it.patch }))
            }

            VersionTypes.Major -> {
                semvers.filter { it.isStable }
                    .maxWithOrNull(compareBy({ it.major }, { it.minor }, { it.patch }))
            }

            VersionTypes.Patch -> {
                semvers.filter { it.isStable && it.major == semver.major && it.minor == semver.minor }
                    .maxBy { it.patch }
            }
        }
        return highestVersion?.toString()
    }

    fun getAllAnalyzerResults(): List<AnalyzerResultDto> {
        return dependencyAnalyzer.getAllAnalyzerResults()
    }

    companion object {

        /**
         * Returns the newest applicable, stable version compared to the given current version.
         * If a version is explicitly tagged as default this version is used for the comparison.
         * If not the stable version with the highest version number is used.
         * Throws if the current version doesn't follow the semver format.
         */
        private fun getNewestApplicableVersion(
            currentVersion: VersionDto,
            packageList: List<VersionDto>
        ): Pair<LibyearStatus, VersionDto> {
            val current = currentVersion.versionNumber.toVersion(strict = false)
            current.isPreRelease
            val versions = if (current.isStable) {
                getSortedSemVersions(packageList).filter { it.second.isStable && !it.second.isPreRelease }
            } else {
                getSortedSemVersions(packageList).filter { !it.second.isPreRelease }
            }

            val newestVersion = versions.last()

            versions.find { it.first.isDefault }?.let { defaultVersion ->
                return if (defaultVersion.second > current) {
                    Pair(LibyearStatus.SEM_VERSION_WITH_DEFAULT, defaultVersion.first)
                } else {
                    Pair(LibyearStatus.SEM_VERSION_WITH_DEFAULT, currentVersion)
                }
            }

            if (newestVersion.second > current) {
                return Pair(LibyearStatus.SEM_VERSION_WITHOUT_DEFAULT, newestVersion.first)
            }
            return Pair(LibyearStatus.SEM_VERSION_WITHOUT_DEFAULT, currentVersion)
        }

        private fun getSortedSemVersions(packageList: List<VersionDto>): List<Pair<VersionDto, Version>> {
            return packageList.mapNotNull {
                try {
                    Pair(it, it.versionNumber.toVersion(strict = false))
                } catch (exception: Exception) {
                    null
                }
            }.sortedBy { it.second }
        }

        private fun getNewestVersion(packageList: List<VersionDto>): Pair<LibyearStatus, VersionDto> {
            // If available we use the release date of the default version for comparison
            // as this is the recommended version of the maintainers
            val newestVersionByDate = packageList.maxBy { it.releaseDate }
            val defaultVersion = packageList.filter { it.isDefault }

            return if (defaultVersion.count() == 1) {
                Pair(LibyearStatus.DATE_WITH_DEFAULT, defaultVersion.first())
            } else {
                Pair(LibyearStatus.DATE_WITHOUT_DEFAULT, newestVersionByDate)
            }
        }

        fun calculateDifferenceForPackage(
            currentVersion: VersionDto,
            packageList: List<VersionDto>
        ): LibyearResultDto {

            if (packageList.contains(currentVersion) && currentVersion.releaseDate != -1L) {
                val newestVersion = try {
                    getNewestApplicableVersion(currentVersion, packageList)
                } catch (exception: Exception) {
                    getNewestVersion(packageList)
                }

                val differenceInDays = getDifferenceInDays(
                    currentVersion = currentVersion.releaseDate,
                    newestVersion = newestVersion.second.releaseDate
                )

                return if (differenceInDays <= 0) {
                    LibyearResultDto(libyear = -1 * differenceInDays, status = newestVersion.first)
                } else {
                    LibyearResultDto(libyear = 0, status = LibyearStatus.NEWER_THAN_DEFAULT)
                }
            }

            return LibyearResultDto(status = LibyearStatus.NO_RESULT)
        }


    }
}
