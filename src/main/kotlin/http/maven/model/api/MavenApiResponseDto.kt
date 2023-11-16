package http.maven.model.api
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class MavenApiResponseDto(
    @SerialName("response")
    val response: Response?,
    @SerialName("responseHeader")
    val responseHeader: ResponseHeader?
)