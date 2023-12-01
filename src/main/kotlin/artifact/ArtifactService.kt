package artifact

import artifact.model.ArtifactDto
import artifact.model.CreateArtifactDto
import http.deps.DepsClient
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import org.ossreviewtoolkit.model.PackageReference

class ArtifactService(
    private val depsClient: DepsClient = DepsClient(),
    private val ioScope: CoroutineScope = CoroutineScope(Dispatchers.IO)
) {

    suspend fun getAllTransitiveVersionInformation(
        rootPackage: PackageReference,
    ): ArtifactDto? {

        return getDependencyVersionInformation(
            packageRef = rootPackage,
            isTransitiveDependency = false,
            seen = mutableSetOf()
        )?.toArtifactDto() // The toDto call resolves all deferreds
    }


    private suspend fun getDependencyVersionInformation(
        packageRef: PackageReference,
        isTransitiveDependency: Boolean = true,
        seen: MutableSet<PackageReference>,
    ): CreateArtifactDto? {
        return if (seen.contains(packageRef)) {
            null
        } else {
            seen.add(packageRef)


            val versions = ioScope.async {
                depsClient.getVersionsForPackage(
                    type = packageRef.id.type,
                    namespace = packageRef.id.namespace,
                    name = packageRef.id.name
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
                artifactId = packageRef.id.name,
                groupId = packageRef.id.namespace,
                usedVersion = packageRef.id.version,
                isTopLevelDependency = !isTransitiveDependency,
                versionDeferred = versions,
                transitiveDependencies = transitiveDependencies
            )
        }
    }
}
