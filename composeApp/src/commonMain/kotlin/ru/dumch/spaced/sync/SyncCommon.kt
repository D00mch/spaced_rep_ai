package ru.dumch.spaced.sync

import com.appstractive.dnssd.DiscoveryEvent

object SyncCommon {
    const val SERVICE_TYPE = "_http._tcp"
    const val PORT = 8080
}

fun DiscoveryEvent.simpleName(): String = service.host.ifBlank { service.name }