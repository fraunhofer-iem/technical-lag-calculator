package dependencies.graph

import technicalLag.model.TechnicalLagStatistics


class Root(
    children: List<LinkedArtifactDependencies>
) : Node(children)

class LinkedArtifactDependencies(
    val node: ArtifactNode,
    children: List<LinkedArtifactDependencies>
) : Node(children)

abstract class Node(
    val children: List<LinkedArtifactDependencies>,
    private val versionTypeToStats: MutableMap<VersionType, TechnicalLagStatistics> = mutableMapOf()
) {
    fun addStatForVersionType(stats: TechnicalLagStatistics, versionType: VersionType) {
        versionTypeToStats[versionType] = stats
    }

    fun getStatForVersionType(versionType: VersionType): TechnicalLagStatistics? {
        return versionTypeToStats[versionType]
    }

    private fun countChildren(child: Node): Int {
        var counter = child.children.count()

        child.children.forEach { counter += countChildren(it) }
        return counter
    }

    companion object {
        fun numberOfStats(child: Node): Int {
            var counter = child.versionTypeToStats.entries.count()

            if (child.children.isEmpty()) {
                counter += VersionType.entries.count()
            }
            child.children.forEach { counter += numberOfStats(it) }
            return counter
        }
    }

    val numberChildren by lazy {
        countChildren(this)
    }
}
