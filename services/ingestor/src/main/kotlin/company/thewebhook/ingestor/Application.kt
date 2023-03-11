package company.thewebhook.ingestor

import company.thewebhook.eventstore.MessageTooLargeException
import company.thewebhook.eventstore.Producer
import company.thewebhook.ingestor.models.WebhookRequestData
import company.thewebhook.ingestor.plugins.*
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.netty.*
import io.ktor.server.plugins.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import io.ktor.util.*
import kotlinx.coroutines.launch
import kotlinx.datetime.Clock
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.cbor.Cbor
import kotlinx.serialization.encodeToByteArray
import org.koin.ktor.ext.inject

fun main(args: Array<String>): Unit = EngineMain.main(args)

@ExperimentalSerializationApi
fun Application.module() = launch {
    val eventStoreConfig =
        environment.config
            .config("event-store.producer")
            .toMap()
            .filterValues { it is String }
            .mapValues { it.value as String }
    configureSerialization()
    configureMonitoring()
    configureHTTP()
    configureKoin()
    configureRouting()
    val blacklistedHeaders =
        environment.config
            .property("ingestor.blacklisted.headers")
            .getString()
            .lowercase()
            .split(",")
    val blacklistedHeaderPrefixes =
        environment.config
            .property("ingestor.blacklisted.headerPrefixes")
            .getString()
            .lowercase()
            .split(",")

    val producer by inject<Producer<ByteArray>>()
    producer.connect(eventStoreConfig)

    routing {
        HttpMethod.DefaultMethods.forEach {
            route("{...}", it) {
                handle {
                    val serverHost = call.request.origin.serverHost
                    val remoteHost = call.request.origin.remoteHost

                    val webhookData =
                        Cbor.encodeToByteArray(
                            WebhookRequestData(
                                call.request.httpMethod.value,
                                call.request.uri,
                                call.request.headers.toMap().filterKeys { key ->
                                    val keyLower = key.lowercase()
                                    !blacklistedHeaderPrefixes.any { prefix ->
                                        keyLower.startsWith(prefix)
                                    } && !blacklistedHeaders.contains(key.lowercase())
                                },
                                call.receiveText(),
                                Clock.System.now().toEpochMilliseconds(),
                                remoteHost,
                                serverHost,
                            )
                        )
                    try {
                        val res = producer.publish(serverHost, webhookData)
                        call.respond(
                            if (res) {
                                HttpStatusCode.OK
                            } else {
                                call.application.log.error("Publishing webhook data returned false")
                                HttpStatusCode.InternalServerError
                            }
                        )
                    } catch (e: Exception) {
                        call.application.log.error("Exception thrown on publishing webhook data", e)
                        when (e) {
                            is MessageTooLargeException -> {
                                call.respond(HttpStatusCode.PayloadTooLarge)
                            }
                            else -> {
                                call.respond(HttpStatusCode.InternalServerError)
                            }
                        }
                    }
                }
            }
        }
    }
}
