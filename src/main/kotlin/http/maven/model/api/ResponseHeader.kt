package http.maven.model.api

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class ResponseHeader(
    @SerialName("params")
    val params: Params?,
    @SerialName("QTime")
    val qTime: Int?,
    @SerialName("status")
    val status: Int?
)