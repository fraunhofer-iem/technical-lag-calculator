package dependencies

import dependencies.model.*
import org.apache.logging.log4j.kotlin.logger
import org.ossreviewtoolkit.analyzer.Analyzer
import org.ossreviewtoolkit.analyzer.PackageManagerFactory
import org.ossreviewtoolkit.analyzer.determineEnabledPackageManagers
import org.ossreviewtoolkit.model.DependencyGraph
import org.ossreviewtoolkit.model.ResolvedPackageCurations
import org.ossreviewtoolkit.model.config.AnalyzerConfiguration
import org.ossreviewtoolkit.model.config.OrtConfiguration
import org.ossreviewtoolkit.model.config.RepositoryConfiguration
import org.ossreviewtoolkit.model.utils.PackageCurationProvider
import org.ossreviewtoolkit.plugins.packagecurationproviders.api.PackageCurationProviderFactory
import org.ossreviewtoolkit.plugins.packagecurationproviders.api.SimplePackageCurationProvider
import java.io.File


data class DependencyAnalyzerConfig(
    val analyzerConfiguration: AnalyzerConfiguration,
    val enabledPackageManagers: Set<PackageManagerFactory>,
    val enabledCurationProviders: List<Pair<String, PackageCurationProvider>>,
    val repositoryConfiguration: RepositoryConfiguration
)

private data class RawAnalyzerResult(
    val repositoryInfo: RepositoryInfoDto,
    val environmentInfo: EnvironmentInfoDto,
    val dependencyGraphs: Map<String, DependencyGraph>
)


class DependencyAnalyzer(
    private val dependencyGraphService: DependencyGraphService = DependencyGraphService(),
    private val config: DependencyAnalyzerConfig = createDefaultConfig(),
    private val analyzer: Analyzer = Analyzer(config = config.analyzerConfiguration),
) {

    suspend fun getAnalyzerResult(projectPath: File): AnalyzerResultDto? {
        return try {
            val rawAnalyzerResult = runAnalyzer(projectPath)
            val mainProject = rawAnalyzerResult.repositoryInfo.projects.first()


            val transformedGraphs = dependencyGraphService.transformDependencyGraph(
                rawAnalyzerResult.dependencyGraphs,
            )

            val transformedGraphsWithUpdates = transformedGraphs.map { dependencyGraphService.simulateUpdates(it) }

            val result = AnalyzerResultDto(
                dependencyGraphDtos = transformedGraphsWithUpdates.map {
                    DependencyGraphsDto(
                        dependencyGraphs = it,
                        version = mainProject.version,
                        artifactId = mainProject.name,
                        groupId = mainProject.namespace
                    )
                },
                repositoryInfo = rawAnalyzerResult.repositoryInfo,
                environmentInfo = rawAnalyzerResult.environmentInfo,
            )

            result
        } catch (exception: Exception) {
            logger.error { "ORT failed with exception $exception. ${exception.message}" }
            null
        }
    }


    private fun runAnalyzer(
        projectPath: File,
    ): RawAnalyzerResult {

        val managedFiles = analyzer.findManagedFiles(
            absoluteProjectPath = projectPath,
            packageManagers = config.enabledPackageManagers,
            repositoryConfiguration = config.repositoryConfiguration
        )

        val results = analyzer.analyze(managedFiles, config.enabledCurationProviders)

        return RawAnalyzerResult(
            repositoryInfo = RepositoryInfoDto(
                url = results.repository.vcs.url,
                revision = results.repository.vcs.revision,
                projects = results.analyzer?.result?.projects?.map {
                    ProjectDto(
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

    fun close() {
        dependencyGraphService.close()
    }

    companion object {
        private fun createDefaultConfig(): DependencyAnalyzerConfig {

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
