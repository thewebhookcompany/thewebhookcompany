package company.thewebhook.ingestor.plugins

import company.thewebhook.messagestore.producer.Producer
import company.thewebhook.messagestore.producer.PulsarProducerImpl
import io.ktor.server.application.*
import org.koin.dsl.module
import org.koin.ktor.plugin.Koin
import org.koin.logger.slf4jLogger

fun Application.configureKoin() {
    val appModule = module { single<Producer<ByteArray>> { PulsarProducerImpl() } }
    install(Koin) {
        slf4jLogger()
        modules(appModule)
    }
}
