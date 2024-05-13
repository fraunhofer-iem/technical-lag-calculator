package technicalLag.model

import kotlinx.serialization.Serializable

@Serializable
data class AggregatedResults(
    val results: List<LibyearSumsForPackageManagerAndScopes>,
    val csvTransitiveLibyears: List<Long>,
    val csvTransitiveNumberOfDeps: List<Int>,
    val csvDirectNumberOfDeps: List<Int>,
    val cvsDirectLibyears: List<Long>
)
