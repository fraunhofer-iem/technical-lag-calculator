package http.deps.model


import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class Node(
    @SerialName("bundled")
    val bundled: Boolean,
    @SerialName("errors")
    val errors: List<String> = listOf(),
    @SerialName("relation")
    val relation: String,
    @SerialName("versionKey")
    val versionKey: VersionKeyX
)