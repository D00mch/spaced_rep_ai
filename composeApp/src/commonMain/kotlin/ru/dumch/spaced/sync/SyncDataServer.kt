package ru.dumch.spaced.sync

import com.diamondedge.logging.logging
import io.ktor.server.cio.CIO
import io.ktor.server.cio.CIOApplicationEngine
import io.ktor.server.engine.EmbeddedServer
import io.ktor.server.engine.embeddedServer
import io.ktor.server.response.respondText
import io.ktor.server.routing.get
import io.ktor.server.routing.routing

class SyncDataServer(
    private val port: Int = SyncCommon.PORT,
) {
    private val l = logging(tag = SyncDataServer::class.simpleName)

    private val server: EmbeddedServer<CIOApplicationEngine, CIOApplicationEngine.Configuration> by lazy {
        embeddedServer(CIO, port = port) {
            routing {
                get("/hello") {
                    call.respondText("Hello!")
                }
            }
        }
    }

    fun start() {
        l.info { "About to start service" }
        server.start(wait = false)
    }

    fun stop() {
        l.info { "About to stop service" }
        server.stop()
    }
}