package http.maven.model.api

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class Params(
    @SerialName("core")
    val core: String?,
    @SerialName("fl")
    val fl: String?,
    @SerialName("indent")
    val indent: String?,
    @SerialName("q")
    val q: String?,
    @SerialName("rows")
    val rows: String?,
    @SerialName("sort")
    val sort: String?,
    @SerialName("start")
    val start: String?,
    @SerialName("version")
    val version: String?,
    @SerialName("wt")
    val wt: String?
)