package http.maven.model.api
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class Doc(
    @SerialName("a")
    val a: String?,
    @SerialName("ec")
    val ec: List<String> = emptyList(),
    @SerialName("g")
    val g: String?,
    @SerialName("id")
    val id: String?,
    @SerialName("p")
    val p: String?,
    @SerialName("tags")
    val tags: List<String> = emptyList(),
    @SerialName("timestamp")
    val timestamp: Long?,
    @SerialName("v")
    val v: String?
)