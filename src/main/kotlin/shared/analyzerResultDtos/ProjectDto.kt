package shared.analyzerResultDtos

import commands.calculateTechnicalLag.model.TechnicalLagDto
import commands.calculateTechnicalLag.model.TechnicalLagStatistics
import kotlinx.serialization.Serializable
import shared.project.DependencyEdge
import shared.project.DependencyGraph
import shared.project.GraphMetadata
import shared.project.Project
import shared.project.artifact.ArtifactVersion
import shared.project.artifact.VersionType

@Serializable
data class ProjectDto(
    val artifacts: List<ArtifactDto> = listOf(), // Stores all components and their related metadata
    val graphs: List<ScopeToVersionToGraph>, // Maps the graphs' scope to multiple versions of the original dependency graph
    val graph: List<ScopeToGraph>, // Maps the graphs' scope to the dependency graph extracted from the project
    val ecosystem: String, // Used to identify the appropriate APIs to call for additional information
    val version: String = "",
    val artifactId: String = "",
    val groupId: String = ""
) {
    constructor(
        project: Project,
        version: String,
        artifactId: String,
        groupId: String
    ) : this(
        groupId = groupId,
        artifactId = artifactId,
        version = version,
        artifacts = project.artifacts.map {
            ArtifactDto(
                artifactId = it.artifactId,
                groupId = it.groupId,
                versions = it.sortedVersions.map { ArtifactVersionDto(it) },
            )
        },
        ecosystem = project.ecosystem,
        graph = project.graph.map { (scope, graph) ->
            ScopeToGraph(
                scope = scope,
                graph = DependencyGraphDto.createFromGraph(graph)
            )
        },
        graphs = project.graphs.map { (scope, versionToGraph) ->
            ScopeToVersionToGraph(
                scope = scope,
                versionToGraph = versionToGraph.map { (version, graph) ->
                    VersionToGraph(
                        version = version,
                        graph = DependencyGraphDto.createFromGraph(graph)
                    )
                }
            )
        }
    )
}

@Serializable
data class DependencyNodeDto(
    val artifactIdx: Int, // Index of the artifact in the DependencyGraphs' artifacts list
    val usedVersion: String,
    val stats: List<StatsDto> = listOf()
)

@Serializable
data class StatsDto(
    val versionType: VersionType,
    val stats: TechnicalLagStatistics
) {
    companion object {
        fun createFromMap(versionStatMap: Map<VersionType, TechnicalLagStatistics>): List<StatsDto> {
            if (versionStatMap.isEmpty()) return emptyList()

            return versionStatMap.map { (versionType, stats) ->
                StatsDto(versionType, stats)
            }
        }
    }
}

@Serializable
data class DependencyGraphDto(
    val nodes: List<DependencyNodeDto> = listOf(),
    val edges: List<DependencyEdge> = listOf(),
    val directDependencyIndices: List<Int> = listOf(), // Idx of the nodes' which are direct dependencies of this graph
    val stats: List<StatsDto> = listOf(),
    val metadata: GraphMetadata? = null
) {
    companion object {
        fun createFromGraph(graph: DependencyGraph): DependencyGraphDto {
            return DependencyGraphDto(
                nodes = graph.nodes.map {
                    DependencyNodeDto(
                        artifactIdx = it.artifactIdx,
                        usedVersion = it.usedVersion,
                        stats = StatsDto.createFromMap(it.getAllStats())
                    )
                },
                edges = graph.edges,
                stats = StatsDto.createFromMap(graph.getAllStats()),
                metadata = graph.metadata,
                directDependencyIndices = graph.directDependencyIndices
            )
        }
    }
}


@Serializable
data class ArtifactVersionDto(val versionNumber: String, val releaseDate: Long, val isDefault: Boolean) {
    constructor(value: ArtifactVersion) : this(value.semver.toString(), value.releaseDate, value.isDefault)
}

@Serializable
data class ArtifactDto(
    val artifactId: String,
    val groupId: String,
    val versions: List<ArtifactVersionDto> = listOf(),
)

@Serializable
data class UpdateVersionToTechLagDto(
    val technicalLag: TechnicalLagDto,
    val updateVersion: String
)

@Serializable
data class ScopeToGraph(
    val scope: String,
    val graph: DependencyGraphDto
)

@Serializable
data class ScopeToVersionToGraph(
    val scope: String,
    val versionToGraph: List<VersionToGraph>
)

@Serializable
data class VersionToGraph(
    val version: String,
    val graph: DependencyGraphDto
)
