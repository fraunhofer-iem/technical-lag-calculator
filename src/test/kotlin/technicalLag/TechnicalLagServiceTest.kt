package technicalLag

import dependencies.model.*
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toInstant
import org.junit.jupiter.api.Test

class TechnicalLagServiceTest {

    private fun setupArtifacts(): List<Artifact> {
        val usedVersionDate = LocalDateTime(2024, 1, 1, 0, 0).toInstant(TimeZone.of("UTC+3")).toEpochMilliseconds()
        val patchVersionDate = LocalDateTime(2024, 1, 3, 0, 0).toInstant(TimeZone.of("UTC+3")).toEpochMilliseconds()
        val minorVersionDate = LocalDateTime(2024, 1, 9, 0, 0).toInstant(TimeZone.of("UTC+3")).toEpochMilliseconds()
        val majorVersionDate = LocalDateTime(2024, 1, 19, 0, 0).toInstant(TimeZone.of("UTC+3")).toEpochMilliseconds()


        val versions = listOf(
            ArtifactVersion.create(versionNumber = "1.0.0", releaseDate = usedVersionDate),
            ArtifactVersion.create(versionNumber = "1.0.2", releaseDate = patchVersionDate),
            ArtifactVersion.create(versionNumber = "1.1.0", releaseDate = minorVersionDate),
            ArtifactVersion.create(versionNumber = "2.0.0", releaseDate = majorVersionDate),
        )

        return listOf(
            Artifact(artifactId = "artifact One", groupId = "group one", versions = versions),
            Artifact(artifactId = "artifact two", groupId = "group two", versions = versions),
            Artifact(artifactId = "artifact three", groupId = "group three", versions = versions),
        )
    }

    private fun setupDepsGraph(): DependencyGraphs {
        val artifacts: List<Artifact> = setupArtifacts()
        val graph: Map<String, DependencyGraph> = mapOf(
            "compile" to DependencyGraph(
                nodes = listOf(
                    ArtifactNode(0, "1.0.0"),
                    ArtifactNode(1, "1.0.0"),
                    ArtifactNode(2, "1.0.0"),
                ),
                edges = listOf(
                    ArtifactNodeEdge(0, 2),
                    ArtifactNodeEdge(1, 2),
                ),
                directDependencyIndices = listOf(0, 1)
            )
        )

        return DependencyGraphs(
            artifacts = artifacts,
            graph = graph,
            ecosystem = ""
        )
    }

    @Test
    fun calculateTechLagStatsEmptyGraph() {

        val service = TechnicalLagService()

        service.connectDependenciesToStats(
            graphs = DependencyGraphs(
                ecosystem = ""
            )
        )

        // The only outcome we want to check is that we don't run in a runtime exception with an
        // empty data structure
    }

    @Test
    fun calculateTechLagStatsIdenticalData() {

        val service = TechnicalLagService()
        val graphs = setupDepsGraph()
        service.connectDependenciesToStats(
            graphs
        )

        val deps = graphs.graph.values.first().linkedDirectDependencies
        deps.forEach {
            println(it.getStatForVersionType(ArtifactVersion.VersionType.Major))
            println(it.getStatForVersionType(ArtifactVersion.VersionType.Minor))
            println(it.getStatForVersionType(ArtifactVersion.VersionType.Patch))
        }
    }

}
