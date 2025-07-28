package ru.dumch.spaced.sync

import java.net.InetAddress
import javax.jmdns.JmDNS
import javax.jmdns.ServiceInfo

class MdnsPublisher : MdnsController {
    private var jmdns: JmDNS? = null

    override fun start(serviceName: String, port: Int) {
        stop()
        val addr = InetAddress.getLocalHost()
        jmdns = JmDNS.create(addr)
        val serviceInfo = ServiceInfo.create("_http._tcp.local.", serviceName, port, "p2p http service")
        jmdns?.registerService(serviceInfo)
    }

    override fun stop() {
        jmdns?.unregisterAllServices()
        jmdns?.close()
    }
}