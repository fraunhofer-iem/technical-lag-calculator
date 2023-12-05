package dependencies

import artifact.ArtifactService
import http.deps.DepsClient
import io.ktor.client.*
import io.ktor.client.engine.mock.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.http.*
import io.ktor.serialization.kotlinx.json.*
import io.ktor.utils.io.*
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.Json
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

import org.junit.jupiter.api.Assertions.*
import java.nio.file.Paths

class DependencyAnalyzerTest {

    private lateinit var artifactService: ArtifactService

    @OptIn(ExperimentalCoroutinesApi::class)
    @BeforeEach
    fun setUp() {
        val viteNoDefaults = this.javaClass.classLoader.getResource("npm/apiResponseViteNoDefaults.json")?.readText()
        val vite = this.javaClass.classLoader.getResource("npm/apiResponseVite.json")?.readText()
        if (vite != null && viteNoDefaults != null) {
            val mockEngine = MockEngine { request ->
                if (request.url.toString() == "https://api.deps.dev/v3alpha/systems/npm/packages/viteNoDefaults") {
                    respond(
                        content = ByteReadChannel(viteNoDefaults, Charsets.UTF_8),
                        status = HttpStatusCode.OK,
                        headers = headersOf(HttpHeaders.ContentType, "application/json")
                    )
                } else {
                    respond(
                        content = ByteReadChannel(vite, Charsets.UTF_8),
                        status = HttpStatusCode.OK,
                        headers = headersOf(HttpHeaders.ContentType, "application/json")
                    )
                }
            }
            val httpClient = HttpClient(mockEngine) {
                install(ContentNegotiation) {
                    json(
                        Json { ignoreUnknownKeys = true }
                    )
                }
            }
            artifactService = ArtifactService(DepsClient(
                httpClient = httpClient,
                ioDispatcher = UnconfinedTestDispatcher()
            )
            )
        }
    }

    @Test
    fun getDependencyPackagesForProject() = runTest {
        val dependencyAnalyzer = DependencyAnalyzer(artifactService)
        val resourceDirectory = Paths.get("src","test","resources", "npm").toAbsolutePath()
        val file = resourceDirectory.toFile()

        val results = dependencyAnalyzer.getDependencyPackagesForProject(file)

        println(results)
    }
}