package artifact.model

import io.github.z4kn4fein.semver.Version
import io.github.z4kn4fein.semver.toVersion
import kotlinx.serialization.Serializable
import kotlinx.serialization.Transient
import technicalLag.model.TechnicalLagResultStatus
import util.TimeHelper.msToDateString

enum class VersionTypes {
    Minor, Major, Patch
}

@Serializable
data class VersionDto(
    val versionNumber: String,
    val releaseDate: Long,
    @Transient
    val released: String = msToDateString(releaseDate),
    val isDefault: Boolean = false
) {

    companion object {
        /**
         * Returns the newest applicable, stable version compared to the given current version.
         * If a version is explicitly tagged as default this version is used for the comparison.
         * If not the stable version with the highest version number is used.
         * Throws if the current version doesn't follow the semver format.
         */
        fun getNewestApplicableVersion(
            currentVersion: VersionDto,
            versions: List<Pair<VersionDto, Version>>
        ): Pair<TechnicalLagResultStatus, VersionDto> {

            val current = currentVersion.versionNumber.toVersion(strict = false)
            val newestVersion = versions.last()

            versions.find { it.first.isDefault }?.let { defaultVersion ->
                return if (defaultVersion.second > current) {
                    Pair(TechnicalLagResultStatus.SEM_VERSION_WITH_DEFAULT, defaultVersion.first)
                } else {
                    Pair(TechnicalLagResultStatus.SEM_VERSION_WITH_DEFAULT, currentVersion)
                }
            }

            if (newestVersion.second > current) {
                return Pair(TechnicalLagResultStatus.SEM_VERSION_WITHOUT_DEFAULT, newestVersion.first)
            }
            return Pair(TechnicalLagResultStatus.SEM_VERSION_WITHOUT_DEFAULT, currentVersion)
        }

        fun getSortedSemVersions(packageList: List<VersionDto>): List<Pair<VersionDto, Version>> {
            return packageList.map {
                Pair(it, it.versionNumber.toVersion(strict = false))
            }.sortedBy { it.second }
        }
    }
}