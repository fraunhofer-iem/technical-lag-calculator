package artifact.model

import kotlinx.coroutines.Deferred
import kotlinx.coroutines.awaitAll
import libyears.LibyearCalculator

data class CreateArtifactDto(
    var artifactId: String? = null,
    var groupId: String? = null,
    var usedVersion: String? = null,
    var versionDeferred: Deferred<List<VersionDto>>? = null,
    var isTopLevelDependency: Boolean? = null,
    val transitiveDependencies: List<Deferred<CreateArtifactDto?>>
) {
    suspend fun toArtifactDto(): ArtifactDto {
        if (artifactIsComplete()) {

            val versions = try {
                versionDeferred?.await()
            } catch (exception: Exception) {
                println("API version job failed with error $exception")
                null
            } ?: emptyList()

            val usedVersionDto = versions.find { it.versionNumber == usedVersion }
                ?: VersionDto(versionNumber = usedVersion!!)

            return ArtifactDto(
                artifactId = artifactId!!,
                groupId = groupId!!,
                usedVersion = usedVersionDto,
                isTopLevelDependency = isTopLevelDependency!!,
                versions = versions,
                transitiveDependencies = transitiveDependencies.awaitAll().mapNotNull { it?.toArtifactDto() },
                libyearResult = LibyearCalculator.calculateDifferenceForPackage(usedVersionDto, versions),
            )
        }

        throw Exception("Transformation of incomplete artifact not possible.")
    }

    private fun artifactIsComplete(): Boolean {
        return artifactId != null &&
                groupId != null &&
                usedVersion != null &&
                isTopLevelDependency != null
    }
}
