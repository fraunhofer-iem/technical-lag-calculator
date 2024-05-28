package dependencies.model

import kotlinx.serialization.Serializable

@Serializable
data class DependencyGraphsDto(
    val artifacts: List<Artifact> = listOf(), // Stores all components and their related metadata
    val graphs: List<ScopeToVersionToGraph>, // Maps the graphs' scope to multiple versions of the original dependency graph
    val graph: List<ScopeToGraph>, // Maps the graphs' scope to the dependency graph extracted from the project
    val ecosystem: String, // Used to identify the appropriate APIs to call for additional information
    val version: String = "",
    val artifactId: String = "",
    val groupId: String = ""
) {
    constructor(
        dependencyGraphs: DependencyGraphs,
        version: String,
        artifactId: String,
        groupId: String
    ) : this(
        groupId = groupId,
        artifactId = artifactId,
        version = version,
        artifacts = dependencyGraphs.artifacts,
        ecosystem = dependencyGraphs.ecosystem,
        graph = dependencyGraphs.graph.map { (scope, graph) ->
            ScopeToGraph(
                scope = scope,
                graph = graph
            )
        },
        graphs = dependencyGraphs.graphs.map { (scope, versionToGraph) ->
            ScopeToVersionToGraph(
                scope = scope,
                versionToGraph = versionToGraph.map { (version, graph) ->
                    VersionToGraph(
                        version = version,
                        graph = graph
                    )
                }
            )
        }
    )
}

@Serializable
data class ScopeToGraph(
    val scope: String,
    val graph: DependencyGraph
)

@Serializable
data class ScopeToVersionToGraph(
    val scope: String,
    val versionToGraph: List<VersionToGraph>
)

@Serializable
data class VersionToGraph(
    val version: String,
    val graph: DependencyGraph
)