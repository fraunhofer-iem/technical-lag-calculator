package shared.project

import commands.calculateTechnicalLag.model.TechnicalLagStatistics
import kotlinx.serialization.Serializable
import shared.analyzerResultDtos.DependencyGraphDto
import shared.project.artifact.ArtifactVersion
import shared.project.artifact.LinkedDependencyNode
import shared.project.artifact.LinkedDependencyRoot
import shared.project.artifact.VersionType

interface IStatisticsContainer {

    fun addStatForVersionType(stats: TechnicalLagStatistics, versionType: VersionType)
    fun getStatForVersionType(versionType: VersionType): TechnicalLagStatistics?
}

abstract class StatisticsContainer {
    private val versionTypeToStats: MutableMap<VersionType, TechnicalLagStatistics> = mutableMapOf()

    fun getAllStats(): Map<VersionType, TechnicalLagStatistics> {
        return versionTypeToStats
    }

    fun addStatForVersionType(stats: TechnicalLagStatistics, versionType: VersionType) {
        versionTypeToStats[versionType] = stats
    }

    fun getStatForVersionType(versionType: VersionType): TechnicalLagStatistics? {
        return versionTypeToStats[versionType]
    }

    fun count(): Int {
        return versionTypeToStats.entries.count()
    }
}

@Serializable
data class GraphMetadata(
    val numberOfNodes: Int,
    val numberOfEdges: Int,
    val percentageOfNodesWithStats: Double,
)

class DependencyNode private constructor(
    val artifactIdx: Int, // Index of the artifact in the DependencyGraphs' artifacts list
    val usedVersion: String,
) : StatisticsContainer() {

    companion object {
        fun create(artifactIdx: Int, version: String): DependencyNode {
            return DependencyNode(artifactIdx, ArtifactVersion.validateAndHarmonizeVersionString(version))
        }
    }
}


@Serializable
data class DependencyEdge(
    // Indices of the nodes in the DependencyGraph's nodes list
    val from: Int,
    val to: Int,
)


/**
 * A data class representing a dependency graph.
 * The idxes stored in edges reference the graph's nodes.
 * The artifactIdx in each node references the global artifacts list
 * stored in DependencyGraphs.
 */
class DependencyGraph(
    val nodes: List<DependencyNode> = listOf(),
    val edges: List<DependencyEdge> = listOf(),
    val directDependencyIndices: List<Int> = listOf(), // Idx of the nodes' which are direct dependencies of this graph
) : StatisticsContainer() {


    val rootDependency by lazy {
        linkDependencies()
    }

    val metadata by lazy {
        calculateMetadata()
    }

    private fun calculateMetadata(): GraphMetadata {
        val numberOfStats = rootDependency.numberOfStats()
        val expectedNumberOfStats = (rootDependency.numberChildren + 1) * VersionType.entries.count()
        return GraphMetadata(
            numberOfNodes = nodes.count(),
            numberOfEdges = edges.count(),
            percentageOfNodesWithStats = (numberOfStats.toDouble() / expectedNumberOfStats.toDouble()) * 100,
        )
    }


    /**
     * Function to resolve the DependencyGraph's nodes and edges lists to create a linked data structure
     * used for easier access and traversal of the stored data.
     */
    private fun linkDependencies(): LinkedDependencyRoot {
        val nodeToChildMap: MutableMap<DependencyNode, MutableList<DependencyNode>> = mutableMapOf()

        edges.forEach { edge ->
            val fromNode = nodes[edge.from]

            if (!nodeToChildMap.contains(fromNode)) {
                nodeToChildMap[fromNode] = mutableListOf()
            }

            nodeToChildMap[fromNode]?.add(nodes[edge.to])
        }

        // Function to build the NodeWithChildren recursively with cycle detection
        fun buildNodeWithChildren(node: DependencyNode, visited: MutableSet<DependencyNode>): LinkedDependencyNode {
            if (node in visited) {
                // Handle the cycle case here
                // For example, return a node with no children or throw an exception
                return LinkedDependencyNode(node, listOf())  // Returning node with no children in case of a cycle
            }

            visited.add(node)

            val children = nodeToChildMap[node]?.map { buildNodeWithChildren(it, visited) } ?: listOf()
            visited.remove(node)

            return LinkedDependencyNode(node, children)
        }

        return LinkedDependencyRoot(
            children = directDependencyIndices.map { idx ->
                val rootNode = nodes[idx]
                buildNodeWithChildren(rootNode, mutableSetOf())
            },
            graph = this
        )
    }

    constructor(dependencyGraphDto: DependencyGraphDto) : this(
        edges = dependencyGraphDto.edges,
        directDependencyIndices = dependencyGraphDto.directDependencyIndices,
        nodes = dependencyGraphDto.nodes.map {
            val node = DependencyNode.create(
                artifactIdx = it.artifactIdx,
                version = it.usedVersion
            )

            it.stats.forEach { statistics ->
                node.addStatForVersionType(statistics.stats, statistics.versionType)
            }

            node
        }
    ) {
        dependencyGraphDto.stats.forEach { stats ->
            this.addStatForVersionType(stats.stats, stats.versionType)
        }
    }
}
