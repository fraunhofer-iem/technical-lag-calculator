package artifact.model

import kotlinx.serialization.Serializable

@Serializable
data class ArtifactNode(
    val artifactIdx: Int, // Index of the artifact in the DependencyGraphs' artifacts list
    val usedVersionIdx: Int, // Index of the used version in the Artifact's version list
)

@Serializable
data class ArtifactNodeEdge(
    // Indices of the nodes in the DependencyGraph's nodes list
    val from: Int,
    val to: Int,
)

/**
 * A data class representing a dependency graph.
 * The idxes stored in edges reference the graph's nodes.
 * The artifactIdx and usedVersionIdx in each node references the global artifacts list and their versions
 * stored in DependencyGraphs.
 */
@Serializable
data class DependencyGraph(
    val nodes: List<ArtifactNode> = listOf(),
    val edges: List<ArtifactNodeEdge> = listOf(),
    val directDependencyIndices: List<Int> = listOf(), // Idx of the nodes' which are direct dependencies of this graph
)

/**
 * A data class representing a package used in a dependency.
 * An artifact can be referenced from multiple dependency graphs' nodes.
 * As the versions list can grow very large it's recommended to make sure to create only
 * one artifact for each package and reuse it properly.
 */
@Serializable
data class Artifact(
    val artifactId: String,
    val groupId: String,
    val versions: List<Version> = listOf(),
)

@Serializable
data class Version(
    val versionNumber: String,
    val releaseDate: Long,
    val isDefault: Boolean = false
)

data class DependencyGraphs(
    val artifacts: List<Artifact> = listOf(), // Stores all components and their related metadata
    val graphs: Map<String, Map<String, DependencyGraph>> = mapOf(), // Maps the graphs' scope to multiple versions of the original dependency graph
    val graph: Map<String, DependencyGraph> = mapOf(), // Maps the graphs' scope to the dependency graph extracted from the project
    val ecosystem: String, // Used to identify the appropriate APIs to call for additional information
    val version: String = "",
    val artifactId: String = "",
    val groupId: String = ""
)
