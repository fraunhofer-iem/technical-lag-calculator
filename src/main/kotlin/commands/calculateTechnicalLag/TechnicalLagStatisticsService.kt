package commands.calculateTechnicalLag

import commands.calculateTechnicalLag.model.Statistics
import commands.calculateTechnicalLag.model.TechnicalLagDto
import commands.calculateTechnicalLag.model.TechnicalLagStatistics
import commands.calculateTechnicalLag.visualization.Visualizer
import shared.project.DependencyGraph
import shared.project.DependencyNode
import shared.project.IStatisticsContainer
import shared.project.Project
import shared.project.artifact.Artifact
import shared.project.artifact.LinkedDependencyNode
import shared.project.artifact.LinkedNode
import shared.project.artifact.VersionType
import kotlin.math.pow
import kotlin.math.sqrt

private data class AggregateData(
    val size: Int,
    val transitiveLibDays: MutableList<Long>,
    val numberMissedReleases: MutableList<Int>,
    val releaseDistances: MutableList<Triple<Double, Double, Double>>,
    val releaseFrequencies: MutableList<Double>,
) {
    constructor(size: Int) : this(
        size,
        ArrayList<Long>(size),
        ArrayList<Int>(size),
        ArrayList<Triple<Double, Double, Double>>(size),
        ArrayList<Double>(size)
    )
}


private class AggregateVersionTypeCollection(val size: Int) {

    private val aggregates: Map<VersionType, AggregateData> = initAggregates()

    private fun initAggregates(): Map<VersionType, AggregateData> {
        // this guarantees that the map contains an entry for each VersionType
        return VersionType.entries.associateWith { AggregateData(size = size) }
    }

    fun getAggregate(version: VersionType): AggregateData {
        return aggregates[version]!!
    }

    fun add(aggregateCollection: AggregateVersionTypeCollection) {
        VersionType.entries.forEach { versionType ->

            getAggregate(versionType).releaseDistances.addAll(
                aggregateCollection.getAggregate(versionType).releaseDistances
            )
            getAggregate(versionType).transitiveLibDays.addAll(
                aggregateCollection.getAggregate(versionType).transitiveLibDays
            )
            getAggregate(versionType).numberMissedReleases.addAll(
                aggregateCollection.getAggregate(versionType).numberMissedReleases
            )
            getAggregate(versionType).releaseFrequencies.addAll(
                aggregateCollection.getAggregate(versionType).releaseFrequencies
            )
        }
    }
}

internal class TechnicalLagStatisticsService {

    fun connectDependenciesToStats(graphs: Project) {

        graphs.graph.values.forEach { graph ->
            processGraph(graph, graphs.artifacts)
        }

        graphs.graphs.values.forEach {
            it.values.forEach { graph ->
                processGraph(graph, graphs.artifacts)
            }
        }
    }

    fun getAllLibdays(project: Project): Map<String, List<Long>> {
        return project.graph.entries.associate { (scope, graph) ->
            scope to graph.nodes.mapNotNull { node ->
                node.getAllStats()[VersionType.Major]?.technicalLag?.libDays
            }
        }
    }

    fun getTechnicalLagExport(project: Project): List<Visualizer.TechnicalLagExport> {
        return listOf(
            getAllTechnicalLagExport(project),
            getDirectTechnicalLagExport(project),
            getTransitiveTechnicalLagExport(project)
        ).flatten()
    }

    private fun getAllTechnicalLagExport(project: Project): List<Visualizer.TechnicalLagExport> {
        return project.graph.entries.flatMap { (scope, graph) ->
            graph.nodes.mapNotNull { node ->
                nodeToTechnicalLagExport(scope, node, project)
            }
        }
    }


    fun getDirectTechnicalLagExport(project: Project): List<Visualizer.TechnicalLagExport> {
        return project.graph.entries.flatMap { (scope, graph) ->
            graph.rootDependency.children.mapNotNull { child ->
                nodeToTechnicalLagExport("$scope-direct", child.node, project)
            }
        }
    }

