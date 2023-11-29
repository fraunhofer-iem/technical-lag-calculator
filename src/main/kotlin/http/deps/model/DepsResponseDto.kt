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