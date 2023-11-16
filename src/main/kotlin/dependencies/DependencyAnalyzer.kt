package dependencies

import org.apache.logging.log4j.kotlin.logger
import org.ossreviewtoolkit.analyzer.Analyzer
import org.ossreviewtoolkit.analyzer.determineEnabledPackageManagers
import org.ossreviewtoolkit.model.DependencyGraph
import org.ossreviewtoolkit.model.ResolvedPackageCurations
import org.ossreviewtoolkit.model.config.OrtConfiguration
import org.ossreviewtoolkit.model.config.RepositoryConfiguration
import org.ossreviewtoolkit.plugins.packagecurationproviders.api.PackageCurationProviderFactory
import org.ossreviewtoolkit.plugins.packagecurationproviders.api.SimplePackageCurationProvider
import java.io.File

class DependencyAnalyzer {

    fun getDependencyPackagesForProject(projectPath: File): Map<String, DependencyGraph> {
        val ortConfig = OrtConfiguration()
        val analyzerConfiguration = ortConfig.analyzer
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

        val analyzer = Analyzer(config = analyzerConfiguration)
        val managedFiles = analyzer.findManagedFiles(
            absoluteProjectPath = projectPath,
            packageManagers = enabledPackageManagers,
            repositoryConfiguration = repositoryConfiguration
        )

        val results = analyzer.analyze(managedFiles, enabledCurationProviders)

        println("analyzer results: ${results.analyzer?.result?.dependencyGraphs}")

        return results.analyzer?.result?.dependencyGraphs ?: emptyMap()
    }

}