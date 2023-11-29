package artifact

import artifact.model.ArtifactDto
import artifact.model.CreateArtifactDto
import http.deps.DepsClient
import org.ossreviewtoolkit.model.PackageReference

class ArtifactService(private val storeResults: Boolean = false) {

    private val depsClient = DepsClient()

    suspend fun getAllTransitiveVersionInformation(
        rootPackage: PackageReference,
    ): List<ArtifactDto> {
        val infoList: MutableList<CreateArtifactDto> = mutableListOf()
        val seen: MutableSet<PackageReference> = mutableSetOf()
        getDependencyVersionInformation(
            packageRef = rootPackage,
            infoList = infoList,
            isTransitiveDependency = false,
            seen = seen
        )

        return infoList.map { it.toArtifactDto() }
    }

    //TODO: check for cyclic dependencies
    private suspend fun getDependencyVersionInformation(
        packageRef: PackageReference,
        infoList: MutableList<CreateArtifactDto>,
        isTransitiveDependency: Boolean = true,
        seen: MutableSet<PackageReference>
    ) {
        if (seen.contains(packageRef)) {
            println("FOUND CIRCLE")
            return
        } else {
            seen.add(packageRef)
            val versions = depsClient.getVersionsForPackage(
                type = packageRef.id.type,
                namespace = packageRef.id.namespace,
                name = packageRef.id.name)

            val createArtifactDto = CreateArtifactDto(
                artifactId = packageRef.id.name,
                groupId = packageRef.id.namespace,
                usedVersion = packageRef.id.version,
                isTopLevelDependency = !isTransitiveDependency,
                versions = versions.associateBy { it.versionNumber}.toMutableMap(),
                transitiveDependencies = mutableListOf()
            )

            packageRef.dependencies.forEach {
                getDependencyVersionInformation(
                    packageRef = it,
                    infoList = createArtifactDto.transitiveDependencies,
                    seen = seen
                )
            }

            infoList.add(createArtifactDto)
        }
    }
}
