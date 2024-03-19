package artifact

import artifact.model.ArtifactDto
import artifact.model.CreateArtifactDto
import artifact.model.PackageReferenceDto
import http.deps.DepsClient
import http.deps.model.DepsTreeResponseDto
import http.deps.model.Node
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async

class ArtifactService(
    private val depsClient: DepsClient = DepsClient(),
    private val ioScope: CoroutineScope = CoroutineScope(Dispatchers.IO)
) {

    suspend fun getAllTransitiveVersionInformation(
        rootPackage: PackageReferenceDto,
    ): ArtifactDto? {

        return getDependencyVersionInformation(
            packageRef = rootPackage,
            seen = mutableSetOf()
        )?.toArtifactDto() // The toDto call resolves all deferreds
    }

    suspend fun getDependencyTreeForPkg(
        ecosystem: String,
        namespace: String = "",
        name: String,
        version: String
    ): ArtifactDto? {

        depsClient.getDepsForPackage(
            ecosystem = ecosystem,
            namespace = namespace,
            name = name,
            version = version
        )?.let { depsTreeResponse ->
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

        return CreateArtifactDto(
            nameId = nameAndNamespace.second,
            groupId = nameAndNamespace.first,
            usedVersion = node.versionKey.version,
            transitiveDependencies = transitiveNodes,
            versionDeferred = ioScope.async {
                return@async depsClient.getVersionsForPackage(
                    ecosystem = ecosystem,
                    namespace = nameAndNamespace.first,
                    name = nameAndNamespace.second
                )
            }
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
