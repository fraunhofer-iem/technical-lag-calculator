package dependencies.graph

import dependencies.model.DependencyGraphsDto

data class DependencyGraphs(
    val artifacts: List<Artifact> = listOf(), // Stores all components and their related metadata
    val graphs: Map<String, Map<String, DependencyGraph>> = mapOf(), // Maps the graphs' scope to multiple versions of the original dependency graph
    val graph: Map<String, DependencyGraph> = mapOf(), // Maps the graphs' scope to the dependency graph extracted from the project
    val ecosystem: String, // Used to identify the appropriate APIs to call for additional information
    val version: String = "",
    val artifactId: String = "",
    val groupId: String = ""
) {
    constructor(dependencyGraphsDto: DependencyGraphsDto) : this(
        artifacts = dependencyGraphsDto.artifacts.map {
            Artifact(
                versions = it.versions.map {
                    ArtifactVersion.create(
                        versionNumber = it.versionNumber,
                        releaseDate = it.releaseDate,
                        isDefault = it.isDefault
                    )
                },
                groupId = it.groupId,
                artifactId = it.artifactId,
                versionToVersionTypeToTechLag = it.technicalLag.associate {
                    Pair(
                        it.updateVersion,
                        it.technicalLag
                    )
                }
                    .toMutableMap()
            )
        },
        ecosystem = dependencyGraphsDto.ecosystem,
        version = dependencyGraphsDto.version,
        artifactId = dependencyGraphsDto.artifactId,
        groupId = dependencyGraphsDto.groupId,
        graph = dependencyGraphsDto.graph.associate { Pair(it.scope, it.graph) },
        graphs = dependencyGraphsDto.graphs.associate {
            Pair(
                it.scope,
                it.versionToGraph.associate { Pair(it.version, it.graph) }
            )
        }
    )
}
