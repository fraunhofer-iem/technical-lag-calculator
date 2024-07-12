package calculateTechnicalLag

import commands.calculateTechnicalLag.TechnicalLagStatisticsService
import commands.calculateTechnicalLag.model.Statistics
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toInstant
import org.junit.jupiter.api.Test
import shared.project.DependencyEdge
import shared.project.DependencyGraph
import shared.project.DependencyNode
import shared.project.Project
import shared.project.artifact.Artifact
import shared.project.artifact.ArtifactVersion
import shared.project.artifact.VersionType
import kotlin.math.pow
import kotlin.math.sqrt
import kotlin.test.assertEquals
import kotlin.test.assertNotEquals

class TechnicalLagStatisticsServiceTest {

    private fun setupIdenticalArtifacts(): List<Artifact> {
        val usedVersionDate = LocalDateTime(2024, 1, 1, 0, 0).toInstant(TimeZone.of("UTC+0")).toEpochMilliseconds()
        val patchVersionDate = LocalDateTime(2024, 1, 3, 0, 0).toInstant(TimeZone.of("UTC+0")).toEpochMilliseconds()
        val minorVersionDate = LocalDateTime(2024, 1, 9, 0, 0).toInstant(TimeZone.of("UTC+0")).toEpochMilliseconds()
        val majorVersionDate = LocalDateTime(2024, 1, 19, 0, 0).toInstant(TimeZone.of("UTC+0")).toEpochMilliseconds()


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

    private fun getIdenticalVersionsGraph(): Project {
        val artifacts: List<Artifact> = setupIdenticalArtifacts()
        val graph: Map<String, DependencyGraph> = mapOf(
            "compile" to DependencyGraph(
                nodes = listOf(
                    DependencyNode.create(0, "1.0.0"),
                    DependencyNode.create(1, "1.0.0"),
                    DependencyNode.create(2, "1.0.0"),
                ),
                edges = listOf(
                    DependencyEdge(0, 2),
                    DependencyEdge(1, 2),
                ),
                directDependencyIndices = listOf(0, 1)
            )
        )

        return Project(
            artifacts = artifacts,
            graph = graph,
            ecosystem = ""
        )
    }

    private fun setupArtifacts(): List<Artifact> {
        val usedVersionDate = LocalDateTime(2024, 1, 1, 0, 0).toInstant(TimeZone.of("UTC+0")).toEpochMilliseconds()
        val intermediateVersionDate =
            LocalDateTime(2024, 1, 2, 0, 0).toInstant(TimeZone.of("UTC+0")).toEpochMilliseconds()
        val patchVersionDate = LocalDateTime(2024, 1, 3, 0, 0).toInstant(TimeZone.of("UTC+0")).toEpochMilliseconds()
        val minorVersionDate = LocalDateTime(2024, 1, 9, 0, 0).toInstant(TimeZone.of("UTC+0")).toEpochMilliseconds()
        val majorVersionDate = LocalDateTime(2024, 1, 19, 0, 0).toInstant(TimeZone.of("UTC+0")).toEpochMilliseconds()


        val versions = listOf(
            ArtifactVersion.create(versionNumber = "1.0.0", releaseDate = usedVersionDate),
            ArtifactVersion.create(versionNumber = "1.0.1", releaseDate = intermediateVersionDate),
            ArtifactVersion.create(versionNumber = "1.0.2", releaseDate = patchVersionDate),
            ArtifactVersion.create(versionNumber = "1.1.0", releaseDate = minorVersionDate),
            ArtifactVersion.create(versionNumber = "2.0.0", releaseDate = majorVersionDate),
        )

        val usedVersionDate2 = LocalDateTime(2024, 1, 1, 0, 0).toInstant(TimeZone.of("UTC+0")).toEpochMilliseconds()
        val intermediateVersionDate2 =
            LocalDateTime(2024, 2, 2, 0, 0).toInstant(TimeZone.of("UTC+0")).toEpochMilliseconds()
        val patchVersionDate2 = LocalDateTime(2024, 2, 3, 0, 0).toInstant(TimeZone.of("UTC+0")).toEpochMilliseconds()
        val minorVersionDate2 = LocalDateTime(2024, 2, 9, 0, 0).toInstant(TimeZone.of("UTC+0")).toEpochMilliseconds()
        val majorVersionDate2 = LocalDateTime(2024, 2, 19, 0, 0).toInstant(TimeZone.of("UTC+0")).toEpochMilliseconds()


        val versions2 = listOf(
            ArtifactVersion.create(versionNumber = "0.0.1", releaseDate = usedVersionDate2),
            ArtifactVersion.create(versionNumber = "0.0.2", releaseDate = intermediateVersionDate2),
            ArtifactVersion.create(versionNumber = "0.0.3", releaseDate = patchVersionDate2),
            ArtifactVersion.create(versionNumber = "0.5.2", releaseDate = minorVersionDate2),
            ArtifactVersion.create(versionNumber = "2.1.2", releaseDate = majorVersionDate2),
        )

        val usedVersionDate3 = LocalDateTime(2024, 1, 1, 0, 0).toInstant(TimeZone.of("UTC+0")).toEpochMilliseconds()
        val intermediateVersionDate3 =
            LocalDateTime(2024, 3, 2, 0, 0).toInstant(TimeZone.of("UTC+0")).toEpochMilliseconds()
        val patchVersionDate3 = LocalDateTime(2024, 3, 3, 0, 0).toInstant(TimeZone.of("UTC+0")).toEpochMilliseconds()
        val minorVersionDate3 = LocalDateTime(2024, 3, 9, 0, 0).toInstant(TimeZone.of("UTC+0")).toEpochMilliseconds()
        val majorVersionDate3 = LocalDateTime(2024, 3, 19, 0, 0).toInstant(TimeZone.of("UTC+0")).toEpochMilliseconds()


        val versions3 = listOf(
            ArtifactVersion.create(versionNumber = "1.0.0", releaseDate = usedVersionDate3),
            ArtifactVersion.create(versionNumber = "1.0.2", releaseDate = intermediateVersionDate3),
            ArtifactVersion.create(versionNumber = "1.0.4", releaseDate = patchVersionDate3),
            ArtifactVersion.create(versionNumber = "1.2.0", releaseDate = minorVersionDate3),
            ArtifactVersion.create(versionNumber = "2.1.3", releaseDate = majorVersionDate3),
        )

        return listOf(
            Artifact(artifactId = "artifact One", groupId = "group one", versions = versions),
            Artifact(artifactId = "artifact two", groupId = "group two", versions = versions2),
            Artifact(artifactId = "artifact three", groupId = "group three", versions = versions3),
            Artifact(artifactId = "artifact four", groupId = "group four", versions = versions),
            Artifact(artifactId = "artifact five", groupId = "group five", versions = versions),
            Artifact(artifactId = "artifact six", groupId = "group six", versions = versions),
        )
    }

    private fun getGraphs(): Project {
        val artifacts: List<Artifact> = setupArtifacts()
        val graph: Map<String, DependencyGraph> = mapOf(
            "compile" to DependencyGraph(
                nodes = listOf(
                    DependencyNode.create(0, "1.0.0"),  // libdays major - 18
                    DependencyNode.create(1, "0.0.1"), // libdays major - 49
                    DependencyNode.create(2, "1.0.0"), // libdays major - 78
                    DependencyNode.create(3, "1.0.1"), // libdays major - 17
                    DependencyNode.create(4, "1.0.0"), // libdays major - 18
                    DependencyNode.create(5, "2.0.0"), // libdays major - 0
                ), // 78 + 49 + 18 + 0
                edges = listOf(
                    DependencyEdge(0, 2),
                    DependencyEdge(0, 1),
                    DependencyEdge(1, 2),
                    DependencyEdge(3, 4),
                    DependencyEdge(3, 5),
                ),
                directDependencyIndices = listOf(0, 3)
            )
        )

        return Project(
            artifacts = artifacts,
            graph = graph,
            ecosystem = ""
        )
    }

    @Test
    fun calculateTechLagStatsEmptyGraph() {

        val service = TechnicalLagStatisticsService()

        service.connectDependenciesToStats(
            graphs = Project(
                ecosystem = ""
            )
        )

        // The only outcome we want to check is that we don't run in a runtime exception with an
        // empty data structure
    }

    @Test
    fun calculateStatsDirectDepsOnly() {

        val service = TechnicalLagStatisticsService()
        val graphs = getGraphs()
        service.connectDependenciesToStats(
            graphs
        )
        val graph = graphs.graph.values.first()
        val directDepsStats = graph.getDirectDependencyStats()
        // 18 + 17 = 17.5
        assertEquals(17.5, directDepsStats[VersionType.Major]?.libDays?.average)
    }

    @Test
    fun calculateStatsTransitiveDepsOnly() {

        val service = TechnicalLagStatisticsService()
        val graphs = getGraphs()
        service.connectDependenciesToStats(
            graphs
        )
        val graph = graphs.graph.values.first()
        val directDepsStats = graph.getTransitiveDependencyStats()
        // 78 + 49 + 18 + 0 + 78
        assertEquals(44.6, directDepsStats[VersionType.Major]?.libDays?.average)
        assertNotEquals(
            directDepsStats[VersionType.Major]?.libDays?.average,
            graph.rootDependency.statContainer.getStatForVersionType(VersionType.Major)?.libDays?.average
        )
    }

    @Test
    fun calculateTechLagStatsIdenticalData() {

        val service = TechnicalLagStatisticsService()
        val graphs = getIdenticalVersionsGraph()
        service.connectDependenciesToStats(
            graphs
        )

        val root = graphs.graph.values.first().rootDependency
        root.children.forEach {
            val majorStats = it.statContainer.getStatForVersionType(VersionType.Major)
            assertEquals(18.0, majorStats?.libDays?.average ?: -1)
            assertEquals(0.0, majorStats?.libDays?.stdDev ?: -1)
            assertEquals(3.0, majorStats?.missedReleases?.average ?: -1)
            assertEquals(
                Triple(
                    Statistics(
                        average = 1.0,
                        stdDev = 0.0,
                        variance = 0.0,
                    ),
                    Statistics(
                        average = 0.0,
                        stdDev = 0.0,
                        variance = 0.0,
                    ),
                    Statistics(
                        average = 0.0,
                        stdDev = 0.0,
                        variance = 0.0,
                    )
                ),
                majorStats?.distance ?: -1
            )

            val minorStats = it.statContainer.getStatForVersionType(VersionType.Minor)
            assertEquals(8.0, minorStats?.libDays?.average ?: -1)
            assertEquals(0.0, minorStats?.libDays?.stdDev ?: -1)
            assertEquals(2.0, minorStats?.missedReleases?.average ?: -1)
            assertEquals(
                Triple(
                    Statistics(
                        average = 0.0,
                        stdDev = 0.0,
                        variance = 0.0,
                    ),
                    Statistics(
                        average = 1.0,
                        stdDev = 0.0,
                        variance = 0.0,
                    ),
                    Statistics(
                        average = 0.0,
                        stdDev = 0.0,
                        variance = 0.0,
                    )
                ),
                minorStats?.distance ?: -1
            )

            val patchStats = it.statContainer.getStatForVersionType(VersionType.Patch)
            assertEquals(2.0, patchStats?.libDays?.average ?: -1)
            assertEquals(0.0, patchStats?.libDays?.stdDev ?: -1)
            assertEquals(1.0, patchStats?.missedReleases?.average ?: -1)
            assertEquals(
                Triple(
                    Statistics(
                        average = 0.0,
                        stdDev = 0.0,
                        variance = 0.0,
                    ),
                    Statistics(
                        average = 0.0,
                        stdDev = 0.0,
                        variance = 0.0,
                    ),
                    Statistics(
                        average = 1.0,
                        stdDev = 0.0,
                        variance = 0.0,
                    )
                ),
                patchStats?.distance ?: -1
            )
        }
    }

    @Test
    fun calculateTechLagStats() {

        val service = TechnicalLagStatisticsService()
        val graphs = getGraphs()
        service.connectDependenciesToStats(
            graphs
        )

        val root = graphs.graph.values.first().rootDependency
        val firstDirectDep = root.children.first()
        // first contains data for nodes 0, 1, 2, 2
        val firstDepMajorStats = firstDirectDep.statContainer.getStatForVersionType(VersionType.Major)
        //  49, 78, 78 - avg. 55.75
        // sqrt(((49-55.75)^2 + (78-55.75)^2 + (78-55.75)^2) / 4)
        println(firstDepMajorStats)
        val avg = (49.0 + 78.0 + 78.0) / 3.0
        assertEquals(avg, firstDepMajorStats?.libDays?.average ?: -1)
        assertEquals(
            sqrt(
                ((49 - avg).pow(2) + (78 - avg).pow(
                    2
                ) + (78 - avg).pow(2)) / 3
            ), firstDepMajorStats?.libDays?.stdDev ?: -1
        )

        assertEquals(4.0, firstDepMajorStats?.missedReleases?.average ?: -1)
        assertEquals(0.0, firstDepMajorStats?.missedReleases?.stdDev ?: -1)

        val secondDirectDep = root.children[1]

        val secondDepMajorStats = secondDirectDep.statContainer.getStatForVersionType(VersionType.Major)

        println(secondDepMajorStats)
        val secondAvg = (18.0) / 2.0
        assertEquals(secondAvg, secondDepMajorStats?.libDays?.average ?: -1)
        assertEquals(
            sqrt(
                ((18 - secondAvg).pow(2) + (0 - secondAvg).pow(
                    2
                )) / 2.0
            ), secondDepMajorStats?.libDays?.stdDev ?: -1
        )
        // (0 + 4 ) / 2
        assertEquals(4.0 / 2.0, secondDepMajorStats?.missedReleases?.average ?: -1)
        assertEquals(
            2.0, secondDepMajorStats?.missedReleases?.stdDev ?: -1
        )

        // TODO: extend test with distance checks

    }


}
