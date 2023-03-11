package company.thewebhook.ingestor.plugins

import io.ktor.server.application.*
import io.ktor.server.plugins.compression.*
import io.ktor.server.plugins.forwardedheaders.*

fun Application.configureHTTP() {
    install(Compression) {
        gzip { priority = 1.0 }
        deflate {
            priority = 10.0
            minimumSize(1024) // condition
        }
    }
    /** Use this only if behind a trusted proxy */
    install(XForwardedHeaders) { useFirstProxy() }
}
