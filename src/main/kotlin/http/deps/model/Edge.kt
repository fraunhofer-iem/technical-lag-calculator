package http.deps.model


import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class Edge(
    @SerialName("fromNode")
    val fromNode: Int,
    @SerialName("requirement")
    val requirement: String?,
    @SerialName("toNode")
    val toNode: Int
)