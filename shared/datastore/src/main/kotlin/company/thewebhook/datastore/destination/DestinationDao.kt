package company.thewebhook.datastore.destination

import company.thewebhook.util.models.Destination

abstract class DestinationDao {
    companion object {
        fun get() : DestinationDao {
            return DestinationDaoImpl()
        }
    }

    abstract suspend fun getByURL(url: String, level: Int): Destination?
    abstract suspend fun getById(id: String, level: Int): Destination?
    abstract suspend fun getAll(): Map<String, Destination>
}