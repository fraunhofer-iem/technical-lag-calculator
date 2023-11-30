package dependencies.model

import kotlinx.serialization.Serializable
import org.ossreviewtoolkit.utils.common.Os

@Serializable
data class EnvironmentInfoDto(
    val ortVersion: String,
    val javaVersion: String,
    val os: String = Os.name
)