    fun getTransitiveTechnicalLagExport(project: Project): List<Visualizer.TechnicalLagExport> {
        return project.graph.entries.flatMap { (scope, graph) ->
            graph.rootDependency.children.flatMap { directChild ->
                directChild.children.flatMap { grandChild ->
                    val childData = mutableListOf<Visualizer.TechnicalLagExport>()

                    fun childToTechnicalLagExport(child: LinkedDependencyNode) {
                        val data = nodeToTechnicalLagExport("$scope-transitive", child.node, project)
                        if (data != null) childData.add(data)
                        child.children.forEach { childToTechnicalLagExport(it) }
                    }
                    childToTechnicalLagExport(grandChild)
                    childData
                }
            }
        }
    }

    private fun nodeToTechnicalLagExport(
        scope: String,
        node: DependencyNode,
        project: Project
    ): Visualizer.TechnicalLagExport? {

        val technicalLag = node.getAllStats()[VersionType.Major]?.technicalLag ?: return null
        val artifact = project.artifacts[node.artifactIdx]
        val ident = "${artifact.groupId}/${artifact.artifactId}"


        return Visualizer.TechnicalLagExport(
            scope = scope,
            libdays = technicalLag.libDays,
            distanceMajor = technicalLag.distance.first,
            distanceMinor = technicalLag.distance.second,
            distancePatch = technicalLag.distance.third,
            releaseFrequencyPerMonth = technicalLag.releaseFrequency.releasesPerMonth,
            numberOfMissedReleases = technicalLag.numberOfMissedReleases,
            repository = "${project.groupId}/${project.artifactId}",
            version = node.usedVersion,
            packageIdent = ident,
        )

    }

    fun getAllDirectLibdays(project: Project): Map<String, List<Long>> {
        return project.graph.entries.associate { (scope, graph) ->
            val directLibdays =
                graph.rootDependency.children.mapNotNull { child -> child.node.getAllStats()[VersionType.Major]?.technicalLag?.libDays }
            scope to directLibdays
        }
    }

    private fun collectAllLibdaysFromChildren(child: LinkedDependencyNode, libyears: MutableList<Long>) {
        val libday = child.node.getAllStats()[VersionType.Major]?.technicalLag?.libDays
        if (libday != null) {
            libyears.add(libday)
        }
        child.children.forEach { collectAllLibdaysFromChildren(it, libyears) }
    }

    fun getAllTransitiveLibdays(project: Project): Map<String, List<Long>> {
        return project.graph.entries.associate { (scope, graph) ->
            val transitiveLibdays = mutableListOf<Long>()
            graph.rootDependency.children.forEach { child ->
                child.children.forEach { grandChild ->
                    collectAllLibdaysFromChildren(grandChild, transitiveLibdays)
                }
            }
            scope to transitiveLibdays
        }
    }


    private fun processGraph(graph: DependencyGraph, artifacts: List<Artifact>) {
        calculateAllStatistics(graph.rootDependency, artifacts)
        val directStats = calculateDirectStatistics(graph.rootDependency, artifacts)
        graph.addAllDirectDependencyStats(directStats)

        val transitiveStats = calculateTransitiveStatistics(graph.rootDependency, artifacts)
        graph.addAllTransitiveDependencyStats(transitiveStats)
    }

    private fun calculateDirectStatistics(root: LinkedNode, artifacts: List<Artifact>): IStatisticsContainer {
        // copy root node (we can only attach one set of stats to each node)
        // remove all grandchildren
        // aggregate the data as we do for a normal tree with "calculateAllStatistics"
        val directRoot = root.copy()
        directRoot.children.forEach { child ->
            child.clearChildren()
        }
        calculateAllStatistics(directRoot, artifacts)

        return directRoot.statContainer
    }

