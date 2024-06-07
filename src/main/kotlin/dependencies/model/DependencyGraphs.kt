package dependencies.model

import io.github.z4kn4fein.semver.toVersion
import kotlinx.serialization.Serializable
import technicalLag.model.TechnicalLagDto
import technicalLag.model.TechnicalLagStatistics
import util.TimeHelper

@Serializable
data class ArtifactNode(
    val artifactIdx: Int, // Index of the artifact in the DependencyGraphs' artifacts list
    val usedVersion: String
) {
    companion object {
        fun create(artifactIdx: Int, version: String): ArtifactNode {
            return ArtifactNode(artifactIdx, ArtifactVersion.validateAndHarmonizeVersionString(version))
        }
    }
}

@Serializable
data class ArtifactNodeEdge(
    // Indices of the nodes in the DependencyGraph's nodes list
    val from: Int,
    val to: Int,
)

abstract class Node(
    val children: List<ArtifactDependency>,
    private val versionTypeToStats: MutableMap<ArtifactVersion.VersionType, TechnicalLagStatistics> = mutableMapOf()
) {
    fun addStatForVersionType(stats: TechnicalLagStatistics, versionType: ArtifactVersion.VersionType) {
        versionTypeToStats[versionType] = stats
    }

    fun getStatForVersionType(versionType: ArtifactVersion.VersionType): TechnicalLagStatistics? {
        return versionTypeToStats[versionType]
    }

    private fun countChildren(child: Node): Int {
        var counter = child.children.count()

        child.children.forEach { counter += countChildren(it) }
        return counter
    }

    val numberChildren by lazy {
        countChildren(this)
    }
}

class Root(
    children: List<ArtifactDependency>
) : Node(children)

class ArtifactDependency(
    val node: ArtifactNode,
    children: List<ArtifactDependency>
) : Node(children)


/**
 * A data class representing a dependency graph.
 * The idxes stored in edges reference the graph's nodes.
 * The artifactIdx in each node references the global artifacts list
 * stored in DependencyGraphs.
 */
@Serializable
data class DependencyGraph(
    val nodes: List<ArtifactNode> = listOf(),
    val edges: List<ArtifactNodeEdge> = listOf(),
    val directDependencyIndices: List<Int> = listOf(), // Idx of the nodes' which are direct dependencies of this graph
) {

    val rootDependency by lazy {
        linkDependencies()
    }


    /**
     * Function to resolve the DependencyGraph's nodes and edges lists to create a linked data structure
     * used for easier access and traversal of the stored data.
     */
    private fun linkDependencies(): Root {
        val nodeToChildMap: MutableMap<ArtifactNode, MutableList<ArtifactNode>> = mutableMapOf()

        edges.forEach { edge ->
            val fromNode = nodes[edge.from]

            if (!nodeToChildMap.contains(fromNode)) {
                nodeToChildMap[fromNode] = mutableListOf()
            }

            nodeToChildMap[fromNode]?.add(nodes[edge.to])
        }

        // Function to build the NodeWithChildren recursively with cycle detection
        fun buildNodeWithChildren(node: ArtifactNode, visited: MutableSet<ArtifactNode>): ArtifactDependency {
            if (node in visited) {
                // Handle the cycle case here
                // For example, return a node with no children or throw an exception
                return ArtifactDependency(node, listOf())  // Returning node with no children in case of a cycle
            }

            visited.add(node)

            val children = nodeToChildMap[node]?.map { buildNodeWithChildren(it, visited) } ?: listOf()
            visited.remove(node)

            return ArtifactDependency(node, children)
        }

        return Root(
            children = directDependencyIndices.map { idx ->
                val rootNode = nodes[idx]
                buildNodeWithChildren(rootNode, mutableSetOf())
            }
        )
    }
}

/**
 * A data class representing a package used in a dependency.
 * An artifact can be referenced from multiple dependency graphs' nodes.
 * As the versions list can grow very large it's recommended to make sure to create only
 * one artifact for each package and reuse it properly.
 */
