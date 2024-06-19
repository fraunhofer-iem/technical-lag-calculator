package dependencies

import dependencies.graph.ArtifactVersion
import dependencies.graph.VersionType
import http.deps.DepsClient
import http.deps.model.DepsTreeResponseDto
import http.deps.model.Edge
import http.deps.model.Node
import http.deps.model.VersionKeyX
import io.mockk.coEvery
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toInstant
import org.junit.jupiter.api.Test
import org.ossreviewtoolkit.model.DependencyGraph
import org.ossreviewtoolkit.model.DependencyReference
import org.ossreviewtoolkit.model.Identifier
import org.ossreviewtoolkit.model.RootDependencyIndex
import technicalLag.model.TechnicalLagDto
import kotlin.test.assertEquals


class LinkedArtifactDependenciesGraphServiceTest {

    companion object {
        fun setupTree(): DependencyGraph {
            val ids = listOf(
                Identifier(
                    "npm",
                    "org.apache.commons",
                    "commons-lang3",
                    "2.11"
                ), // TODO: this is an important test case. this must not become an artifact, but must be part of the tree
                Identifier("npm", "org.apache.commons", "commons-lang3", "3.11"),
                Identifier("npm", "org.apache.commons", "commons-collections4", "4.4.3"),
                Identifier("npm", "org.apache.commons", "commons-configuration", "2.4"),
                Identifier(type = "npm", namespace = "org.junit", name = "junit", version = "5")
            )

            val refLang = DependencyReference(0)
            val refCollections = DependencyReference(1)
            val refCollectionsLast = DependencyReference(4)
            val refConfig =
                DependencyReference(2, dependencies = sortedSetOf(refLang, refCollections, refCollectionsLast))
            val refCsv = DependencyReference(3, dependencies = sortedSetOf(refConfig))
            val fragments = sortedSetOf(DependencyGraph.DEPENDENCY_REFERENCE_COMPARATOR, refCsv)
            val scopeMap = mapOf("scope" to listOf(RootDependencyIndex(3), RootDependencyIndex(1)))

            return DependencyGraph(ids, fragments, scopeMap)
        }

    }


    @Test
    fun transformDependencyGraph() = runTest {

        val mockDepsClient = mockk<DepsClient>()
        coEvery { mockDepsClient.getVersionsForPackage(any(), any(), any()) } returns listOf(
            ArtifactVersion.create(versionNumber = "3.11", releaseDate = 0L),
            ArtifactVersion.create(versionNumber = "4.4.3", releaseDate = 0L),
            ArtifactVersion.create(versionNumber = "2.4", releaseDate = 0L),
            ArtifactVersion.create(versionNumber = "5", releaseDate = 0L),
            ArtifactVersion.create(versionNumber = "2.11", releaseDate = 0L),
            ArtifactVersion.create(versionNumber = "5.1.2", releaseDate = 0L),
        )

        coEvery { mockDepsClient.getDepsForPackage(any(), any(), any(), any()) } returns null

        val dependencyGraphService = DependencyGraphService(depsClient = mockDepsClient)
        val graph = setupTree()

        val transformedGraphs = dependencyGraphService.transformDependencyGraph(
            mapOf("npm" to graph)
        )

        // checks whether we correctly parse artifacts and versions
        assertEquals(4, transformedGraphs.first().artifacts.count())
        assertEquals(6, transformedGraphs.first().artifacts.first().sortedVersions.count())


        val transformedGraph = transformedGraphs.first().graph.values.first()

        assertEquals(6, transformedGraph.nodes.count())

        // Check if the edges connect the correct nodes which are linked to the correct artifacts
        assertEquals(4, transformedGraph.edges.count())

        val rootNode = transformedGraph.nodes[transformedGraph.directDependencyIndices.first()]
        val rootArtifact = transformedGraphs.first().artifacts[rootNode.artifactIdx]
        assertEquals("commons-configuration", rootArtifact.artifactId)

        val directDeps = transformedGraph.rootDependency
        assertEquals(2, directDeps.children.count())
        assertEquals(1, directDeps.children.first().children.count())
        assertEquals(3, directDeps.children.first().children.first().children.count())
    }

