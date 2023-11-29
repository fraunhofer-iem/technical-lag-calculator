package http.deps.model


import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class Version(
    @SerialName("isDefault")
    val isDefault: Boolean? = null,
    @SerialName("publishedAt")
    val publishedAt: String? = null,
    @SerialName("versionKey")
    val versionKey: VersionKey
)