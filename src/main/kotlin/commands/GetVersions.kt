package commands

import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.parameters.options.option
import com.github.ajalt.clikt.parameters.options.required
import com.github.ajalt.clikt.parameters.types.path
import kotlinx.coroutines.runBlocking
import vulnerabilities.VulnerabilityVersionDownloader

class GetVersions : CliktCommand() {
    private val inputPath by option(
        envvar = "INPUT_PATH", help = "Path to the folder in which the vulnerability information are stored."
    )
        .path(mustExist = false, canBeFile = false)
        .required()

    override fun run() = runBlocking {
        val vulnerabilityVersionDownloader = VulnerabilityVersionDownloader()
        vulnerabilityVersionDownloader.storeVersionsForVulnerablePackages(inputPath)
    }
}
