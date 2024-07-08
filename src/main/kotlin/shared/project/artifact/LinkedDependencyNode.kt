package shared.project.artifact

import shared.project.DependencyNode
import shared.project.IStatisticsContainer
import shared.project.StatisticsContainer

class LinkedDependencyRoot(
    children: List<LinkedDependencyNode>,
    graph: IStatisticsContainer
) : LinkedNode(children, graph) {
    override fun copy(): LinkedDependencyRoot {
        return LinkedDependencyRoot(
            children = _children.map { it.copy() },
            graph = StatisticsContainer()
        )
    }
}

class LinkedDependencyNode(
    val node: DependencyNode,
    children: List<LinkedDependencyNode>
) : LinkedNode(children, node) {
    override fun copy(): LinkedDependencyNode {
        return LinkedDependencyNode(
            children = _children.map { it.copy() },
            node = node.copy()
        )
    }
}

abstract class LinkedNode(
    children: List<LinkedDependencyNode>,
    val statContainer: IStatisticsContainer
) {

    protected val _children = children.toMutableList()
    val children: List<LinkedDependencyNode>
        get() = _children

    fun addChildren(node: LinkedDependencyNode) {
        _children.add(node)
    }

    fun addChildren(nodes: Collection<LinkedDependencyNode>) {
        _children.addAll(nodes)
    }

    fun removeChildren(node: LinkedDependencyNode) {
        _children.remove(node)
    }

    fun clearChildren() {
        _children.clear()
    }

    abstract fun copy(): LinkedNode

    private fun countChildren(child: LinkedNode): Int {
        var counter = child._children.count()

        child._children.forEach { counter += countChildren(it) }
        return counter
    }

    fun numberOfStats(): Int {
        var counter = statContainer.count()

        if (_children.isEmpty()) {
            counter += VersionType.entries.count()
        }
        _children.forEach { counter += it.numberOfStats() }
        return counter
    }

    val numberChildren by lazy {
        countChildren(this)
    }
}
