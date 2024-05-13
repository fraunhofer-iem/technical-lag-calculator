package technicalLag.model

import kotlinx.serialization.Serializable

@Serializable
data class LibyearSumsResult(val libyears: Long, val numberOfDependencies: Int)

@Serializable
data class LibyearsAndDependencyCount(val transitive: LibyearSumsResult, val direct: LibyearSumsResult)

@Serializable
data class LibyearSumsForPackageManagerAndScopes(val packageManagerToScopes:  Map<String, Map<String, LibyearsAndDependencyCount>>)
