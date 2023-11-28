package dependencies.db

import dependencies.model.DependencyGraphDto
import kotlinx.serialization.json.Json
import org.jetbrains.exposed.dao.IntEntity
import org.jetbrains.exposed.dao.IntEntityClass
import org.jetbrains.exposed.dao.id.EntityID
import org.jetbrains.exposed.dao.id.IntIdTable
import org.jetbrains.exposed.sql.json.jsonb


object DependencyGraphs : IntIdTable() {
    val graph = jsonb<DependencyGraphDto>("graphs", Json.Default)
}

class DependencyGraph(id: EntityID<Int>) : IntEntity(id) {
    companion object : IntEntityClass<DependencyGraph>(DependencyGraphs)

    var graph by DependencyGraphs.graph
}
