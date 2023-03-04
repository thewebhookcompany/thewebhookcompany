package company.thewebhook.ingestor

import company.thewebhook.ingestor.plugins.*
import io.ktor.server.application.*
import io.ktor.server.netty.*

fun main(args: Array<String>): Unit = EngineMain.main(args)

fun Application.module() {
    configureSerialization()
    //    configureDatabases()
    configureMonitoring()
    configureHTTP()
    configureRouting()
}
