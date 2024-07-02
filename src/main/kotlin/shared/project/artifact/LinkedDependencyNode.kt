package shared.project.artifact

import commands.calculateTechnicalLag.model.TechnicalLagStatistics
import shared.project.DependencyNode
import shared.project.IStatisticsContainer
import shared.project.StatisticsContainer

class LinkedDependencyRoot(
    children: List<LinkedDependencyNode>,
    graph: StatisticsContainer
) : LinkedNode(children, graph)

class LinkedDependencyNode(
    val node: DependencyNode,
    children: List<LinkedDependencyNode>
) : LinkedNode(children, node)

abstract class LinkedNode(
    val children: List<LinkedDependencyNode>,
    private val statContainer: StatisticsContainer
) : IStatisticsContainer {

    override fun addStatForVersionType(stats: TechnicalLagStatistics, versionType: VersionType) {
        statContainer.addStatForVersionType(stats, versionType)
    }

    override fun getStatForVersionType(versionType: VersionType): TechnicalLagStatistics? {
        return statContainer.getStatForVersionType(versionType)
    }

    private fun countChildren(child: LinkedNode): Int {
        var counter = child.children.count()

        child.children.forEach { counter += countChildren(it) }
        return counter
    }

    fun numberOfStats(): Int {
        var counter = statContainer.count()

        if (children.isEmpty()) {
            counter += VersionType.entries.count()
        }
        children.forEach { counter += it.numberOfStats() }
        return counter
    }

    val numberChildren by lazy {
        countChildren(this)
    }
}
