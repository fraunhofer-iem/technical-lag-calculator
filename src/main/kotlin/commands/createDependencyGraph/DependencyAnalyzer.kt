package commands.createDependencyGraph

import commands.createDependencyGraph.DependencyAnalyzerConfig.Companion.createDefaultConfig
import commands.createDependencyGraph.model.RawAnalyzerResult
import shared.analyzerResultDtos.EnvironmentInfoDto
import shared.analyzerResultDtos.ProjectInfoDto
import shared.analyzerResultDtos.RepositoryInfoDto
import org.apache.logging.log4j.kotlin.logger
import org.ossreviewtoolkit.analyzer.Analyzer
import org.ossreviewtoolkit.analyzer.PackageManagerFactory
import org.ossreviewtoolkit.analyzer.determineEnabledPackageManagers
import org.ossreviewtoolkit.model.OrtResult
import org.ossreviewtoolkit.model.ResolvedPackageCurations
import org.ossreviewtoolkit.model.config.AnalyzerConfiguration
import org.ossreviewtoolkit.model.config.OrtConfiguration
import org.ossreviewtoolkit.model.config.PluginConfiguration
import org.ossreviewtoolkit.model.config.RepositoryConfiguration
import org.ossreviewtoolkit.model.utils.PackageCurationProvider
import org.ossreviewtoolkit.plugins.packagecurationproviders.api.PackageCurationProviderFactory
import org.ossreviewtoolkit.plugins.packagecurationproviders.api.SimplePackageCurationProvider
import org.ossreviewtoolkit.plugins.reporters.cyclonedx.CycloneDxReporter
import org.ossreviewtoolkit.reporter.ReporterInput
import org.ossreviewtoolkit.utils.common.Options
import java.io.File


private data class DependencyAnalyzerConfig(
    val analyzerConfiguration: AnalyzerConfiguration,
    val enabledPackageManagers: Set<PackageManagerFactory>,
    val enabledCurationProviders: List<Pair<String, PackageCurationProvider>>,
    val repositoryConfiguration: RepositoryConfiguration
) {
    companion object {
        fun createDefaultConfig(): DependencyAnalyzerConfig {

            val ortConfig = OrtConfiguration()
            val analyzerConfiguration = ortConfig.analyzer.copy(allowDynamicVersions = true)
            val repositoryConfiguration = RepositoryConfiguration()
            val enabledPackageManagers = analyzerConfiguration.determineEnabledPackageManagers()

            val enabledCurationProviders = buildList {
                val repositoryPackageCurations = repositoryConfiguration.curations.packages

                if (ortConfig.enableRepositoryPackageCurations) {
                    add(
                        ResolvedPackageCurations.REPOSITORY_CONFIGURATION_PROVIDER_ID
                                to
                                SimplePackageCurationProvider(repositoryPackageCurations)
                    )
                } else if (repositoryPackageCurations.isNotEmpty()) {
                    logger.warn {
                        "Existing package curations are not applied " +
                                "because the feature is disabled."
                    }
                }

                addAll(PackageCurationProviderFactory.create(ortConfig.packageCurationProviders))
            }

            return DependencyAnalyzerConfig(
                analyzerConfiguration = analyzerConfiguration,
                enabledPackageManagers = enabledPackageManagers,
                enabledCurationProviders = enabledCurationProviders,
                repositoryConfiguration = repositoryConfiguration
            )
        }
    }
}

/**
 * This class wraps the execution of the ORT, which retrieves the initial dependency tree from the given project,
 * and then transforms the dependency tree into the internal DependencyGraphs data structure.
 * During the transformation multiple API calls to deps.dev are issued through the DependencyGraphService to retrieve the necessary version information
 * for each node, as well as information about the graph's update possibilities.
 */
internal class DependencyAnalyzer {

    private val config: DependencyAnalyzerConfig = createDefaultConfig()
    private val analyzer: Analyzer = Analyzer(config = config.analyzerConfiguration)


    fun run(
        projectPath: File,
    ): RawAnalyzerResult {

        val managedFiles = analyzer.findManagedFiles(
            absoluteProjectPath = projectPath,
            packageManagers = config.enabledPackageManagers,
            repositoryConfiguration = config.repositoryConfiguration
        )

        fun CycloneDxReporter.generateReport(result: OrtResult, options: Options): List<File> =
            generateReport(
                ReporterInput(result),
                File("/tmp/depGraphs/"),
                PluginConfiguration(options)
            )

        val results = analyzer.analyze(managedFiles, config.enabledCurationProviders)
        val jsonOptions = mapOf("single.bom" to "true", "output.file.formats" to "json")
        val sbom = CycloneDxReporter().generateReport(results, jsonOptions)
        println(sbom)
        return RawAnalyzerResult(
            repositoryInfo = RepositoryInfoDto(
                url = results.repository.vcs.url,
                revision = results.repository.vcs.revision,
                projects = results.analyzer?.result?.projects?.map {
                    ProjectInfoDto(
                        type = it.id.type,
                        namespace = it.id.namespace,
                        name = it.id.name,
                        version = it.id.version,
                    )
                }
                    ?: emptyList()
            ),
            environmentInfo = EnvironmentInfoDto(
                ortVersion = results.analyzer?.environment?.ortVersion ?: "ORT version not found",
                javaVersion = results.analyzer?.environment?.javaVersion ?: "Java version not found"
            ),
            dependencyGraphs = results.analyzer?.result?.dependencyGraphs ?: emptyMap()
        )
    }
}
