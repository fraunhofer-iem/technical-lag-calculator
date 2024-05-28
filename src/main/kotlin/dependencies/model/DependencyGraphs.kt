package dependencies.model

import io.github.z4kn4fein.semver.toVersion
import kotlinx.serialization.Serializable
import technicalLag.model.TechnicalLagDto
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

data class Dependency(
    val node: ArtifactNode,
    val children: List<Dependency>,
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
    val linkedDirectDependencies by lazy {
        linkDependencies()
    }

    /**
     * Function to resolve the DependencyGraph's nodes and edges lists to create a linked data structure
     * used for easier access and traversal of the stored data.
     */
    private fun linkDependencies(): List<Dependency> {
        val nodeToChildMap: MutableMap<ArtifactNode, MutableList<ArtifactNode>> = mutableMapOf()

        edges.forEach { edge ->
            val fromNode = nodes[edge.from]

            if (!nodeToChildMap.contains(fromNode)) {
                nodeToChildMap[fromNode] = mutableListOf()
            }

            nodeToChildMap[fromNode]?.add(nodes[edge.to])
        }

        // Function to build the NodeWithChildren recursively
        fun buildNodeWithChildren(node: ArtifactNode): Dependency {
            val children = nodeToChildMap[node]?.map { buildNodeWithChildren(it) } ?: listOf()
            return Dependency(node, children)
        }

        return directDependencyIndices.map { idx ->
            val rootNode = nodes[idx]
            buildNodeWithChildren(rootNode)
        }
    }
}

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
    val versions: List<ArtifactVersion> = listOf()
) {

    private val versionToVersionTypeToTechLag: MutableMap<String, TechnicalLagDto> =
        mutableMapOf()

    fun getTechLagForVersion(version: String, versionType: ArtifactVersion.VersionType): TechnicalLagDto? {
        val ident = "$version-$versionType"
        if (versionToVersionTypeToTechLag.contains(ident)) {
            return versionToVersionTypeToTechLag[ident]
        } else {
            val techLag = calculateTechnicalLag(version, versionType)
            if (techLag != null) {
                versionToVersionTypeToTechLag[ident] = techLag
            }
            return techLag
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
                    libyear = -1 * differenceInDays,
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

        // TODO: needs testing
        // We want this to return the version itself if it is the highest applicable version
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
)
