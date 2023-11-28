package dependencies.model

import org.ossreviewtoolkit.utils.common.Os

data class EnvironmentInfoDto(
    val ortVersion: String,
    val javaVersion: String,
    val os: String = Os.name
)
