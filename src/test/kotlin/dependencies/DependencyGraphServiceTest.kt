package dependencies

import artifact.model.ArtifactVersion
import http.deps.DepsClient
import io.mockk.coEvery
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Test
import org.ossreviewtoolkit.model.DependencyGraph
import org.ossreviewtoolkit.model.DependencyReference
import org.ossreviewtoolkit.model.Identifier
import org.ossreviewtoolkit.model.RootDependencyIndex
import kotlin.test.assertEquals

class DependencyGraphServiceTest {


    private fun setupTree(): DependencyGraph {
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
        val refConfig = DependencyReference(2, dependencies = sortedSetOf(refLang, refCollections, refCollectionsLast))
        val refCsv = DependencyReference(3, dependencies = sortedSetOf(refConfig))
        val fragments = sortedSetOf(DependencyGraph.DEPENDENCY_REFERENCE_COMPARATOR, refCsv)
        val scopeMap = mapOf("scope" to listOf(RootDependencyIndex(3)))

        return DependencyGraph(ids, fragments, scopeMap)
    }


    @Test
    fun transformDependencyGraph() = runTest {

        val mockDepsClient = mockk<DepsClient>()
        coEvery { mockDepsClient.getVersionsForPackage(any(), any(), any()) } returns listOf(
            ArtifactVersion(versionNumber = "3.11", releaseDate = 0L),
            ArtifactVersion(versionNumber = "4.4.3", releaseDate = 0L),
            ArtifactVersion(versionNumber = "2.4", releaseDate = 0L),
            ArtifactVersion(versionNumber = "5", releaseDate = 0L),
            ArtifactVersion(versionNumber = "2.11", releaseDate = 0L),
            ArtifactVersion(versionNumber = "5.1.2", releaseDate = 0L),
        )

        coEvery { mockDepsClient.getDepsForPackage(any(), any(), any(), any()) } returns null

        val dependencyGraphService = DependencyGraphService(depsClient = mockDepsClient)
        val graph = setupTree()

        val transformedGraphs = dependencyGraphService.transformDependencyGraph(
            mapOf("npm" to graph)
        )

        // checks whether we correctly parse artifacts and versions
        assertEquals(4, transformedGraphs.first().artifacts.count())
        assertEquals(6, transformedGraphs.first().artifacts.first().versions.count())


        val transformedGraph = transformedGraphs.first().graph.values.first()

        assertEquals(5, transformedGraph.nodes.count())

        // Check if the edges connect the correct nodes which are linked to the correct artifacts
        assertEquals(4, transformedGraph.edges.count())

        val rootNode = transformedGraph.nodes[transformedGraph.directDependencyIndices.first()]
        val rootArtifact = transformedGraphs.first().artifacts[rootNode.artifactIdx]
        assertEquals("commons-configuration", rootArtifact.artifactId)

        val directDeps = transformedGraph.linkedDirectDependencies
        assertEquals(1, directDeps.count())
        assertEquals(1, directDeps.first().children.count())
        assertEquals(3, directDeps.first().children.first().children.count())
    }

    @Test
    fun transformDependencyGraphWithUpdates() = runTest {

        val mockDepsClient = mockk<DepsClient>()
        coEvery { mockDepsClient.getVersionsForPackage(any(), any(), any()) } returns listOf(
            ArtifactVersion(versionNumber = "3.11", releaseDate = 0L),
            ArtifactVersion(versionNumber = "4.4.3", releaseDate = 0L),
            ArtifactVersion(versionNumber = "2.4", releaseDate = 0L),
            ArtifactVersion(versionNumber = "5", releaseDate = 0L),
            ArtifactVersion(versionNumber = "2.11", releaseDate = 0L),
            ArtifactVersion(versionNumber = "5.1.2", releaseDate = 0L),
        )

        coEvery { mockDepsClient.getDepsForPackage(any(), any(), any(), any()) } returns null

        val dependencyGraphService = DependencyGraphService(depsClient = mockDepsClient)
        val graph = setupTree()

        val transformedGraphs = dependencyGraphService.transformDependencyGraph(
            mapOf("npm" to graph)
        )

    }
}