package http.deps.model


import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class DepsTreeResponseDto(
    @SerialName("edges")
    val edges: List<Edge> = listOf(),
    @SerialName("error")
    val error: String?,
    /**
     * The nodes of the dependency graph. The first node is the root of the graph.
     */
    @SerialName("nodes")
    val nodes: List<Node> = listOf()
)