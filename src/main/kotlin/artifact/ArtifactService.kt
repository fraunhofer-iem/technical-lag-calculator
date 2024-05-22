package artifact

import artifact.model.*
import http.deps.DepsClient
import http.deps.model.DepsTreeResponseDto
import http.deps.model.Node
import io.github.z4kn4fein.semver.toVersion
import kotlinx.coroutines.*
import org.apache.logging.log4j.kotlin.logger
import java.util.concurrent.locks.ReentrantReadWriteLock

class ArtifactService @OptIn(ExperimentalCoroutinesApi::class) constructor(
    private val depsClient: DepsClient = DepsClient(),
    // It is important to limit the parallelization of the IO scope, which is used to make server
    // requests, or else the server at some point will tell us to go away.
    private val ioScope: CoroutineScope = CoroutineScope(Dispatchers.IO.limitedParallelism(10))
) {





    fun directDependencyPackageReferenceToArtifact(
        rootPackage: PackageReferenceDto
    ): ArtifactDto? {

        return getDependencyVersionInformation(
            packageRef = rootPackage,
            seen = mutableSetOf()
        )
    }

    private fun getDependencyVersionInformation(
        packageRef: PackageReferenceDto,
        seen: MutableSet<PackageReferenceDto>,
    ): ArtifactDto? {
        return if (seen.contains(packageRef)) {
            null
        } else {
            seen.add(packageRef)

            val transitiveDependencies = packageRef.dependencies.mapNotNull {
                getDependencyVersionInformation(
                    packageRef = it,
                    seen = seen
                )
            }

            return ArtifactDto(
                artifactId = packageRef.name,
                groupId = packageRef.namespace,
                usedVersion = packageRef.version,
                transitiveDependencies = transitiveDependencies
            )
        }
    }

    fun close() {
        depsClient.close()
        ioScope.cancel()
    }

    suspend fun simulateUpdateForArtifact(
        ecosystem: String,
        artifactDto: ArtifactDto,
        usedVersion: VersionDto,
        allVersions: List<VersionDto>
    ): UpdatePossibilities {
        try {
            logger.info { "Simulate update for ${artifactDto.artifactId}" }
            val highestPossibleMinor =
                getApplicableVersion(usedVersion, allVersions, VersionTypes.Minor)
            val highestPossibleMajor =
                getApplicableVersion(usedVersion, allVersions, VersionTypes.Major)
            val highestPossiblePatch =
                getApplicableVersion(usedVersion, allVersions, VersionTypes.Patch)
            logger.info { "Update possibilities $highestPossiblePatch, $highestPossibleMajor, $highestPossibleMinor" }

            // TODO: we need to merge this existing code with the updated version storage data structure

            val updatedSubTreeMinor = getDependencyTreeForPkg(
                ecosystem,
                artifactDto.groupId,
                artifactDto.artifactId,
                highestPossibleMinor
            )

            val updatedSubTreeMajor = getDependencyTreeForPkg(
                ecosystem,
                artifactDto.groupId,
                artifactDto.artifactId,
                highestPossibleMajor
            )

            val updatedSubTreePatch = getDependencyTreeForPkg(
                ecosystem,
                artifactDto.groupId,
                artifactDto.artifactId,
                highestPossiblePatch
            )

            return UpdatePossibilities(
                minor = updatedSubTreeMinor,
                major = updatedSubTreeMajor,
                patch = updatedSubTreePatch
            )

        } catch (exception: Exception) {
            logger.warn { "Get update possibilities failed with $exception" }
            return UpdatePossibilities()
        }
    }

    /**
     * Returns the highest matching version from the given versions array with the same version type as the
     * given version's type.
     */
    private fun getApplicableVersion(version: VersionDto, versions: List<VersionDto>, type: VersionTypes): String? {
        val semvers = versions.map { it.versionNumber.toVersion(strict = false) }
        val semver = version.versionNumber.toVersion(strict = false)

        val highestVersion = when (type) {
            VersionTypes.Minor -> {
                semvers.filter { it.isStable && it.major == semver.major }
                    .maxWithOrNull(compareBy({ it.minor }, { it.patch }))
            }

            VersionTypes.Major -> {
                semvers.filter { it.isStable }
                    .maxWithOrNull(compareBy({ it.major }, { it.minor }, { it.patch }))
            }

            VersionTypes.Patch -> {
                semvers.filter { it.isStable && it.major == semver.major && it.minor == semver.minor }
                    .maxByOrNull { it.patch }
            }
        }
        return highestVersion?.toString()
    }

    private suspend fun getDependencyTreeForPkg(
        ecosystem: String,
        namespace: String = "",
        name: String,
        version: String?
    ): ArtifactDto? {

        if (version == null) {
            return null
        }
        depsClient.getDepsForPackage(
            ecosystem = ecosystem,
            namespace = namespace,
            name = name,
            version = version
        )?.let { depsTreeResponse ->
            logger.info { "Deps for package $namespace $name retrieved. Node size: ${depsTreeResponse.nodes.size}" }

            removeCycles(depsTreeResponse)

            if (depsTreeResponse.nodes.isNotEmpty()) {
                return getCreateArtifactFromVersion(
                    ecosystem,
                    depsTreeResponse,
                    0,
                    depsTreeResponse.nodes.first()
                )
            }
        }

        return null
    }

    private fun removeCycles(tree: DepsTreeResponseDto) {
        val visited = BooleanArray(tree.nodes.size)
        val adjacencyList = Array(tree.nodes.size) { mutableListOf<Int>() }

        // Build adjacency list from edges
        for (edge in tree.edges) {
            adjacencyList[edge.fromNode].add(edge.toNode)
        }

        fun dfs(node: Int, parent: Int) {
            if (visited[node]) {
                // Cycle detected, remove edge
                tree.edges.removeIf { it.fromNode == parent && it.toNode == node }
                return
            }
            visited[node] = true
            for (adjNode in adjacencyList[node].toList()) {
                dfs(adjNode, node)
            }
            visited[node] = false // Backtrack
        }

        // Perform DFS traversal from each node
        for (i in visited.indices) {
            dfs(i, -1)
        }
    }


    private fun getCreateArtifactFromVersion(
        ecosystem: String,
        tree: DepsTreeResponseDto,
        idx: Int,
        node: Node
    ): ArtifactDto {

        val transitiveNodes = tree.edges
            .filter { it.fromNode == idx }
            .map { Pair(it.toNode, tree.nodes[it.toNode]) }
            .map { getCreateArtifactFromVersion(ecosystem, tree, it.first, it.second) }

        val nameAndNamespaceSplit = node.versionKey.name.split("/")
        val nameAndNamespace = if (nameAndNamespaceSplit.count() == 2) {
            Pair(nameAndNamespaceSplit[0], nameAndNamespaceSplit[1])
        } else {
            Pair("", nameAndNamespaceSplit[0])
        }

        return ArtifactDto(
            artifactId = nameAndNamespace.second,
            groupId = nameAndNamespace.first,
            usedVersion = node.versionKey.version,
            transitiveDependencies = transitiveNodes,
        )
    }


}

