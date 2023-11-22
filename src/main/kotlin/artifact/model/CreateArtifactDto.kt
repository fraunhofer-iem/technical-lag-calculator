package artifact.model

import libyears.LibyearCalculator

data class CreateArtifactDto(
    var artifactId: String? = null,
    var groupId: String? = null,
    var usedVersion: String? = null,
    var versions: MutableMap<String, VersionDto> = mutableMapOf(),
    var isTopLevelDependency: Boolean? = null,
    val transitiveDependencies: MutableList<CreateArtifactDto> = mutableListOf()
) {

    private val libyearCalculator = LibyearCalculator()
    fun toArtifactDto(): ArtifactDto {
        if (artifactId != null &&
            groupId != null &&
            usedVersion != null &&
            isTopLevelDependency != null
        ) {
            val versionList = versions.values.toList()

            return ArtifactDto(
                artifactId = artifactId!!,
                groupId = groupId!!,
                usedVersion = usedVersion!!,
                isTopLevelDependency = isTopLevelDependency!!,
                versions = versionList,
                transitiveDependencies = transitiveDependencies.map { it.toArtifactDto() },
                libyear = libyearCalculator.calculateDifferenceForPackage(usedVersion!!, versionList)
            )
        }

        throw Exception("Transformation of incomplete artifact not possible.")
    }

    fun addVersions(versionsToAdd: List<VersionDto>) {
        versionsToAdd.forEach {
            versions[it.versionNumber] = it
        }
    }
}
