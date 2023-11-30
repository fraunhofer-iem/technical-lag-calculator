package dependencies.db

import dependencies.model.AnalyzerResultDto
import kotlinx.serialization.json.Json
import org.jetbrains.exposed.dao.IntEntity
import org.jetbrains.exposed.dao.IntEntityClass
import org.jetbrains.exposed.dao.id.EntityID
import org.jetbrains.exposed.dao.id.IntIdTable
import org.jetbrains.exposed.sql.json.jsonb


object AnalyzerResults : IntIdTable() {
    val result = jsonb<AnalyzerResultDto>("result", Json.Default)
}

class AnalyzerResult(id: EntityID<Int>) : IntEntity(id) {
    companion object : IntEntityClass<AnalyzerResult>(AnalyzerResults)

    var result by AnalyzerResults.result
}
