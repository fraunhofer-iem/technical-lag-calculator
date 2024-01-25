package artifact

import artifact.model.ArtifactDto
import artifact.model.CreateArtifactDto
import artifact.model.PackageReferenceDto
import http.deps.DepsClient
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
            isTransitiveDependency = false,
            seen = mutableSetOf()
        )?.toArtifactDto() // The toDto call resolves all deferreds
    }


    private suspend fun getDependencyVersionInformation(
        packageRef: PackageReferenceDto,
        isTransitiveDependency: Boolean = true,
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
                artifactId = packageRef.name,
                groupId = packageRef.namespace,
                usedVersion = packageRef.version,
                isTopLevelDependency = !isTransitiveDependency,
                versionDeferred = versions,
                transitiveDependencies = transitiveDependencies
            )
        }
    }
}
