package commands.createDependencyGraph.model

import org.ossreviewtoolkit.model.DependencyGraph
import shared.analyzerResultDtos.EnvironmentInfoDto
import shared.analyzerResultDtos.RepositoryInfoDto

internal data class RawAnalyzerResult(
    val repositoryInfo: RepositoryInfoDto,
    val environmentInfo: EnvironmentInfoDto,
    val dependencyGraphs: Map<String, DependencyGraph>
)
