package vulnerabilities.entity

import org.jetbrains.exposed.dao.UUIDEntity
import org.jetbrains.exposed.dao.UUIDEntityClass
import org.jetbrains.exposed.dao.id.EntityID
import org.jetbrains.exposed.dao.id.UUIDTable
import org.jetbrains.exposed.sql.Table
import org.jetbrains.exposed.sql.kotlin.datetime.date
import java.util.*


object Vulnerabilities : UUIDTable() {
    val vulnerabilityId = varchar("vulnerability_id", 50)
    val publishedDate = date("published_date")
    val packageName = varchar("package_name", 250)
    val introducedVersion = varchar("introduced_version", 250)
    val fixedVersion = varchar("fixed_version", 250).nullable()
}

class Vulnerability(id: EntityID<UUID>) : UUIDEntity(id) {
    companion object : UUIDEntityClass<Vulnerability>(Vulnerabilities)

    var versions by Version via VulnerabilitiesVersions

    var vulnerabilityId by Vulnerabilities.vulnerabilityId
    var publishedDate by Vulnerabilities.publishedDate
    var packageName by Vulnerabilities.packageName
    var introducedVersion by Vulnerabilities.introducedVersion
    var fixedVersion by Vulnerabilities.fixedVersion
}

object Versions : UUIDTable() {
    val versionNumber = varchar("version_number", 250)
    val publishedDate = date("published_date")
}

object VulnerabilitiesVersions : Table() {
    val version = reference("version", Versions)
    val vulnerability = reference("vulnerability", Vulnerabilities)
}

class Version(id: EntityID<UUID>) : UUIDEntity(id) {
    companion object : UUIDEntityClass<Version>(Versions)

    var versionNumber by Versions.versionNumber
    var publishedDate by Versions.publishedDate
}
