package artifact.db

import org.jetbrains.exposed.dao.IntEntity
import org.jetbrains.exposed.dao.IntEntityClass
import org.jetbrains.exposed.dao.id.EntityID
import org.jetbrains.exposed.dao.id.IntIdTable
import org.jetbrains.exposed.sql.Column

object Artifacts : IntIdTable() {
    val artifactId: Column<String> = varchar("artifactId", 100)
    val groupId: Column<String> = varchar("groupId", 100)
}

class Artifact(id: EntityID<Int>) : IntEntity(id) {
    companion object : IntEntityClass<Artifact>(Artifacts)

    var artifactId by Artifacts.artifactId
    var groupId by Artifacts.groupId
    val versions by Version referrersOn Versions.artifact
}
