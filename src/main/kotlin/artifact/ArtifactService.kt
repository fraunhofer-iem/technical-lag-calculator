package artifact

import artifact.model.*
import http.deps.DepsClient
import http.deps.model.DepsTreeResponseDto
import http.deps.model.Node
import io.github.z4kn4fein.semver.toVersion
import kotlinx.coroutines.*
import org.apache.logging.log4j.kotlin.logger

class ArtifactService @OptIn(ExperimentalCoroutinesApi::class) constructor(
    private val depsClient: DepsClient = DepsClient(),
    // It is important to limit the parallelization of the IO scope, which is used to make server
    // requests, or else the server at some point will tell us to go away.
    private val ioScope: CoroutineScope = CoroutineScope(Dispatchers.IO.limitedParallelism(10))
) {

    suspend fun directDependencyPackageReferenceToArtifact(
        rootPackage: PackageReferenceDto
    ): ArtifactDto? {

        return getDependencyVersionInformation(
            packageRef = rootPackage,
            seen = mutableSetOf()
        )?.toArtifactDto() // The toDto call resolves all deferreds

    }

    fun close() {
        depsClient.close()
        ioScope.cancel()
    }

    suspend fun simulateUpdateForArtifact(ecosystem: String, artifactDto: ArtifactDto): ArtifactDto {
        try {
            logger.info { "Simulate update for ${artifactDto.artifactId}" }
            val highestPossibleMinor =
                getApplicableVersion(artifactDto.usedVersion, artifactDto.versions, VersionTypes.Minor)
            val highestPossibleMajor =
                getApplicableVersion(artifactDto.usedVersion, artifactDto.versions, VersionTypes.Major)
            val highestPossiblePatch =
                getApplicableVersion(artifactDto.usedVersion, artifactDto.versions, VersionTypes.Patch)
            logger.info { "Update possibilities $highestPossiblePatch, $highestPossibleMajor, $highestPossibleMinor" }

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

            val updatePossibilities = UpdatePossibilities(
                minor = updatedSubTreeMinor,
                major = updatedSubTreeMajor,
                patch = updatedSubTreePatch
            )

            return ArtifactDto(
                artifactId = artifactDto.artifactId,
                groupId = artifactDto.groupId,
                usedVersion = artifactDto.usedVersion,
                versions = artifactDto.versions,
                transitiveDependencies = artifactDto.transitiveDependencies,
                updatePossibilities = updatePossibilities
            )
        } catch (exception: Exception) {
            logger.warn { "Get update possibilities failed with $exception" }
            return artifactDto
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
                ).toArtifactDto()

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


    private suspend fun getCreateArtifactFromVersion(
        ecosystem: String,
        tree: DepsTreeResponseDto,
        idx: Int,
        node: Node
    ): CreateArtifactDto {

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

        val versions = ioScope.async {
            depsClient.getVersionsForPackage(
                ecosystem = ecosystem,
                namespace = nameAndNamespace.first,
                name = nameAndNamespace.second
            )
        }


        return CreateArtifactDto(
            nameId = nameAndNamespace.second,
            groupId = nameAndNamespace.first,
            usedVersion = node.versionKey.version,
            transitiveDependencies = transitiveNodes,
            versionDeferred = versions
        )
    }


    private suspend fun getDependencyVersionInformation(
        packageRef: PackageReferenceDto,
        seen: MutableSet<PackageReferenceDto>,
    ): CreateArtifactDto? {
        return if (seen.contains(packageRef)) {
            null
        } else {
            seen.add(packageRef)

            val versions = ioScope.async {
                depsClient.getVersionsForPackage(
                    ecosystem = packageRef.type,
                    namespace = packageRef.namespace,
                    name = packageRef.name
                )
            }

            val transitiveDependencies = packageRef.dependencies.map {
                ioScope.async {
                    getDependencyVersionInformation(
                        packageRef = it,
                        seen = seen
                    )
                }
            }

            return CreateArtifactDto(
                nameId = packageRef.name,
                groupId = packageRef.namespace,
                usedVersion = packageRef.version,
                versionDeferred = versions,
                transitiveDependencyDeferreds = transitiveDependencies
            )
        }
    }
}
