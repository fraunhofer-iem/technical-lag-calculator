package technicalLag

import dependencies.model.Artifact
import dependencies.model.ArtifactVersion
import dependencies.model.Dependency
import dependencies.model.DependencyGraphs
import technicalLag.model.Statistics
import technicalLag.model.TechnicalLagDto
import technicalLag.model.TechnicalLagStatistics
import kotlin.math.pow
import kotlin.math.sqrt

class TechnicalLagService {

    private data class AggregateData(
        val size: Int,
        val transitiveLibDays: MutableList<Long>,
        val numberMissedReleases: MutableList<Int>,
        val releaseDistances: MutableList<Triple<Double, Double, Double>>,
    ) {
        constructor(size: Int) : this(
            size,
            ArrayList<Long>(size),
            ArrayList<Int>(size),
            ArrayList<Triple<Double, Double, Double>>(size)
        )
    }


    fun connectDependenciesToStats(graphs: DependencyGraphs) {

        graphs.graph.values.forEach { graph ->
            graph.linkedDirectDependencies.forEach { dep ->
                val size = graph.nodes.count()
                val aggregate = mapOf(
                    ArtifactVersion.VersionType.Major to AggregateData(size = size),
                    ArtifactVersion.VersionType.Minor to AggregateData(size = size),
                    ArtifactVersion.VersionType.Patch to AggregateData(size = size)
                )
                calculateAllStatistics(dep, graphs.artifacts, aggregate)
            }
        }

        graphs.graphs.values.forEach {
            it.values.forEach { graph ->
                graph.linkedDirectDependencies.forEach { dep ->
                    val size = graph.nodes.count()
                    val aggregate = mapOf(
                        ArtifactVersion.VersionType.Major to AggregateData(size = size),
                        ArtifactVersion.VersionType.Minor to AggregateData(size = size),
                        ArtifactVersion.VersionType.Patch to AggregateData(size = size)
                    )
                    calculateAllStatistics(dep, graphs.artifacts, aggregate)
                }
            }
        }
    }

    private fun calculateAllStatistics(
        dependency: Dependency,
        artifacts: List<Artifact>,
        aggregate: Map<ArtifactVersion.VersionType, AggregateData>
    ) {

        dependency.children.forEach { child ->
            calculateAllStatistics(child, artifacts, aggregate)
        }

        val artifact = artifacts[dependency.node.artifactIdx]

        ArtifactVersion.VersionType.entries.forEach { versionType ->
            val technicalLag = artifact.getTechLagForVersion(dependency.node.usedVersion, versionType)

            dependency.addStatForVersionType(
                stats = aggregateDataToStats(technicalLag, aggregate[versionType]!!),
                versionType = versionType
            )

            if (technicalLag != null) {
                aggregate[versionType]!!.transitiveLibDays.add(technicalLag.libDays)
                aggregate[versionType]!!.numberMissedReleases.add(technicalLag.numberOfMissedReleases)
                aggregate[versionType]!!.releaseDistances.add(
                    Triple(
                        technicalLag.distance.first.toDouble(),
                        technicalLag.distance.second.toDouble(),
                        technicalLag.distance.third.toDouble()
                    )
                )
            }
        }
    }

    private fun aggregateDataToStats(
        technicalLag: TechnicalLagDto? = null,
        aggregateData: AggregateData
    ): TechnicalLagStatistics {

        val libDaysStats = dataToStatistics(aggregateData.transitiveLibDays)
        val missedReleasesStats = dataToStatistics(aggregateData.numberMissedReleases.map { it.toLong() })
        val releaseDistancesStats = releaseDistanceToStatistics(aggregateData.releaseDistances)

        return TechnicalLagStatistics(
            technicalLag = technicalLag,
            missedReleases = missedReleasesStats,
            libDays = libDaysStats,
            distance = releaseDistancesStats
        )
    }

    private fun dataToStatistics(data: List<Long>): Statistics? {
        return if (data.isNotEmpty()) {
            val avgTransitiveLibyears = data.average()

            val variance: Double = data.map {
                (it - avgTransitiveLibyears).pow(
                    2
                )
            }.average()
            val stdDev: Double = sqrt(variance)
            Statistics(
                average = avgTransitiveLibyears,
                variance = variance,
                stdDev = stdDev
            )
        } else {
            null
        }
    }

    private fun releaseDistanceToStatistics(distances: List<Triple<Double, Double, Double>>): Triple<Statistics, Statistics, Statistics> {
        val avg = calculateAvgReleaseDistance(distances)
        val variance = calculateVarianceForReleaseDistance(distances, avg)
        val stdDev = Triple(sqrt(variance.first), sqrt(variance.second), sqrt(variance.third))

        return Triple(
            Statistics(
                average = avg.first,
                variance = variance.first,
                stdDev = stdDev.first
            ),
            Statistics(
                average = avg.second,
                variance = variance.second,
                stdDev = stdDev.second
            ),
            Statistics(
                average = avg.third,
                variance = variance.third,
                stdDev = stdDev.third
            )
        )
    }

    private fun calculateVarianceForReleaseDistance(
        distances: List<Triple<Double, Double, Double>>,
        avg: Triple<Double, Double, Double>
    ): Triple<Double, Double, Double> {
        val squared = distances.map {
            Triple((it.first - avg.first).pow(2), (it.second - avg.second).pow(2), (it.third - avg.third).pow(2))
        }
        return calculateAvgReleaseDistance(squared)
    }

    private fun calculateAvgReleaseDistance(distances: List<Triple<Double, Double, Double>>): Triple<Double, Double, Double> {
        return if (distances.isNotEmpty()) {
            val sumOfDistances = distances.reduce { acc, triple ->
                Triple(
                    acc.first + triple.first,
                    acc.second + triple.second,
                    acc.third + triple.third
                )
            }
            Triple(
                sumOfDistances.first / distances.size,
                sumOfDistances.second / distances.size,
                sumOfDistances.third / distances.size,
            )
        } else {
            Triple(0.0, 0.0, 0.0)
        }
    }

}
