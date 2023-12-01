package libyears


import artifact.model.ArtifactDto
import artifact.model.VersionDto
import dependencies.model.DependencyGraphDto
import java.time.ZoneId
import java.time.temporal.ChronoUnit
import java.util.*

object LibyearCalculator {

    fun printDependencyGraph(dependencyGraphDto: DependencyGraphDto) {
        dependencyGraphDto.packageManagerToScopes.forEach { (packageManager, scopes) ->
            println("\n\nLibyears for $packageManager")
            scopes.scopesToDependencies.forEach { (scope, artifacts) ->
                println("Libyears in scope $scope")
                val directDependencies = artifacts.filter {
                    it.libyear != null && it.isTopLevelDependency
                }.sumOf { it.libyear!! }
                println(
                    "Direct dependency libyears: $directDependencies Days " +
                            "(equals to roughly ${directDependencies / 365.25} years)"
                )

                val transitiveDependencySum = artifacts.sumOf {
                    it.transitiveDependencies.sumOf { transitive -> calculateTransitiveLibyears(transitive) }
                }
                println(
                    "Transitive dependency libyears: $transitiveDependencySum Days " +
                            "(equals to roughly ${transitiveDependencySum / 365.25} years)"
                )
            }
        }

        println("Warnings for dependencies older than 180 days:")
        dependencyGraphDto.packageManagerToScopes.values.forEach {
            it.scopesToDependencies.values.forEach {
                it.forEach { artifact ->
                    printLibyearWarning(artifact)
                }
            }
        }
    }

    fun getNewestVersion(packageList: List<VersionDto>): VersionDto? {
        // If available we use the release date of the default version for comparison
        // as this is the recommended version of the maintainers
        val newestVersionByDate = packageList.maxByOrNull { it.releaseDate }
        val defaultVersion = packageList.filter { it.isDefault }

        return if(defaultVersion.count() == 1) {
            defaultVersion.first()
        } else {
            newestVersionByDate
        }
    }

    fun calculateDifferenceForPackage(currentVersion: String, packageList: List<VersionDto>): Long? {
        if (packageList.isNotEmpty()) {
            val currentPackage = packageList.filter { it.versionNumber == currentVersion }
            if (currentPackage.isNotEmpty()) {

                val newestVersion = getNewestVersion(packageList)

                if (newestVersion != null) {
                    val currentVersionTime = Date(currentPackage.first().releaseDate)
                    val newestVersionTime = Date(newestVersion.releaseDate)


                    println("Library Difference $currentVersionTime $newestVersionTime")
                    val startLocalDate = newestVersionTime.toInstant().atZone(ZoneId.systemDefault()).toLocalDate()
                    val endLocalDate = currentVersionTime.toInstant().atZone(ZoneId.systemDefault()).toLocalDate()

                    val differenceInDays = ChronoUnit.DAYS.between(startLocalDate, endLocalDate)
                    println("Differences in days: $differenceInDays")

                    return differenceInDays
                }
            }
        }
        return null
    }

    private fun printLibyearWarning(artifact: ArtifactDto) {
        if (artifact.libyear != null && artifact.libyear < -180) {
            println(
                "Dependency ${artifact.groupId}/${artifact.artifactId}" +
                        "is ${artifact.libyear} days old."
            )
            val newestVersion = getNewestVersion(artifact.versions)
            println(
                "The used version is ${artifact.usedVersion} and " +
                        "the newest version ${newestVersion?.versionNumber}"
            )
        }
        artifact.transitiveDependencies.forEach { printLibyearWarning(it) }
    }

    private fun calculateTransitiveLibyears(artifact: ArtifactDto): Long {
        var sumLibyears = artifact.libyear ?: 0

        for (dependency in artifact.transitiveDependencies) {
            sumLibyears += calculateTransitiveLibyears(dependency)
        }

        return sumLibyears
    }
}
