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
import java.net.URLEncoder
import java.time.OffsetDateTime
import java.time.format.DateTimeFormatter

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

    suspend fun getVersionsForPackage(type: String, namespace: String, name: String): List<VersionDto> {
        val requestUrl: String? = getRequestUrl(
            type = type,
            namespace = namespace,
            name = name
        )

        return if (requestUrl != null) {

            val responseDto = try {
                val response = httpClient.request(requestUrl)

                val currentResponse = response.body<DepsResponseDto>()
                println("API response:${currentResponse}")

                currentResponse
            } catch (exception: Exception) {
                println("Exception during http call to $requestUrl. $exception")

                null
            }

            responseDto?.versions?.mapNotNull { version ->
                versionResponseToDto(version)
            } ?: emptyList()
        } else {
            println("Currently unsupported package manager")
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
            println("Insufficient data in maven response to create version dto")
            println(version)
            null
        }
    }

    private enum class UrlConcatenationSymbol(val concatSymbol: String) {
        MAVEN_AND_GRADLE(":"),
        NPM("/")
    }

    private suspend fun getRequestUrl(type: String, name: String, namespace: String): String? {
        return when (type.lowercase()) {
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

    private fun dateToMs(dateString: String): Long {
        val formatter: DateTimeFormatter = DateTimeFormatter.ISO_OFFSET_DATE_TIME
        val dateTime: OffsetDateTime = OffsetDateTime.parse(dateString, formatter)
        return dateTime.toInstant().toEpochMilli()
    }
}
