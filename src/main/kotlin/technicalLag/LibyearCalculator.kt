package technicalLag


import dependencies.model.DependencyGraphDto
import kotlinx.serialization.Serializable
import java.io.File


//data class LibyearConfig(
//    val projectPath: File,
//)
//
//@Serializable
//data class LibyearStatsSimulation(
//    val current: LibyearStats,
//    val minor: LibyearStats?,
//    val patch: LibyearStats?,
//    val major: LibyearStats?
//)



class LibyearCalculator {


//    fun calculateLibyearsForNode(dependencyGraphDto: DependencyGraphDto) {
//        dependencyGraphDto.packageManagerToScopes.forEach { (pkgManager, scopes) ->
//            scopes.scopesToDependencies.forEach { (scope, deps) ->
//                deps.forEach { dep ->
//                    val libyear = calculateDifferenceForPackage(dep.usedVersion, dep.versions)
//
//                }
//            }
//        }
//    }
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

}
