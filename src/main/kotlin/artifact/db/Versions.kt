package artifact.db

import org.jetbrains.exposed.dao.IntEntity
import org.jetbrains.exposed.dao.IntEntityClass
import org.jetbrains.exposed.dao.id.EntityID
import org.jetbrains.exposed.dao.id.IntIdTable
import org.jetbrains.exposed.sql.Column

object Versions : IntIdTable() {
    val version: Column<String> = varchar("version", 50)
    val releaseDate: Column<Long> = long("releaseDate").default(-1L)
    val artifact = reference("artifact", Artifacts)
}

class Version(id: EntityID<Int>) : IntEntity(id) {
    companion object : IntEntityClass<Version>(Versions)

    var versionNumber by Versions.version
    var releaseDate by Versions.releaseDate
    var artifact by Artifact referencedOn Versions.artifact
}