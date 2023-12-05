package artifact.model

import org.ossreviewtoolkit.model.PackageReference

data class PackageReferenceDto(
    val name: String,
    val namespace: String,
    val version: String,
    val type: String,
    val dependencies: List<PackageReferenceDto>
) {
    companion object {
        fun initFromPackageRef(packageRef: PackageReference): PackageReferenceDto {

            return PackageReferenceDto(
                name = packageRef.id.name,
                namespace = packageRef.id.namespace,
                version = packageRef.id.version,
                type = packageRef.id.type,
                dependencies = packageRef.dependencies.map {
                    initFromPackageRef(it)
                }
            )
        }
    }
}
