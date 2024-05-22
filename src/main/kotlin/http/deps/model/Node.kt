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