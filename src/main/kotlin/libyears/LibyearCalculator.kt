package libyears


import artifact.model.ArtifactDto
import artifact.model.VersionDto
import dependencies.model.DependencyGraphDto
import io.github.z4kn4fein.semver.Version
import io.github.z4kn4fein.semver.toVersion
import util.TimeHelper.getDifferenceInDays

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

    /**
     * Returns the newest applicable, stable version compared to the given current version.
     * If a version is explicitly tagged as default this version is used for the comparison.
     * If not the stable version with the highest version number is used.
     * Throws if the current version doesn't follow the semver format.
     */
    private fun getNewestApplicableVersion(currentVersion: VersionDto, packageList: List<VersionDto>): VersionDto {
        val current = currentVersion.versionNumber.toVersion(strict = false)
        val versions = getSortedSemVersions(packageList).filter { it.second.isStable }
        val newestVersion = versions.last()

        versions.find { it.first.isDefault }?.let { defaultVersion ->
            return if (defaultVersion.second > current) {
                defaultVersion.first
            } else {
                currentVersion
            }
        }

        if (newestVersion.second > current) {
            return newestVersion.first
        }
        return currentVersion
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

    private fun getNewestVersion(packageList: List<VersionDto>): VersionDto? {
        // If available we use the release date of the default version for comparison
        // as this is the recommended version of the maintainers
        val newestVersionByDate = packageList.maxByOrNull { it.releaseDate }
        val defaultVersion = packageList.filter { it.isDefault }

        return if (defaultVersion.count() == 1) {
            defaultVersion.first()
        } else {
            newestVersionByDate
        }
    }

    fun calculateDifferenceForPackage(currentVersion: VersionDto, packageList: List<VersionDto>): Long? {
        if(packageList.contains(currentVersion) && currentVersion.releaseDate != -1L) {
            val newestVersion = try {
                getNewestApplicableVersion(currentVersion, packageList)
            } catch (exception: Exception) {
                getNewestVersion(packageList)
            }

            if (newestVersion != null) {
                val differenceInDays = getDifferenceInDays(
                    currentVersion = currentVersion.releaseDate,
                    newestVersion = newestVersion.releaseDate
                )
                // we should do further checks based upon semantic versioning, whether the
                // semversion is greater than what we are using
                return if (differenceInDays > 0) {
                    0
                } else {
                    differenceInDays
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
