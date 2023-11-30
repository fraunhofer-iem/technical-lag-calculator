package util

import dependencies.db.AnalyzerResults
import kotlinx.coroutines.Dispatchers
import org.jetbrains.exposed.sql.*
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
        SchemaUtils.create(AnalyzerResults)
    }
}
