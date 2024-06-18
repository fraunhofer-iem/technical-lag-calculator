package dependencies.model

import dependencies.model.Node.Companion.numberOfStats
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
                counter += VersionType.entries.count() // TODO: we have stats for leaf nodes that shouldn't be the case
            }
            child.children.forEach { counter += numberOfStats(it) }
            return counter
        }
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

@Serializable
data class GraphMetadata(
    val numberOfNodes: Int,
    val numberOfEdges: Int,
    val percentageOfNodesWithStats: Double,
)

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

    val metadata by lazy {
        calculateMetadata()
    }

    private fun calculateMetadata(): GraphMetadata {
        val numberOfStats = numberOfStats(rootDependency)
        val expectedNumberOfStats = (rootDependency.numberChildren + 1) * VersionType.entries.count()
        return GraphMetadata(
            numberOfNodes = nodes.count(),
            numberOfEdges = edges.count(),
            percentageOfNodesWithStats = (numberOfStats.toDouble() / expectedNumberOfStats.toDouble()) * 100,
        )
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

//TODO: create data class for artifact. implement comparator in version
/**
 * A data class representing a package used in a dependency.
 * An artifact can be referenced from multiple dependency graphs' nodes.
 * As the versions list can grow very large it's recommended to make sure to create only
 * one artifact for each package and reuse it properly.
 */
class Artifact(
    val artifactId: String,
    val groupId: String,
    versions: List<ArtifactVersion> = listOf(),
    val sortedVersions: List<ArtifactVersion> = versions.sorted(),
    private val versionToVersionTypeToTechLag: MutableMap<String, TechnicalLagDto> =
        mutableMapOf()
) {


    override fun toString(): String {
        return "${groupId}:${artifactId} with ${sortedVersions.size} versions."
    }

    fun getTechLagMap(): Map<String, TechnicalLagDto> {
        return versionToVersionTypeToTechLag
    }

    /**
     * Returns the technical lag between the given rawVersion and the target version defined by
     * versionType (major, minor, patch).
     */
    fun getTechLagForVersion(rawVersion: String, versionType: VersionType): TechnicalLagDto? {
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

    private fun calculateTechnicalLag(version: String, versionType: VersionType): TechnicalLagDto? {

        return if (sortedVersions.isEmpty()) {
            null
        } else {
            val newestVersion =
                ArtifactVersion.findHighestApplicableVersion(version, sortedVersions, versionType)
            val currentVersion = sortedVersions.find { it.versionNumber == version }
            if (newestVersion != null && currentVersion != null) {

                val differenceInDays = TimeHelper.getDifferenceInDays(
                    currentVersion = currentVersion.releaseDate,
                    newestVersion = newestVersion.releaseDate
                )

                // TODO: missed releases was negative. that should be helpful to debug the findHighestApplicableVersion function
                // TODO: check for -1 return
                val filteredVersions =
                    sortedVersions.filter { it.semver.isStable || currentVersion.semver.isPreRelease == it.semver.isPreRelease }

                val missedReleases =
                    filteredVersions.indexOfFirst { it.versionNumber == newestVersion.versionNumber } - filteredVersions.indexOfFirst { it.versionNumber == currentVersion.versionNumber }

                // TODO: there are negative values here at some point, need to check
                // The calculation is incorrect if we limit the compared versions
                // if we check for newest minor release we can e.g. have this
                // 0.1.6 as current, newest minor 0.2.2., and right now we calculate the distance as
                // (0, 1, -4). it should be (0.1.2)

                TechnicalLagDto(
                    libDays = -1 * differenceInDays,
                    version = newestVersion.versionNumber,
                    distance = calculateReleaseDistance(newestVersion, currentVersion),
                    numberOfMissedReleases = missedReleases
                )
            } else {
                null
            }
        }
    }

    private fun calculateReleaseDistance(
        newer: ArtifactVersion,
        older: ArtifactVersion
    ): Triple<Int, Int, Int> {
        val oldSemVer = older.semver
        val newSemVer = newer.semver

        if (oldSemVer == newSemVer) {
            return Triple(0, 0, 0)
        }

        val newerIdx = sortedVersions.indexOf(newer)
        val olderIdx = sortedVersions.indexOf(older)

        var majorCounter = 0
        var minorCounter = 0
        var patchCounter = 0

        var maxMajor = oldSemVer.major
        var maxMinor = if (maxMajor == newSemVer.major) oldSemVer.minor else -1

        for (i in (olderIdx + 1)..newerIdx) {
            val current = sortedVersions[i].semver

            if (current.isStable || current.isPreRelease == oldSemVer.isPreRelease) {
                when {
                    current.major > maxMajor -> {
                        maxMajor = current.major
                        majorCounter += 1
                        maxMinor = current.minor
                    }

                    current.major == newSemVer.major -> {
                        when {
                            current.minor > maxMinor -> {
                                maxMinor = current.minor
                                minorCounter += 1
                            }

                            current.minor == newSemVer.minor -> patchCounter += 1
                        }
                    }
                }
            }
        }

        return Triple(majorCounter, minorCounter, patchCounter)
    }
}

enum class VersionType {
    Minor, Major, Patch
}

class ArtifactVersion private constructor(
    val versionNumber: String,
    val releaseDate: Long,
    val isDefault: Boolean = false
) : Comparable<ArtifactVersion> {

    val semver by lazy {
        versionNumber.toVersion(strict = false)
    }

    override fun compareTo(other: ArtifactVersion): Int = semver.compareTo(other.semver)


    companion object {
        fun create(versionNumber: String, releaseDate: Long, isDefault: Boolean = false): ArtifactVersion {
            return ArtifactVersion(
                releaseDate = releaseDate,
                isDefault = isDefault,
                // this step harmonizes possibly weired version formats like 2.4 or 5
                // those are parsed to 2.4.0 and 5.0.0
                versionNumber = validateAndHarmonizeVersionString(versionNumber)
            )
        }

        fun validateAndHarmonizeVersionString(version: String): String {
            return version.toVersion(strict = false).toString()
        }

        // TODO: incorrect finding for beta release 2.0.0-next.0 is not newer than 2.0.0-next.5
        fun findHighestApplicableVersion(
            version: String,
            versions: List<ArtifactVersion>,
            updateType: VersionType
        ): ArtifactVersion? {

            val semvers = versions.map { it.semver }
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

            // TODO: for prereleases / unstable releases the maxWith / maxBy operator return unwanted results
            // beta releases like 2.0.0-next.0 is not newer than 2.0.0-next.5
            val highestVersion = when (updateType) {
                VersionType.Minor -> {
                    filteredVersions.filter { it.major == current.major }
                        .max()
                }

                VersionType.Major -> {
                    filteredVersions.max()
                }

                VersionType.Patch -> {
                    filteredVersions.filter { it.major == current.major && it.minor == current.minor }
                        .max()
                }
            }

            return versions.find { it.versionNumber == highestVersion.toString() }
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
