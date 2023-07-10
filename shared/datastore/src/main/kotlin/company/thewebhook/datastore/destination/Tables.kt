package company.thewebhook.datastore.destination

import org.jetbrains.exposed.sql.Column
import org.jetbrains.exposed.sql.Table

object DestinationTable: Table() {
    val id = varchar("id", 64).uniqueIndex()
    val url = varchar("url", 2048).uniqueIndex()
    val delayBeforeSendingMillis = long("delayBeforeSendingMillis")
    val httpVersion = varchar("url", 10)
    val timeoutMillis = long("timeoutMillis")

    override val primaryKey = PrimaryKey(id)
}

object TransformTable: Table() {
    val id = varchar("id", 64).uniqueIndex()
    val script = varchar("script", Int.MAX_VALUE)

    override val primaryKey = PrimaryKey(id)
}

object DestinationGroupTable: Table() {
    val id = varchar("id", 64).uniqueIndex()
    val parentDestination = reference("parentDestination", DestinationTable.id)
    val level = integer("level")

    override val primaryKey = PrimaryKey(DestinationTable.id)
}

object DestinationMappingTable: Table() {
    val id = varchar("id", 64).uniqueIndex()
    val destinationGroup = reference("destinationGroup", DestinationGroupTable.id)
    val destination = reference("destination", DestinationTable.id)
    val level = integer("level")
    val weight: Column<Int> = integer("weight")
    val bodyTransform = reference("bodyTransform", TransformTable.id)
    val filterTransform = reference("filterTransform", TransformTable.id)

    override val primaryKey = PrimaryKey(id)
}