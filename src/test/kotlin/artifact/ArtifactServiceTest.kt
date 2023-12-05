package artifact

import artifact.model.PackageReferenceDto
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
import kotlin.test.Test
import kotlin.test.assertEquals


class ArtifactServiceTest {

    private lateinit var apiClient: DepsClient

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
            apiClient = DepsClient(
                httpClient = httpClient,
                ioDispatcher = UnconfinedTestDispatcher()
            )
        }
    }

    @Test
    fun getAllTransitiveVersionInformation() = runTest {

        val artifactService = ArtifactService(

            depsClient = apiClient
        )

        val packageDto = PackageReferenceDto(
            name = "vite",
            namespace = "", type = "NPM", version = "5.0.4",
            dependencies = listOf()
        )

        val artifact = artifactService.getAllTransitiveVersionInformation(
            packageDto
        )
        artifact?.libyear?.let { libyear ->
            assertEquals(0, libyear)
        }

        val packageDtoOldVersion = PackageReferenceDto(
            name = "vite",
            namespace = "", type = "NPM", version = "3.1.1",
            dependencies = listOf()
        )

        val artifactOld = artifactService.getAllTransitiveVersionInformation(
            packageDtoOldVersion
        )

        artifactOld?.libyear?.let { libyear ->
            assertEquals(-14, libyear)
        }
    }

    @Test
    fun noDefaults() = runTest {

        val artifactService = ArtifactService(

            depsClient = apiClient
        )

        val packageDto = PackageReferenceDto(
            name = "viteNoDefaults",
            namespace = "", type = "NPM", version = "5.0.4",
            dependencies = listOf()
        )

        val artifact = artifactService.getAllTransitiveVersionInformation(
            packageDto
        )

        artifact?.libyear?.let { libyear ->
            assertEquals(-1, libyear)

        }
    }

}