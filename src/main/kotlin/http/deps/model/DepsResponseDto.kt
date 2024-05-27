package http.deps.model


import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class DepsResponseDto(
    @SerialName("packageKey")
    val packageKey: PackageKey,
    @SerialName("versions")
    val versions: List<Version> = emptyList()
)

@Serializable
data class Version(
    @SerialName("isDefault")
    val isDefault: Boolean? = null,
    @SerialName("publishedAt")
    val publishedAt: String? = null,
    @SerialName("versionKey")
    val versionKey: VersionKey
)

@Serializable
data class VersionKey(
    @SerialName("name")
    val name: String,
    @SerialName("system")
    val system: String,
    @SerialName("version")
    val version: String
)

@Serializable
data class PackageKey(
    @SerialName("name")
    val name: String,
    @SerialName("system")
    val system: String
)
