package company.thewebhook.ingestor

import company.thewebhook.ingestor.models.WebhookRequestData
import company.thewebhook.ingestor.plugins.*
import company.thewebhook.messagestore.producer.Producer
import company.thewebhook.messagestore.util.MessageTooLargeException
import company.thewebhook.messagestore.util.getProducerConfigFromEnv
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.netty.*
import io.ktor.server.plugins.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import io.ktor.util.*
import kotlin.text.Regex
import kotlinx.coroutines.launch
import kotlinx.datetime.Clock
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.cbor.Cbor
import kotlinx.serialization.encodeToByteArray
import org.koin.ktor.ext.inject

fun main(args: Array<String>): Unit = EngineMain.main(args)

class ApplicationConfigException(message: String) : Exception(message)

@ExperimentalSerializationApi
fun Application.module() = launch {
    configureSerialization()
    configureMonitoring()
    configureHTTP()
    configureKoin()
    configureRouting()
    val blacklistedHeaders = System.getenv("INGESTOR_BLACKLISTED_HEADERS").lowercase().split(",")
    val blacklistedHeaderPrefixes =
        System.getenv("INGESTOR_BLACKLISTED_HEADER_PREFIXES").lowercase().split(",")

    val serverHostRegexString =
        System.getenv("INGESTOR_VALIDATION_SERVER_HOST_REGEX")
            ?: throw ApplicationConfigException(
                "INGESTOR_VALIDATION_SERVER_HOST_REGEX cannot be empty"
            )

    val producer by inject<Producer<ByteArray>>()
    producer.connect(getProducerConfigFromEnv())

    val serverHostRegex = Regex(serverHostRegexString)

    routing {
        HttpMethod.DefaultMethods.forEach {
            route("{...}", it) {
                handle {
                    val serverHost = call.request.origin.serverHost.lowercase()
                    val remoteHost = call.request.origin.remoteHost.lowercase()

                    if (!serverHostRegex.matches(serverHost)) {
                        call.application.log.error(
                            "Server host does not match the regex. Server Host: $serverHost, Remote Host: $remoteHost."
                        )
                        call.respond(HttpStatusCode.BadRequest, "HOST_INVALID")
                        return@handle
                    }

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
                        val res = producer.publish("$serverHost-incoming", webhookData)
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
