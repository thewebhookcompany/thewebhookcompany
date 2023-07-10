package company.thewebhook.datastore

import company.thewebhook.util.toBase64
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import java.util.*
import com.zaxxer.hikari.HikariConfig
import com.zaxxer.hikari.HikariDataSource
import company.thewebhook.util.ApplicationEnv
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.jetbrains.exposed.sql.Database
import org.jetbrains.exposed.sql.transactions.transaction

object DatabaseFactory {
    private val instanceId = UUID.randomUUID().toBase64()
    private val logger: Logger = LoggerFactory.getLogger(this::class.java.name + "-" + instanceId)
    private const val dbDriverClassName = "${ApplicationEnv.envPrefix}DATABASE_DRIVER_CLASS_NAME"
    private const val dbPoolSize = "${ApplicationEnv.envPrefix}DATABASE_POOL_SIZE"
    private const val dbJdbcUrl = "${ApplicationEnv.envPrefix}DATABASE_JDBC_URL"
    private const val dbUsername = "${ApplicationEnv.envPrefix}DATABASE_USERNAME"
    private const val dbPassword = "${ApplicationEnv.envPrefix}DATABASE_PASSWORD"

    fun connect() {
        logger.debug("Connecting to database")

        val config = HikariConfig().apply {
            driverClassName = ApplicationEnv.get(dbDriverClassName, "")
            jdbcUrl = ApplicationEnv.get(dbJdbcUrl, "")
            username = ApplicationEnv.get(dbUsername, "")
            password = ApplicationEnv.get(dbPassword, "")
            maximumPoolSize = ApplicationEnv.get(dbPoolSize, "10").toInt()
            isAutoCommit = false
            transactionIsolation = "TRANSACTION_REPEATABLE_READ"
            validate()
        }

        val hikari = HikariDataSource(config)
        Database.connect(hikari)

        logger.debug("Connected to database")
    }

    suspend fun <T> dbExec(
        block: () -> T
    ): T = withContext(Dispatchers.IO) {
        transaction { block() }
    }
}