package shared.project.artifact

import shared.project.DependencyNode
import commands.calculateTechnicalLag.model.TechnicalLagStatistics


class LinkedDependencyRoot(
    children: List<LinkedDependencyNode>
) : LinkedNode(children)

class LinkedDependencyNode(
    val node: DependencyNode,
    children: List<LinkedDependencyNode>
) : LinkedNode(children)

abstract class LinkedNode(
    val children: List<LinkedDependencyNode>,
    private val versionTypeToStats: MutableMap<VersionType, TechnicalLagStatistics> = mutableMapOf()
) {
    fun addStatForVersionType(stats: TechnicalLagStatistics, versionType: VersionType) {
        versionTypeToStats[versionType] = stats
    }

    fun getStatForVersionType(versionType: VersionType): TechnicalLagStatistics? {
        return versionTypeToStats[versionType]
    }

    private fun countChildren(child: LinkedNode): Int {
        var counter = child.children.count()

        child.children.forEach { counter += countChildren(it) }
        return counter
    }

    fun numberOfStats(): Int {
        var counter = versionTypeToStats.entries.count()

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