    private fun calculateTransitiveStatistics(root: LinkedNode, artifacts: List<Artifact>): IStatisticsContainer {
        // copy root node (we can only attach one set of stats to each node)
        val transitiveRoot = root.copy()
        // get all grandchildren
        val grandchildren = mutableListOf<LinkedDependencyNode>()
        transitiveRoot.children.forEach { child ->
            grandchildren.addAll(child.children)
        }
        // remove all children
        transitiveRoot.clearChildren()
        // link root to grandchildren
        transitiveRoot.addChildren(grandchildren)
        calculateAllStatistics(transitiveRoot, artifacts)

        return transitiveRoot.statContainer
    }

    private fun calculateAllStatistics(
        root: LinkedNode,
        artifacts: List<Artifact>,
    ) {
        val aggregateVersionTypeCollection = AggregateVersionTypeCollection(size = root.numberChildren)

        root.children.forEach { child ->
            val childAggregate = AggregateVersionTypeCollection(size = child.numberChildren)

            calculateChildStats(child, artifacts, childAggregate)
            aggregateVersionTypeCollection.add(childAggregate)
        }

        VersionType.entries.forEach { versionType ->
            root.statContainer.addStatForVersionType(
                stats = aggregateDataToStats(aggregateData = aggregateVersionTypeCollection.getAggregate(versionType)),
                versionType = versionType
            )
        }

    }


    private fun calculateChildStats(
        artifactDependency: LinkedDependencyNode,
        artifacts: List<Artifact>,
        aggregateVersionTypeCollection: AggregateVersionTypeCollection
    ) {
        artifactDependency.children.forEach { child ->
            calculateChildStats(child, artifacts, aggregateVersionTypeCollection)
        }

        val artifact = artifacts[artifactDependency.node.artifactIdx]

        VersionType.entries.forEach { versionType ->
            val technicalLag = artifact.getTechLagForVersion(artifactDependency.node.usedVersion, versionType)

            // Leaf nodes have no stats, because stats communicate information about transitive dependencies
            if (artifactDependency.children.isNotEmpty()) {
                artifactDependency.statContainer.addStatForVersionType(
                    stats = aggregateDataToStats(
                        technicalLag,
                        aggregateVersionTypeCollection.getAggregate(versionType)
                    ),
                    versionType = versionType
                )
            }

            if (technicalLag != null) {
                aggregateVersionTypeCollection.getAggregate(versionType).transitiveLibDays.add(technicalLag.libDays)
                aggregateVersionTypeCollection.getAggregate(versionType).numberMissedReleases.add(technicalLag.numberOfMissedReleases)
                // TODO: the release distance for minor releases is always 0,0,X it should be 0,Y,X
                aggregateVersionTypeCollection.getAggregate(versionType).releaseDistances.add(
                    Triple(
                        technicalLag.distance.first.toDouble(),
                        technicalLag.distance.second.toDouble(),
                        technicalLag.distance.third.toDouble()
                    )
                )
                aggregateVersionTypeCollection.getAggregate(versionType).releaseFrequencies.add(
                    technicalLag.releaseFrequency.releasesPerMonth // TODO: extend this to the other measurements
                )
            }

        }
    }

    private fun aggregateDataToStats(
        technicalLag: TechnicalLagDto? = null,
        aggregateData: AggregateData
    ): TechnicalLagStatistics {

        val libDaysStats = dataToStatistics(aggregateData.transitiveLibDays.map { it.toDouble() })
        val missedReleasesStats = dataToStatistics(aggregateData.numberMissedReleases.map { it.toDouble() })
        val releaseDistancesStats = aggregatedTripleToStatistics(aggregateData.releaseDistances)
        val releaseFrequencyStats = dataToStatistics(aggregateData.releaseFrequencies)

        return TechnicalLagStatistics(
            technicalLag = technicalLag,
            missedReleases = missedReleasesStats,
            libDays = libDaysStats,
            distance = releaseDistancesStats,
            releaseFrequency = releaseFrequencyStats
        )
    }

    private fun dataToStatistics(data: List<Double>): Statistics? {
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

    private fun aggregatedTripleToStatistics(distances: List<Triple<Double, Double, Double>>): Triple<Statistics, Statistics, Statistics>? {
        if (distances.isEmpty()) {
            return null
        }
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
