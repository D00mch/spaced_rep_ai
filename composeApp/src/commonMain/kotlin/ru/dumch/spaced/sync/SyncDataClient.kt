package ru.dumch.spaced.sync

import com.appstractive.dnssd.DiscoveredService
import com.diamondedge.logging.logging
import io.ktor.client.HttpClient
import io.ktor.client.engine.cio.CIO
import io.ktor.client.request.get
import io.ktor.client.statement.bodyAsText

class SyncDataClient() {
    private val l = logging(tag = SyncDataClient::class.simpleName)
    private var client: HttpClient? = null

    fun start() {
        client?.run { stop() }
        client = HttpClient(CIO)
    }

    fun stop() {
        client?.close()
    }

    suspend fun headShake(service: DiscoveredService): String {
        val ip = service.addresses.firstOrNull() ?: return run {
            l.e { "service.addresses is empty. Service: $service" }
            "No message, address is not supported"
        }
        val url = "http://$ip:${service.port}/hello"

        try {
            val result = client!!.get(url).bodyAsText()
            l.i { "Response: $result" }
            return result
        } catch (e: Exception) {
            l.i { "Request failed: ${e.message}" }
            return "Request failed"
        }
    }
}