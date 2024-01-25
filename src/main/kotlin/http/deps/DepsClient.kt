package http.deps

import artifact.model.VersionDto
import http.deps.model.DepsResponseDto
import http.deps.model.Version
import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.engine.cio.*
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
    private val httpClient: HttpClient = HttpClient(CIO) {
        install(ContentNegotiation) {
            json(
                Json { ignoreUnknownKeys = true }
            )
        }
    },
    private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO
) {

    suspend fun getVersionsForPackage(ecosystem: String, namespace: String = "", name: String): List<VersionDto> {
        val requestUrl: String? = getRequestUrl(
            ecosystem = ecosystem,
            namespace = namespace,
            name = name
        )

        return if (requestUrl != null) {

            val responseDto = try {
                val response = httpClient.request(requestUrl)

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
            logger.warn { "Insufficient data in maven response to create version dto $version" }
            null
        }
    }

    private enum class UrlConcatenationSymbol(val concatSymbol: String) {
        MAVEN_AND_GRADLE(":"),
        NPM("/")
    }

    private suspend fun getRequestUrl(ecosystem: String, name: String, namespace: String): String? {
        return when (ecosystem.lowercase()) {
            "maven", "gradle" -> {
                val urlNamespace = concatNamespaceAndName(
                    name = name,
                    namespace = namespace,
                    UrlConcatenationSymbol.MAVEN_AND_GRADLE.concatSymbol
                )
                "https://api.deps.dev/v3alpha/systems/maven/packages/$urlNamespace"
            }

            "npm" -> {
                val urlNamespace = concatNamespaceAndName(
                    name = name,
                    namespace = namespace,
                    UrlConcatenationSymbol.NPM.concatSymbol
                )
                "https://api.deps.dev/v3alpha/systems/npm/packages/$urlNamespace"
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
