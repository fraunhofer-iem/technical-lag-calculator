package shared.project.artifact

import io.github.z4kn4fein.semver.toVersion

enum class VersionType {
    Minor, Major, Patch
}

class ArtifactVersion private constructor(
    val versionNumber: String,
    val releaseDate: Long,
    val isDefault: Boolean = false
) : Comparable<ArtifactVersion> {

    val semver by lazy {
        versionNumber.toVersion(strict = false)
    }

    override fun compareTo(other: ArtifactVersion): Int = semver.compareTo(other.semver)


    companion object {
        fun create(versionNumber: String, releaseDate: Long, isDefault: Boolean = false): ArtifactVersion {
            return ArtifactVersion(
                releaseDate = releaseDate,
                isDefault = isDefault,
                // this step harmonizes possibly weired version formats like 2.4 or 5
                // those are parsed to 2.4.0 and 5.0.0
                versionNumber = validateAndHarmonizeVersionString(versionNumber)
            )
        }

        fun validateAndHarmonizeVersionString(version: String): String {
            return version.toVersion(strict = false).toString()
        }

        fun findHighestApplicableVersion(
            version: String,
            versions: List<ArtifactVersion>,
            updateType: VersionType
        ): ArtifactVersion? {

            val semvers = versions.map { it.semver }
            val current = version.toVersion(strict = false)

            val filteredVersions = if (current.isStable) {
                semvers.filter { it.isStable && !it.isPreRelease }
            } else {
                if (current.isPreRelease) {
                    semvers
                } else {
                    semvers.filter { !it.isPreRelease }
                }
            }

            val highestVersion = when (updateType) {
                VersionType.Minor -> {
                    filteredVersions.filter { it.major == current.major }

                }

                VersionType.Major -> {
                    filteredVersions
                }

                VersionType.Patch -> {
                    filteredVersions.filter { it.major == current.major && it.minor == current.minor }
                }
            }.max()

            return versions.find { it.versionNumber == highestVersion.toString() }
        }
    }
}
