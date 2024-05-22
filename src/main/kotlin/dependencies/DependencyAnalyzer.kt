package dependencies

import artifact.ArtifactService
import artifact.model.*
import dependencies.model.*
import http.deps.DepsClient
import kotlinx.coroutines.*
import org.apache.logging.log4j.kotlin.logger
import org.ossreviewtoolkit.analyzer.Analyzer
import org.ossreviewtoolkit.analyzer.PackageManagerFactory
import org.ossreviewtoolkit.analyzer.determineEnabledPackageManagers
import org.ossreviewtoolkit.model.DependencyGraph
import org.ossreviewtoolkit.model.PackageReference
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


class DependencyAnalyzer @OptIn(ExperimentalCoroutinesApi::class) constructor(
    private val artifactService: ArtifactService = ArtifactService(),
    private val config: DependencyAnalyzerConfig = createDefaultConfig(),
    private val analyzer: Analyzer = Analyzer(config = config.analyzerConfiguration),
    private val depsClient: DepsClient = DepsClient(),
    // It is important to limit the parallelization of the IO scope, which is used to make server
    // requests, or else the server at some point will tell us to go away.
    private val ioScope: CoroutineScope = CoroutineScope(Dispatchers.IO.limitedParallelism(10))
) {

    private val results: MutableList<AnalyzerResultDto> = mutableListOf()

    suspend fun getAnalyzerResult(projectPath: File): AnalyzerResultDto? {
        return try {
            val rawAnalyzerResult = runAnalyzer(projectPath)
            val mainProject = rawAnalyzerResult.repositoryInfo.projects.first()


            val transformedGraphs = transformDependencyGraph(
                rawAnalyzerResult.dependencyGraphs,
            )

            val result = AnalyzerResultDto(
                dependencyGraphDtos = transformedGraphs.map {
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
            results.add(result)
            result
        } catch (exception: Exception) {
            logger.error { "ORT failed with exception $exception. ${exception.message}" }
            null
        }
    }

    fun close() {
        artifactService.close()
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

    private suspend fun getArtifactsWithVersions(ecosystem: String, artifacts: List<Artifact>): List<Artifact> {
        val artifactDeferred: List<Deferred<Artifact>> = artifacts.map { artifact ->
            ioScope.async {
                val versions = depsClient.getVersionsForPackage(
                    ecosystem = ecosystem,
                    namespace = artifact.groupId,
                    name = artifact.artifactId
                )
                Artifact(
                    artifactId = artifact.artifactId,
                    groupId = artifact.groupId,
                    versions = versions
                )
            }
        }

        return artifactDeferred.awaitAll()
    }

    private suspend fun transformDependencyGraph(
        dependencyGraphs: Map<String, DependencyGraph>
    ): List<DependencyGraphs> {


        return dependencyGraphs.map { (packageManager, graph) ->

            val seen: MutableMap<String, Int> = mutableMapOf() // maps identifier to artifact idx
            val uniqueArtifacts: MutableList<Artifact> = mutableListOf()
            graph.packages.forEach { current ->
                val ident = current.namespace + current.name
                if (!seen.contains(ident)) {
                    seen[ident] = uniqueArtifacts.count()
                    uniqueArtifacts.add(
                        Artifact(
                            artifactId = current.name,
                            groupId = current.namespace,
                        )
                    )
                }
            }

            val artifactsWithVersions: List<Artifact> = getArtifactsWithVersions(
                artifacts = uniqueArtifacts,
                ecosystem = packageManager
            )
            uniqueArtifacts.clear()


            val graphs = graph.createScopes().associate { scope ->
                val directDependencyIndices = mutableListOf<Int>()
                val nodes = mutableListOf<ArtifactNode>()
                val edges = mutableListOf<ArtifactNodeEdge>()

                val seenPkgs: MutableSet<PackageReference> = mutableSetOf()
                scope.dependencies.forEach { packageRef ->

                    fun packageRefToArtifact(namespace: String, name: String) {
                        val ident = namespace + name

                        val idx = seen[ident] ?: -1
                        val artifact = artifactsWithVersions[idx]
                        val usedVersionIdx =
                            artifact.versions.indexOfFirst { it.versionNumber == packageRef.id.version }

                        nodes.add(
                            ArtifactNode(
                                artifactIdx = idx,
                                usedVersionIdx = usedVersionIdx
                            )
                        )

                        packageRef.dependencies.forEach { dependency ->
                            val depIdent = dependency.id.namespace + dependency.id.name
                            val depIdx = seen[depIdent] ?: -1
                            edges.add(
                                ArtifactNodeEdge(
                                    from = idx,
                                    to = depIdx
                                )
                            )

                            if (!seenPkgs.contains(dependency)) { // TODO: test if this is actually correct and generates a complete tree
                                seenPkgs.add(dependency)
                                packageRefToArtifact(
                                    namespace = dependency.id.namespace,
                                    name = dependency.id.name
                                )
                            }
                        }
                    }
                    seenPkgs.add(packageRef)
                    packageRefToArtifact(name = packageRef.id.name, namespace = packageRef.id.namespace)
                    directDependencyIndices.addLast(seen[packageRef.id.namespace + packageRef.id.name])

                }
                scope.name to DependencyGraph(
                    nodes = nodes,
                    edges = edges,
                    directDependencyIndices = directDependencyIndices
                )
            }

            DependencyGraphs(
                ecosystem = packageManager,
                artifacts = artifactsWithVersions,
                graph = graphs
            )
        }
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
