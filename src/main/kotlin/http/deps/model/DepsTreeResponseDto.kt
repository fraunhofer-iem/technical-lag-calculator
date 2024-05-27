package http.deps.model


import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class DepsTreeResponseDto(
    @SerialName("edges")
    val edges: MutableList<Edge> = mutableListOf(),
    @SerialName("error")
    val error: String?,
    /**
     * The nodes of the dependency graph. The first node is the root of the graph.
     */
    @SerialName("nodes")
    val nodes: List<Node> = listOf()
)

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
) {
    val namespaceAndName by lazy {
        val namespaceAndNameSplit = versionKey.name.split("/")

        if (namespaceAndNameSplit.count() == 2) {
            Pair(namespaceAndNameSplit[0], namespaceAndNameSplit[1])
        } else {
            Pair("", namespaceAndNameSplit[0])
        }
    }

    fun getNamespace(): String = namespaceAndName.first
    fun getName(): String = namespaceAndName.second
}

@Serializable
data class Edge(
    @SerialName("fromNode")
    val fromNode: Int,
    @SerialName("requirement")
    val requirement: String?,
    @SerialName("toNode")
    val toNode: Int
)

@Serializable
data class VersionKeyX(
    @SerialName("name")
    val name: String,
    @SerialName("system")
    val system: String,
    @SerialName("version")
    val version: String
)
