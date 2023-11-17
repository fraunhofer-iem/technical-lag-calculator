package http.maven.model.api

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class Response(
    @SerialName("docs")
    val docs: List<Doc> = emptyList(),
    @SerialName("numFound")
    val numFound: Int?,
    @SerialName("start")
    val start: Int?
)