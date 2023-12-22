package libyears.model

import kotlinx.serialization.Serializable

@Serializable
data class LibyearSumsResult(val transitive: Long, val direct: Long)
@Serializable
data class LibyearSumsForPackageManagerAndScopes(val packageManagerToScopes:  Map<String, Map<String, LibyearSumsResult>>)
