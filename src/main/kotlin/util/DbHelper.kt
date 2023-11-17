package util

import artifact.db.Artifacts
import artifact.db.Versions
import kotlinx.coroutines.Dispatchers
import org.jetbrains.exposed.sql.Database
import org.jetbrains.exposed.sql.SchemaUtils
import org.jetbrains.exposed.sql.StdOutSqlLogger
import org.jetbrains.exposed.sql.addLogger
import org.jetbrains.exposed.sql.transactions.experimental.newSuspendedTransaction
import org.jetbrains.exposed.sql.transactions.transaction

suspend fun <T> dbQuery(block: suspend () -> T): T =
    newSuspendedTransaction(Dispatchers.IO) { block() }

fun initDatabase(url: String) {
    Database.connect(url, "org.sqlite.JDBC")

    transaction {
        addLogger(StdOutSqlLogger)
        SchemaUtils.create(Artifacts, Versions)
    }
}
