package vulnerabilities

import kotlinx.serialization.json.Json
import vulnerabilities.dto.osv.OpenSourceVulnerabilityFormat
import java.nio.file.Path

object VulnerabilityHelper {

    private val json = Json { ignoreUnknownKeys = true }
    fun readVulnerabilitiesFromFile(filePath: Path): List<OpenSourceVulnerabilityFormat> {
        val directory = filePath.toFile()

        return if (directory.isDirectory) {
            directory.listFiles()?.filter { it.extension == "json" }?.map { jsonFile ->

                json.decodeFromString<OpenSourceVulnerabilityFormat>(jsonFile.readText())
            } ?: emptyList()
        } else {
            emptyList()
        }
    }
}
