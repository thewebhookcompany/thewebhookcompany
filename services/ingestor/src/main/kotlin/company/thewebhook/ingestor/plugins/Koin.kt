package company.thewebhook.ingestor.plugins

import company.thewebhook.messagestore.producer.KafkaProducerImpl
import company.thewebhook.messagestore.producer.Producer
import io.ktor.server.application.*
import org.koin.dsl.module
import org.koin.ktor.plugin.Koin
import org.koin.logger.slf4jLogger

fun Application.configureKoin() {
    val appModule = module { single<Producer<ByteArray>> { KafkaProducerImpl() } }
    install(Koin) {
        slf4jLogger()
        modules(appModule)
    }
}
