package company.thewebhook.ingestor.plugins

import company.thewebhook.eventstore.KafkaProducer
import company.thewebhook.eventstore.Producer
import io.ktor.server.application.*
import org.koin.dsl.module
import org.koin.ktor.plugin.Koin
import org.koin.logger.slf4jLogger

fun Application.configureKoin() {
    val appModule = module { single<Producer<ByteArray>> { KafkaProducer() } }
    install(Koin) {
        slf4jLogger()
        modules(appModule)
    }
}
