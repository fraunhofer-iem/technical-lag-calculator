package libyears


import artifact.model.ArtifactDto
import dependencies.model.DependencyGraphDto
import artifact.model.VersionDto
import java.time.ZoneId
import java.time.temporal.ChronoUnit
import java.util.*

object LibyearCalculator {

    fun printDependencyGraph(dependencyGraphDto: DependencyGraphDto) {
        dependencyGraphDto.packageManagerToScopes.forEach { (packageManager, scopes) ->
            println("Libyears for $packageManager")
            scopes.scopesToDependencies.forEach { (scope, artifacts) ->
                println("Libyears in scope $scope")
                val directDependencies = artifacts.sumOf { it.libyear }
                println(
                    "Direct dependency libyears: $directDependencies Days " +
                            "(equals to roughly ${directDependencies / 365.25} years)"
                )

                // Here we loose the
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

    private fun printLibyearWarning(artifact: ArtifactDto) {
        if (artifact.libyear < -180) {
            println(
                "Dependency ${artifact.groupId}/${artifact.artifactId}" +
                        "is ${artifact.libyear} days old."
            )
            val newestVersion = artifact.versions.maxByOrNull { it.releaseDate }
            println(
                "The used version is ${artifact.usedVersion} and " +
                        "the newest version ${newestVersion?.versionNumber}"
            )
        }
        artifact.transitiveDependencies.forEach { printLibyearWarning(it) }
    }

    private fun calculateTransitiveLibyears(artifact: ArtifactDto): Long {
        var sumLibyears = artifact.libyear

        for (dependency in artifact.transitiveDependencies) {
            sumLibyears += calculateTransitiveLibyears(dependency)
        }

        return sumLibyears
    }
}
