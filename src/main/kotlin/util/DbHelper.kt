package util

import artifact.db.Artifact
import artifact.db.Artifacts
import artifact.db.Version
import artifact.db.Versions
import artifact.model.ArtifactDto
import dependencies.db.DependencyGraphs
import dependencies.model.DependencyGraphDto
import kotlinx.coroutines.Dispatchers
import org.jetbrains.exposed.dao.with
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.transactions.experimental.newSuspendedTransaction
import org.jetbrains.exposed.sql.transactions.transaction

suspend fun <T> dbQuery(block: suspend () -> T): T =
    newSuspendedTransaction(Dispatchers.IO) { block() }

data class DbConfig(val url: String, val userName: String, val password: String)

fun initDatabase(dbConfig: DbConfig) {

    Database.connect(
        url = dbConfig.url,
        driver = "org.postgresql.Driver",
        user = dbConfig.userName,
        password = dbConfig.password
    )


    transaction {
        addLogger(StdOutSqlLogger)
        SchemaUtils.create(Artifacts, Versions, DependencyGraphs)
    }
}

suspend fun updateCache(dependencyGraphDto: DependencyGraphDto) {
    dependencyGraphDto.packageManagerToScopes.values.forEach {
        it.scopesToDependencies.values.forEach {
            it.forEach { artifact ->
                recursivelyUpdateCache(artifact)
            }
        }
    }
}

private suspend fun recursivelyUpdateCache(artifactDto: ArtifactDto) {
    dbQuery {
        val artifacts = Artifact.find {
            Artifacts.artifactId eq artifactDto.artifactId and (Artifacts.groupId eq artifactDto.groupId)
        }.with(Artifact::versions)

        if (artifacts.count() > 1) {
            println(
                "The cache should only contain a single entry for every groupId and " +
                        "artifact id combo. Clearing cache for ${artifactDto.groupId}/${artifactDto.artifactId}"
            )
            // TODO: we need to test if the artifacts iterable is empty after delete
            artifacts.forEach { it.delete() }
        }

        val updateNeededForArtifact: Pair<Boolean, Artifact> = if (artifacts.empty()) {
            val artifactDb = Artifact.new {
                artifactId = artifactDto.artifactId
                groupId = artifactDto.groupId
            }
            Pair(true, artifactDb)
        } else {
            val artifactDb = artifacts.first()
            if (artifactDb.versions.count().toInt() == artifactDto.versions.count()) {
                Pair(false, artifactDb)
            } else {
                Pair(true, artifactDb)
            }
        }

        if (updateNeededForArtifact.first) {
            artifactDto.versions.forEach { version ->
                if (version.releaseDate != -1L) {
                    Version.new {
                        versionNumber = version.versionNumber
                        releaseDate = version.releaseDate
                        artifact = updateNeededForArtifact.second
                    }
                } else {
                    println("Not storing version with missing release date $version")
                }
            }
        }
    }
    artifactDto.transitiveDependencies.forEach { transitiveDependency ->
        recursivelyUpdateCache(artifactDto = transitiveDependency)
    }

}