data class Artifact(
    val artifactId: String,
    val groupId: String,
    val versions: List<ArtifactVersion> = listOf(),
    private val versionToVersionTypeToTechLag: MutableMap<String, TechnicalLagDto> =
        mutableMapOf()
) {

    override fun toString(): String {
        return "${groupId}:${artifactId} with ${versions.size} versions."
    }

    fun getTechLagMap(): Map<String, TechnicalLagDto> {
        return versionToVersionTypeToTechLag
    }

    /**
     * Returns the technical lag between the given rawVersion and the target version defined by
     * versionType (major, minor, patch).
     */
    fun getTechLagForVersion(rawVersion: String, versionType: ArtifactVersion.VersionType): TechnicalLagDto? {
        val version = ArtifactVersion.validateAndHarmonizeVersionString(rawVersion)
        val ident = "$version-$versionType"

        return if (versionToVersionTypeToTechLag.contains(ident)) {
            versionToVersionTypeToTechLag[ident]
        } else {
            val techLag = calculateTechnicalLag(version, versionType)
            if (techLag != null) {
                versionToVersionTypeToTechLag[ident] = techLag
            }
            techLag
        }
    }

    private fun calculateTechnicalLag(version: String, versionType: ArtifactVersion.VersionType): TechnicalLagDto? {

        return if (versions.isEmpty()) {
            null
        } else {
            val newestVersion =
                ArtifactVersion.findHighestApplicableVersion(version, versions, versionType)
            val currentVersion = versions.find { it.versionNumber == version }
            if (newestVersion != null && currentVersion != null) {

                val differenceInDays = TimeHelper.getDifferenceInDays(
                    currentVersion = currentVersion.releaseDate,
                    newestVersion = newestVersion.releaseDate
                )

                val missedReleases =
                    versions.indexOfFirst { it.versionNumber == newestVersion.versionNumber } - versions.indexOfFirst { it.versionNumber == currentVersion.versionNumber }

                val current = currentVersion.versionNumber.toVersion(strict = false)
                val newest = newestVersion.versionNumber.toVersion(strict = false)

                val distance: Triple<Int, Int, Int> =
                    Triple(
                        newest.major - current.major,
                        newest.minor - current.minor,
                        newest.patch - current.patch
                    )


                TechnicalLagDto(
                    libDays = -1 * differenceInDays,
                    version = newestVersion.versionNumber,
                    distance = distance,
                    numberOfMissedReleases = missedReleases
                )
            } else {
                null
            }
        }
    }
}

@Serializable
data class ArtifactVersion private constructor(
    val versionNumber: String,
    val releaseDate: Long,
    val isDefault: Boolean = false
) {

    enum class VersionType {
        Minor, Major, Patch
    }

    companion object {
        fun create(versionNumber: String, releaseDate: Long, isDefault: Boolean = false): ArtifactVersion {
            return ArtifactVersion(
                releaseDate = releaseDate,
                isDefault = isDefault,
                versionNumber = validateAndHarmonizeVersionString(versionNumber)
            )
        }

        fun validateAndHarmonizeVersionString(version: String): String {
            return version.toVersion(strict = false).toString()
        }

        fun findHighestApplicableVersion(
            version: String,
            versions: List<ArtifactVersion>,
            updateType: VersionType
        ): ArtifactVersion? {

            val semvers = versions.map { it.versionNumber.toVersion(strict = false) }
            val current = version.toVersion(strict = false)

            val filteredVersions = if (current.isStable) {
                semvers.filter { it.isStable && !it.isPreRelease }
            } else {
                if (current.isPreRelease) {
                    semvers
                } else {
                    semvers.filter { !it.isPreRelease }
                }
            }

            when (updateType) {
                VersionType.Minor -> {
                    filteredVersions.filter { it.major == current.major }
                        .maxWithOrNull(compareBy({ it.minor }, { it.patch }))
                }

                VersionType.Major -> {
                    filteredVersions
                        .maxWithOrNull(compareBy({ it.major }, { it.minor }, { it.patch }))
                }

                VersionType.Patch -> {
                    filteredVersions.filter { it.major == current.major && it.minor == current.minor }
                        .maxByOrNull { it.patch }
                }
            }?.let { highestVersion ->
                return versions.find { it.versionNumber == highestVersion.toString() }
            }

            return null
        }
    }
}

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
                versions = it.versions,
                groupId = it.groupId,
                artifactId = it.artifactId,
                versionToVersionTypeToTechLag = it.technicalLag.associate { Pair(it.updateVersion, it.technicalLag) }
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
