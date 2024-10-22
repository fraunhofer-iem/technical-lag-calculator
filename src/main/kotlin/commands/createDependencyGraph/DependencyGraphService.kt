package commands.createDependencyGraph

import kotlinx.coroutines.*
import network.dependencies.DepsClient
import org.ossreviewtoolkit.model.DependencyGraph
import org.ossreviewtoolkit.model.Identifier
import org.ossreviewtoolkit.model.PackageReference
import shared.project.DependencyEdge
import shared.project.DependencyNode
import shared.project.Project
import shared.project.artifact.Artifact
import shared.project.artifact.ArtifactVersion

private data class UniqueArtifacts(
    val artifacts: List<Artifact>,
    val identToIdx: Map<String, Int>
)

internal class DependencyGraphService(
    private val depsClient: DepsClient = DepsClient(),
    // It is important to limit the parallelization of the IO scope, which is used to make server
    // requests, or else the server at some point will tell us to go away.
    private val ioScope: CoroutineScope = CoroutineScope(Dispatchers.IO.limitedParallelism(10))
) {


    /**
     * Transform the given ORT dependency graphs into extended dependency graphs.
     * The extended dependency graphs consist of unique artifacts, which are referenced
     * by the graphs nodes'.
     * Each artifact has a list of all available versions annotated
     * to it.
     * Further, we simulate updates to the dependency graph and store the resulting
     * "potential" graphs in the DependencyGraphs graphs map.
     * Information about artifact releases and potential dependency trees are retrieved
     * from Google's deps.dev API.
     */
    suspend fun createProjectsFromGraphs(
        dependencyGraphs: Map<String, DependencyGraph>
    ): List<Project> {

        return dependencyGraphs.map { (packageManager, graph) ->

            val uniqueArtifacts = getUniqueArtifacts(graph.packages, packageManager)

            val graphs = graph.createScopes().associate { scope ->

                val directDependencyIndices = mutableListOf<Int>()
                val nodes = mutableListOf<DependencyNode>()
                val edges = mutableListOf<DependencyEdge>()
                val seen: MutableSet<PackageReference> = mutableSetOf()

                scope.dependencies.forEach { packageRef ->

                    seen.add(packageRef)
                    directDependencyIndices.addLast(nodes.count())

                    addPackageRefToNodesAndEdges(
                        packageRef = packageRef,
                        uniqueArtifacts = uniqueArtifacts,
                        nodes = nodes,
                        edges = edges,
                        seen = seen
                    )
                }
                // TODO: scope names are not unique !
                scope.name to shared.project.DependencyGraph(
                    nodes = nodes.toList(),
                    edges = edges.toList(),
                    directDependencyIndices = directDependencyIndices
                )
            }

            //TODO: Update function is broken right now and we are not yet settled on how to use it anyways
//            simulateUpdates(
            Project(
                ecosystem = packageManager,
                artifacts = uniqueArtifacts.artifacts,
                graph = graphs
            )
//            )
        }
    }

    private suspend fun getUniqueArtifacts(identifier: List<Identifier>, packageManager: String): UniqueArtifacts {
        val uniqueArtifacts = mapIdentifierToUniqueArtifact(
            identifier = identifier,
        )

        val artifactsWithVersions: List<Artifact> = queryArtifactsVersions(
            artifacts = uniqueArtifacts,
            ecosystem = packageManager
        )

        val artifactToIdx =
            artifactsWithVersions.withIndex().associate { it.value.groupId + it.value.artifactId to it.index }

        return UniqueArtifacts(
            artifacts = artifactsWithVersions,
            identToIdx = artifactToIdx
        )
    }


    /**
     * Based on the given identifier list we first create a
     * list of unique artifacts. These artifacts are then annotated
     * with their version release information based on Google's
     * deps.dev.
     * The returned UniqueArtifacts data class has a convenience
     * identToIdx map to easily find the idx of a given artifact.
     */
    private fun mapIdentifierToUniqueArtifact(
        identifier: List<Identifier>
    ): List<Artifact> {

        val artifactIdentToIdx: MutableSet<String> = mutableSetOf()
        val uniqueArtifacts: MutableList<Artifact> = mutableListOf()

        identifier.forEach { current ->
            val ident = current.namespace + current.name

            if (!artifactIdentToIdx.contains(ident)) {
                artifactIdentToIdx.add(ident)
                uniqueArtifacts.add(
                    Artifact(
                        artifactId = current.name,
                        groupId = current.namespace,
                    )
                )
            }
        }

        return uniqueArtifacts.toList()
    }


    /**
     * Queries the artifacts' versions and removes all versions not following semantic versioning standards.
     */
    private suspend fun queryArtifactsVersions(ecosystem: String, artifacts: List<Artifact>): List<Artifact> {
        val artifactDeferred: List<Deferred<Artifact>> = artifacts.map { artifact ->
            ioScope.async {
                val versions = depsClient.getVersionsForPackage(
                    ecosystem = ecosystem,
                    namespace = artifact.groupId,
                    name = artifact.artifactId
                ).mapNotNull { artifactVersion ->
                    try {
                        ArtifactVersion.create(
                            releaseDate = artifactVersion.releaseDate,
                            isDefault = artifactVersion.isDefault,
                            versionNumber = artifactVersion.versionNumber
                        )
                    } catch (e: Exception) {
                        null
                    }
                }

                Artifact(
                    artifactId = artifact.artifactId,
                    groupId = artifact.groupId,
                    versions = versions
                )
            }
        }

        return artifactDeferred.awaitAll()
    }

    /**
     * Fills the given nodes and edges lists based on the hierarchy contained
     * in the given packageRef.
     * The nodes store the idx of the corresponding unique artifact and the edges
     * reference the idx of the corresponding nodes.
     * The given packageRef might contain circles, thus we use the seen map to break
     * potential circles.
     */
    private fun addPackageRefToNodesAndEdges(
        packageRef: PackageReference,
        uniqueArtifacts: UniqueArtifacts,
        nodes: MutableList<DependencyNode>,
        edges: MutableList<DependencyEdge>,
        seen: MutableSet<PackageReference>
    ): Int {
        val ident = packageRef.id.namespace + packageRef.id.name
        val idx = uniqueArtifacts.identToIdx[ident] ?: -1
        val insertIndex = nodes.count()

        nodes.add(
            DependencyNode.create(
                artifactIdx = idx,
                version = packageRef.id.version
            )
        )

        packageRef.dependencies.forEach { dependency ->

            if (!seen.contains(dependency)) {
                seen.add(dependency)
                val depIdx = addPackageRefToNodesAndEdges(
                    packageRef = dependency,
                    uniqueArtifacts = uniqueArtifacts,
                    nodes = nodes,
                    edges = edges,
                    seen = seen
                )
                edges.add(
                    DependencyEdge(
                        from = insertIndex,
                        to = depIdx
                    )
                )
            }
        }

        return insertIndex
    }


//    suspend fun simulateUpdates(graphs: DependencyGraphs): DependencyGraphs {
//
//        // TODO: I believe this data structure is incomplete right now. After running the code it only contains
//        //  the nodes which have update possibilities. However, if we can't update something in the graph it
//        //  stays unchanged and therefor the original graph content needs to be copied
//        val scopeToVersionToTree: MutableMap<String, Map<ArtifactVersion.VersionType, DepsTreeResponseDto>> =
//            mutableMapOf()
//
//        graphs.graph.forEach { (scope, graph) ->
//            graph.directDependencyIndices.forEach { idx ->
//                val artifactNode = graph.nodes[idx]
//                val artifact = graphs.artifacts[artifactNode.artifactIdx]
//                val currentVersion = artifactNode.usedVersion
//
//                val versionTypesToGraph: MutableMap<ArtifactVersion.VersionType, DepsTreeResponseDto> =
//                    mutableMapOf()
//
//                ArtifactVersion.VersionType.entries.forEach { versionTypes ->
//
//                    ArtifactVersion.findHighestApplicableVersion(
//                        version = currentVersion,
//                        versions = artifact.versions, updateType = versionTypes
//                    )?.let { version ->
//                        getDependencyTreeForPkg(
//                            ecosystem = graphs.ecosystem,
//                            groupId = artifact.groupId,
//                            artifactId = artifact.artifactId,
//                            version = version.versionNumber
//                        )?.let { depsTree ->
//                            versionTypesToGraph[versionTypes] = depsTree
//                        }
//                    }
//                }
//                scopeToVersionToTree[scope] = versionTypesToGraph.toMap()
//            }
//        }
//
//
//        val artifacts = graphs.artifacts.toMutableList()
//        val newArtifacts = scopeToVersionToTree.values.flatMap {
//            it.values.flatMap {
//                it.nodes.map { node ->
//
//                    Artifact(
//                        artifactId = node.getName(),
//                        groupId = node.getNamespace(),
//                    )
//                }
//            }
//        }.toMutableList()
//
//        newArtifacts.removeAll { newArtifact ->
//            artifacts.any { existingArtifact ->
//                newArtifact.artifactId == existingArtifact.artifactId &&
//                        newArtifact.groupId == existingArtifact.groupId
//            }
//        }
//        val newArtifactsWithVersion = queryArtifactsVersions(ecosystem = graphs.ecosystem, artifacts = newArtifacts)
//
//        newArtifactsWithVersion.forEach { artifact ->
//            artifacts.addLast(artifact)
//        }
//
//        newArtifacts.clear()
//
//        val scopeToVersionToGraphs =
//            scopeToVersionToTree.toList().associate { (scope, versionToGraphMap) ->
//                val newGraphs = versionToGraphMap.toList().associate { (version, tree) ->
//                    val nodes = tree.nodes.map { node ->
//                        val artifactIdx =
//                            artifacts.indexOfFirst { it.groupId == node.getNamespace() && it.artifactId == node.getName() }
//                        ArtifactNode.create(
//                            artifactIdx = artifactIdx,
//                            version = node.versionKey.version
//                        )
//                    }
//
//                    val edges = tree.edges.map { edge ->
//                        ArtifactNodeEdge(from = edge.fromNode, to = edge.toNode)
//                    }
//                    version.toString() to DependencyGraph(
//                        nodes = nodes,
//                        edges = edges,
//                        directDependencyIndices = listOf(0)
//                    )
//                }
//                scope to newGraphs
//            }
//
//        return DependencyGraphs(
//            artifacts = graphs.artifacts,
//            graph = graphs.graph,
//            artifactId = graphs.artifactId,
//            groupId = graphs.groupId,
//            version = graphs.version,
//            graphs = scopeToVersionToGraphs,
//            ecosystem = graphs.ecosystem
//        )
//    }

//    private suspend fun getDependencyTreeForPkg(
//        ecosystem: String,
//        groupId: String = "",
//        artifactId: String,
//        version: String
//    ): DepsTreeResponseDto? {
//
//        depsClient.getDepsForPackage(
//            ecosystem = ecosystem,
//            groupId = groupId,
//            artifactId = artifactId,
//            version = version
//        )?.let { depsTreeResponse ->
//            logger.info { "Deps for package $groupId $artifactId retrieved. Node size: ${depsTreeResponse.nodes.size}" }
//
//            removeCycles(depsTreeResponse)
//
//            return depsTreeResponse
//        }
//
//        return null
//    }

//    private fun removeCycles(tree: DepsTreeResponseDto) {
//        val visited = BooleanArray(tree.nodes.size)
//        val adjacencyList = Array(tree.nodes.size) { mutableListOf<Int>() }
//
//        // Build adjacency list from edges
//        for (edge in tree.edges) {
//            adjacencyList[edge.fromNode].add(edge.toNode)
//        }
//
//        fun dfs(node: Int, parent: Int) {
//            if (visited[node]) {
//                // Cycle detected, remove edge
//                tree.edges.removeIf { it.fromNode == parent && it.toNode == node }
//                return
//            }
//            visited[node] = true
//            for (adjNode in adjacencyList[node].toList()) {
//                dfs(adjNode, node)
//            }
//            visited[node] = false // Backtrack
//        }
//
//        // Perform DFS traversal from each node
//        for (i in visited.indices) {
//            dfs(i, -1)
//        }
//    }

    fun close() {
        depsClient.close()
        ioScope.cancel()
    }

}