package shared.project

import shared.analyzerResultDtos.ProjectDto
import shared.project.artifact.Artifact
import shared.project.artifact.ArtifactVersion

typealias ScopesToGraphs = Map<String, DependencyGraph>
typealias GraphUpdates = Map<String, ScopesToGraphs>

data class Project(
    val artifacts: List<Artifact> = listOf(), // Stores all components and their related metadata
    val graphs: GraphUpdates = mapOf(), // Maps the graphs' scope to multiple versions of the original dependency graph
    val graph: ScopesToGraphs = mapOf(), // Maps the graphs' scope to the dependency graph extracted from the project
    val ecosystem: String, // Used to identify the appropriate APIs to call for additional information
    val version: String = "",
    val artifactId: String = "",
    val groupId: String = ""
) {
    constructor(projectDto: ProjectDto) : this(
        artifacts = projectDto.artifacts.map {
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
        ecosystem = projectDto.ecosystem,
        version = projectDto.version,
        artifactId = projectDto.artifactId,
        groupId = projectDto.groupId,
        graph = projectDto.graph.associate { Pair(it.scope, it.graph) },
        graphs = projectDto.graphs.associate {
            Pair(
                it.scope,
                it.versionToGraph.associate { Pair(it.version, it.graph) }
            )
        }
    )
}
