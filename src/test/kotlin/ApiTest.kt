//import http.deps.DepsClient
//import io.ktor.client.*
//import io.ktor.client.engine.mock.*
//import io.ktor.client.plugins.contentnegotiation.*
//import io.ktor.http.*
//import io.ktor.serialization.kotlinx.json.*
//import io.ktor.utils.io.*
//import kotlinx.coroutines.runBlocking
//import kotlinx.serialization.json.Json
//import kotlin.test.*
//
//
//class ApiTest {
//
//
//    /**
//     * This test contains incomplete and invalid entries for the JSON response. We want to check
//     * whether the application crashes and if we return all correctly formatted entries as DTOs.
//     */
//    @Test
//    fun parseApiResponseTest() {
//        runBlocking {
//            this.javaClass.classLoader.getResource("npm/apiResponseVite.json")?.readText()?.let { jsonText ->
//
//                val mockEngine = MockEngine { request ->
//                    println(request.url.toString())
//                    respond(
//                        content = ByteReadChannel(jsonText, Charsets.UTF_8),
//                        status = HttpStatusCode.OK,
//                        headers = headersOf(HttpHeaders.ContentType, "application/json")
//                    )
//                }
//                val httpClient = HttpClient(mockEngine) {
//                    install(ContentNegotiation) {
//                        json(
//                            Json { ignoreUnknownKeys = true }
//                        )
//                    }
//                }
//                val apiClient = DepsClient(
//                    httpClient = httpClient
//                )
//                val versions = apiClient.getVersionsForPackage(
//                    ecosystem = "npm",
//                    namespace = "",
//                    name = "vite"
//                )
//                assertEquals(4, versions.count())
//                assertEquals(1, versions.count { it.isDefault })
//            }
//
//        }
//    }
//
//    @Test
//    fun urlEncoding() {
//        runBlocking {
//
//            val mockEngine = MockEngine { request ->
//                println(request.url.toString())
//                if (request.url.toString() != "https://api.deps.dev/v3alpha/systems/npm/packages/%40test%2F%40vite"
//                    && request.url.toString() != "https://api.deps.dev/v3alpha/systems/maven/packages/%40test%3A%40vite"
//                ) {
//                    fail("URL format is invalid")
//                }
//                respond(
//                    content = ByteReadChannel(
//                        "{" +
//                                "  \"packageKey\": {" +
//                                "    \"system\": \"NPM\"," +
//                                "    \"name\": \"vite\"" +
//                                "  }," +
//                                "  \"versions\":[] }", Charsets.UTF_8
//                    ),
//                    status = HttpStatusCode.OK,
//                    headers = headersOf(HttpHeaders.ContentType, "application/json")
//                )
//            }
//            val httpClient = HttpClient(mockEngine) {
//                install(ContentNegotiation) {
//                    json(
//                        Json { ignoreUnknownKeys = true }
//                    )
//                }
//            }
//            val apiClient = DepsClient(
//                httpClient = httpClient
//            )
//
//            apiClient.getVersionsForPackage(
//                ecosystem = "npm",
//                namespace = "@test",
//                name = "@vite"
//            )
//            apiClient.getVersionsForPackage(
//                ecosystem = "maven",
//                namespace = "@test",
//                name = "@vite"
//            )
//
//        }
//
//    }
//}
