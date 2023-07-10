package company.thewebhook.datastore.destination

import company.thewebhook.datastore.DatabaseFactory.dbExec
import company.thewebhook.util.models.Destination
import company.thewebhook.util.models.HttpVersion
import company.thewebhook.util.models.Transform
import org.jetbrains.exposed.sql.ResultRow
import org.jetbrains.exposed.sql.and
import org.jetbrains.exposed.sql.select
import org.jetbrains.exposed.sql.selectAll

class DestinationDaoImpl: DestinationDao() {
    override suspend fun getByURL(url: String, level: Int): Destination? {
        val destinationResult = dbExec {
            (DestinationMappingTable innerJoin DestinationTable)
                .select {
                    (DestinationTable.url eq url) and (DestinationMappingTable.level eq level)
                }
                .toList()
                .singleOrNull()
        }

        if (destinationResult != null) {
            val onFailureDestinations = getDestinationGroups(destinationResult[DestinationTable.id], level)
                .map {
                    getDestinationsInGroup(it[DestinationGroupTable.id])
                }

            return toDestination(destinationResult, onFailureDestinations)
        }

        return null
    }

    override suspend fun getById(id: String, level: Int): Destination? {
        val destinationResult = dbExec {
            (DestinationMappingTable innerJoin DestinationTable)
                .select {
                    (DestinationTable.id eq id) and (DestinationMappingTable.level eq level)
                }
                .toList()
                .singleOrNull()
        }

        if (destinationResult != null) {
            val onFailureDestinations = getDestinationGroups(destinationResult[DestinationTable.id], level)
                .map {
                    getDestinationsInGroup(it[DestinationGroupTable.id])
                }

            return toDestination(destinationResult, onFailureDestinations)
        }

        return null
    }

    override suspend fun getAll(): Map<String, Destination> {
        val destinationResult = dbExec {
            (DestinationMappingTable innerJoin DestinationTable)
                .selectAll()
                .toList()
        }

        return destinationResult.associate {
            it[DestinationMappingTable.id] to toDestination(
                it,
                getOnFailureDestinations(it[DestinationTable.id], it[DestinationMappingTable.level])
            )
        }
    }

    private suspend fun getOnFailureDestinations(id: String, level: Int): List<List<String>> {
        return getDestinationGroups(id, level)
            .map {
                getDestinationsInGroup(it[DestinationGroupTable.id])
            }
    }

    private suspend fun getDestinationsInGroup(groupId: String): List<String> {
        val destinations = dbExec {
            DestinationMappingTable
                .select {
                    DestinationMappingTable.destinationGroup eq groupId
                }
                .toList()
        }

        return destinations.map {
            it[DestinationMappingTable.id]
        }
    }

    private suspend fun getDestinationGroups(parentDestinationId: String, level: Int): List<ResultRow> {
        val destinationGroups = dbExec {
            DestinationGroupTable
                .select {
                    (DestinationGroupTable.parentDestination eq parentDestinationId) and (DestinationGroupTable.level eq level)
                }
                .toList()
        }

        return destinationGroups
    }

    private suspend fun toDestination(row: ResultRow, onFailureDestinations: List<List<String>>): Destination = Destination(
        id = row[DestinationTable.id],
        url = row[DestinationTable.url],
        delayBeforeSendingMillis = row[DestinationTable.delayBeforeSendingMillis],
        bodyTransform = getTransform(row[DestinationMappingTable.bodyTransform]),
        filterTransform = getTransform(row[DestinationMappingTable.filterTransform]),
        weight = row[DestinationMappingTable.weight],
        httpVersion = HttpVersion.valueOf(row[DestinationTable.httpVersion]),
        timeoutMillis = row[DestinationTable.timeoutMillis],
        onFailureDestinations = onFailureDestinations
    )

    private suspend fun getTransform(id: String): Transform? {
        val tranformResult = dbExec {
            TransformTable
                .select {
                    TransformTable.id eq id
                }
                .toList()
                .singleOrNull()
        }

        if(tranformResult !== null)
            return Transform(
                id = tranformResult[TransformTable.id],
                script = tranformResult[TransformTable.script]
            )

        return null
    }
}