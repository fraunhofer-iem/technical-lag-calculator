package libyears


import artifact.model.ArtifactDto
import artifact.model.VersionDto
import dependencies.model.DependencyGraphDto
import io.github.z4kn4fein.semver.Version
import io.github.z4kn4fein.semver.toVersion
import libyears.model.*
import org.apache.logging.log4j.kotlin.logger
import util.TimeHelper.getDifferenceInDays


object LibyearCalculator {

    fun printDependencyGraph(dependencyGraphDto: DependencyGraphDto): LibyearSumsForPackageManagerAndScopes {
        val packageManagerToScopes: MutableMap<String, MutableMap<String, LibyearsAndDependencyCount>> = mutableMapOf()

        dependencyGraphDto.packageManagerToScopes.forEach { (packageManager, scopes) ->
            logger.info { "\n\nLibyears for $packageManager" }
            packageManagerToScopes[packageManager] = mutableMapOf()
            scopes.scopesToDependencies.forEach { (scope, artifacts) ->
                logger.info { "Libyears in scope $scope" }

                val directDependencies = artifacts.filter {
                    it.libyearResult.libyear != null && it.isTopLevelDependency
                }

                val directResult = LibyearSumsResult(
                    libyears = directDependencies.sumOf { it.libyearResult.libyear!! },
                    numberOfDependencies = directDependencies.count()
                )

                logger.info {
                    "Direct dependency libyears: ${directResult.libyears} days " +
                            "and ${directResult.numberOfDependencies} dependencies."
                }

                val transitiveDependencyResult = artifacts.map {
                    calculateTransitiveLibyearsAndCount(it)
                }

                val transitiveDependencySum = transitiveDependencyResult.sumOf { it.libyears }
                val transitiveDependencyCount = transitiveDependencyResult.sumOf { it.numberOfDependencies }
                val transitiveResult = LibyearSumsResult(
                    libyears = transitiveDependencySum,
                    numberOfDependencies = transitiveDependencyCount
                )
                logger.info {
                    "Direct dependency libyears: $transitiveDependencySum days " +
                            "and $transitiveDependencyCount dependencies."
                }

                packageManagerToScopes[packageManager]?.set(
                    scope,
                    LibyearsAndDependencyCount(direct = directResult, transitive = transitiveResult)
                )
            }
        }

        logger.info { "Warnings for dependencies older than 180 days:" }
        dependencyGraphDto.packageManagerToScopes.values.forEach {
            it.scopesToDependencies.values.forEach {
                it.forEach { artifact ->
                    printLibyearWarning(artifact)
                }
            }
        }

        return LibyearSumsForPackageManagerAndScopes(packageManagerToScopes.mapValues { it.value.toMap() })
    }

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

    fun calculateDifferenceForPackage(currentVersion: VersionDto, packageList: List<VersionDto>): LibyearResultDto {

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
                LibyearResultDto(libyear = differenceInDays, status = newestVersion.first)
            } else {
                LibyearResultDto(libyear = 0, status = LibyearStatus.NEWER_THAN_DEFAULT)
            }
        }

        return LibyearResultDto(status = LibyearStatus.NO_RESULT)
    }

    private fun printLibyearWarning(artifact: ArtifactDto) {
        if (artifact.libyearResult.libyear != null && artifact.libyearResult.libyear < -180) {
            logger.warn {
                "Dependency ${artifact.groupId}/${artifact.artifactId}" +
                        "is ${artifact.libyearResult} days old."
            }
            val newestVersion = getNewestVersion(artifact.versions)
            logger.warn {
                "The used version is ${artifact.usedVersion} and " +
                        "the newest version ${newestVersion.second.versionNumber}"
            }
        }
        artifact.transitiveDependencies.forEach { printLibyearWarning(it) }
    }

    private fun calculateTransitiveLibyearsAndCount(artifact: ArtifactDto): LibyearSumsResult {
        var sumLibyears = artifact.libyearResult.libyear ?: 0
        var transitiveDependencyCount = 0

        for (dependency in artifact.transitiveDependencies) {
            val result = calculateTransitiveLibyearsAndCount(dependency)
            sumLibyears += result.libyears
            transitiveDependencyCount += 1 + result.numberOfDependencies
        }

        return LibyearSumsResult(
            libyears = sumLibyears,
            numberOfDependencies = transitiveDependencyCount
        )
    }
}