    @Test
    fun transformDependencyGraphWithUpdates() = runTest {

        val mockDepsClient = mockk<DepsClient>()
        coEvery { mockDepsClient.getVersionsForPackage(any(), any(), any()) } returns listOf(
            ArtifactVersion.create(versionNumber = "3.11", releaseDate = 0L),
            ArtifactVersion.create(versionNumber = "3.12", releaseDate = 0L),
            ArtifactVersion.create(versionNumber = "4.4.3", releaseDate = 0L),
            ArtifactVersion.create(versionNumber = "2.4", releaseDate = 0L),
            ArtifactVersion.create(versionNumber = "5", releaseDate = 0L),
            ArtifactVersion.create(versionNumber = "2.11", releaseDate = 0L),
            ArtifactVersion.create(versionNumber = "5.1.2", releaseDate = 0L),
        )

        coEvery { mockDepsClient.getDepsForPackage(any(), any(), any(), any()) } returns null
        coEvery {
            mockDepsClient.getDepsForPackage(
                ecosystem = "npm",
                groupId = "org.apache.commons",
                artifactId = "commons-lang3",
                version = "3.12.0"
            )
        } returns DepsTreeResponseDto(
            nodes = listOf(
                Node(
                    bundled = false,
                    relation = "",
                    versionKey = VersionKeyX(
                        name = "org.apache.commons/commons-lang3",
                        system = "npm",
                        version = "3.12"
                    )
                ),
                Node(
                    bundled = false,
                    relation = "",
                    versionKey = VersionKeyX(
                        name = "org.apache.commons/commons-configuration", // existing artifact
                        system = "npm",
                        version = "2.11"
                    )
                ),
                Node(
                    bundled = false,
                    relation = "",
                    versionKey = VersionKeyX(
                        name = "new/artifact", // new artifact
                        system = "npm",
                        version = "5.1.2"
                    )
                ),
            ),
            edges = mutableListOf(
                Edge(fromNode = 0, toNode = 1, requirement = null),
                Edge(fromNode = 0, toNode = 2, requirement = null),
            ),
            error = null
        )

        val dependencyGraphService = DependencyGraphService(depsClient = mockDepsClient)
        val graph = setupTree()

        val transformedGraphs = dependencyGraphService.transformDependencyGraph(
            mapOf("npm" to graph)
        )

        println(transformedGraphs.first().graphs)

    }

    @Test
    fun getTechnicalLagForGraph() = runTest {
        val mockDepsClient = mockk<DepsClient>()
        val usedVersionDate = LocalDateTime(2024, 1, 1, 0, 0).toInstant(TimeZone.of("UTC+3")).toEpochMilliseconds()
        val patchVersionDate = LocalDateTime(2024, 1, 3, 0, 0).toInstant(TimeZone.of("UTC+3")).toEpochMilliseconds()
        val minorVersionDate = LocalDateTime(2024, 1, 9, 0, 0).toInstant(TimeZone.of("UTC+3")).toEpochMilliseconds()
        val majorVersionDate = LocalDateTime(2024, 1, 19, 0, 0).toInstant(TimeZone.of("UTC+3")).toEpochMilliseconds()



        coEvery { mockDepsClient.getVersionsForPackage(any(), any(), any()) } returns listOf(
        )
        coEvery { mockDepsClient.getVersionsForPackage("npm", "org.apache.commons", "commons-lang3") } returns listOf(
            ArtifactVersion.create(versionNumber = "3.11", releaseDate = usedVersionDate),
            ArtifactVersion.create(versionNumber = "3.11.3", releaseDate = patchVersionDate),
            ArtifactVersion.create(versionNumber = "3.12", releaseDate = 0L),
            ArtifactVersion.create(versionNumber = "3.12.3", releaseDate = minorVersionDate),
            ArtifactVersion.create(versionNumber = "4.12.3", releaseDate = majorVersionDate),
        )

        coEvery { mockDepsClient.getDepsForPackage(any(), any(), any(), any()) } returns null

        val dependencyGraphService = DependencyGraphService(depsClient = mockDepsClient)
        val graph = setupTree()

        val transformedGraphs = dependencyGraphService.transformDependencyGraph(
            mapOf("npm" to graph)
        )

        val artifact =
            transformedGraphs.first().artifacts.find { it.artifactId == "commons-lang3" && it.groupId == "org.apache.commons" }!!

        val lagMajor =
            artifact.getTechLagForVersion(rawVersion = "3.11", versionType = VersionType.Major)
        val expectedMajorLag = TechnicalLagDto(
            libDays = 18,
            distance = Triple(1, 0, 0),
            version = "4.12.3",
            numberOfMissedReleases = 4,
        )
        assertEquals(expectedMajorLag, lagMajor)

        val lagMinor =
            artifact.getTechLagForVersion(rawVersion = "3.11", versionType = VersionType.Minor)
        val expectedMinorLag = TechnicalLagDto(
            libDays = 8,
            distance = Triple(0, 1, 1),
            version = "3.12.3",
            numberOfMissedReleases = 3,
        )
        assertEquals(expectedMinorLag, lagMinor)

        val lagPatch =
            artifact.getTechLagForVersion(rawVersion = "3.11", versionType = VersionType.Patch)
        val expectedPatchLag = TechnicalLagDto(
            libDays = 2,
            distance = Triple(0, 0, 1),
            version = "3.11.3",
            numberOfMissedReleases = 1,
        )
        assertEquals(expectedPatchLag, lagPatch)

        val lagNewest =
            artifact.getTechLagForVersion(rawVersion = "4.12.3", versionType = VersionType.Major)
        val noLagExpected = TechnicalLagDto(
            libDays = 0,
            distance = Triple(0, 0, 0),
            version = "4.12.3",
            numberOfMissedReleases = 0,
        )
        assertEquals(noLagExpected, lagNewest)

        val lagNewestPatch =
            artifact.getTechLagForVersion(rawVersion = "4.12.3", versionType = VersionType.Patch)
        val noLagExpectedPatch = TechnicalLagDto(
            libDays = 0,
            distance = Triple(0, 0, 0),
            version = "4.12.3",
            numberOfMissedReleases = 0,
        )
        assertEquals(lagNewestPatch, noLagExpectedPatch)
    }
}