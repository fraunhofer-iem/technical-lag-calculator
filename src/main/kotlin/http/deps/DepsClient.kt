package http.deps

import artifact.model.VersionDto
import http.deps.model.DepsResponseDto
import http.deps.model.DepsTreeResponseDto
import http.deps.model.Version
import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.engine.apache.*
import io.ktor.client.engine.apache5.*
import io.ktor.client.plugins.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.client.request.*
import io.ktor.serialization.kotlinx.json.*
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import org.apache.logging.log4j.kotlin.logger
import util.TimeHelper.dateToMs
import java.net.URLEncoder

class DepsClient(
    private val httpClient: HttpClient = HttpClient(Apache) {
        engine {
            followRedirects = true
            socketTimeout = 10_000
            connectTimeout = 10_000
            connectionRequestTimeout = 20_000
        }
        install(ContentNegotiation) {
            json(
                Json { ignoreUnknownKeys = true }
            )
        }
        install(HttpTimeout) {
            requestTimeoutMillis = 15000
            connectTimeoutMillis = 15000
            socketTimeoutMillis = 15000
        }
        install(HttpRequestRetry) {
            retryOnExceptionOrServerErrors(5)
            exponentialDelay()
        }
    },
    private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO
) {

    fun close() {
        httpClient.close()
    }

    suspend fun getVersionsForPackage(ecosystem: String, namespace: String = "", name: String): List<VersionDto> {
        val requestUrl: String? = getVersionsRequestUrl(
            ecosystem = ecosystem,
            namespace = namespace,
            name = name
        )

        return if (requestUrl != null) {

            val responseDto = try {
                val response = httpClient.get(requestUrl)

                val currentResponse = response.body<DepsResponseDto>()

                currentResponse
            } catch (exception: Exception) {
                logger.error { "Exception during http call to $requestUrl. $exception" }

                null
            }

            responseDto?.versions?.mapNotNull { version ->
                versionResponseToDto(version)
            } ?: emptyList()
        } else {
            logger.error { "Currently unsupported package manager" }
            emptyList()
        }
    }

    suspend fun getDepsForPackage(
        ecosystem: String,
        namespace: String = "",
        name: String,
        version: String
    ): DepsTreeResponseDto? {
        val requestUrl: String? = getDependenciesRequestUrl(
            ecosystem = ecosystem,
            namespace = namespace,
            name = name,
            version = version
        )

        return if (requestUrl != null) {

            try {
                val response = httpClient.request(requestUrl)
                response.body<DepsTreeResponseDto>()
            } catch (exception: Exception) {
                logger.error { "Exception during http call to $requestUrl. $exception" }

                null
            }


        } else {
            logger.error { "Currently unsupported package manager" }
            null
        }
    }

    private fun versionResponseToDto(version: Version): VersionDto? {
        return if (version.publishedAt != null) {
            try {
                VersionDto(
                    versionNumber = version.versionKey.version,
                    releaseDate = dateToMs(version.publishedAt),
                    isDefault = version.isDefault ?: false
                )
            } catch (exception: Exception) {
                null
            }
        } else {
            logger.warn { "Insufficient data in response to create version dto $version" }
            null
        }
    }

    private enum class UrlConcatenationSymbol(val concatSymbol: String) {
        MAVEN_AND_GRADLE(":"),
        NPM("/")
    }

    private suspend fun getVersionsRequestUrl(ecosystem: String, name: String, namespace: String): String? {
        getNameForEcosystem(name, namespace, ecosystem)?.let { urlNamespace ->
            return "https://api.deps.dev/v3/systems/$ecosystem/packages/$urlNamespace"
        }
        return null
    }


    private suspend fun getDependenciesRequestUrl(
        ecosystem: String,
        name: String,
        namespace: String,
        version: String
    ): String? {
        getNameForEcosystem(name, namespace, ecosystem)?.let { urlNamespace ->
            return "https://api.deps.dev/v3/systems/$ecosystem/packages/$urlNamespace/versions/$version:dependencies"
        }

        return null
    }

    private suspend fun getNameForEcosystem(name: String, namespace: String, ecosystem: String): String? {
        return when (ecosystem.lowercase()) {
            "maven", "gradle" -> {
                concatNamespaceAndName(
                    name = name,
                    namespace = namespace,
                    UrlConcatenationSymbol.MAVEN_AND_GRADLE.concatSymbol
                )
            }

            "npm" -> {
                concatNamespaceAndName(
                    name = name,
                    namespace = namespace,
                    UrlConcatenationSymbol.NPM.concatSymbol
                )
            }

            else -> null
        }
    }

    private suspend fun concatNamespaceAndName(name: String, namespace: String, concatSymbol: String): String {
        return withContext(ioDispatcher) {
            URLEncoder.encode(
                if (namespace.isBlank()) {
                    name
                } else {
                    "$namespace$concatSymbol$name"
                }, "UTF-8"
            )

        }
    }
}
