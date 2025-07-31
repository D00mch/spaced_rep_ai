package ru.dumch.spaced.sync

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.appstractive.dnssd.DiscoveredService
import com.appstractive.dnssd.DiscoveryEvent
import com.appstractive.dnssd.discoverServices
import com.appstractive.dnssd.key
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.util.concurrent.ConcurrentHashMap
import com.diamondedge.logging.logging
import org.kodein.di.DI
import org.kodein.di.DIAware

class SyncViewModel(override val di: DI) : ViewModel(), DIAware {
    private val l = logging(tag = SyncViewModel::class.simpleName)

    init {
        l.info { "init" }

        val services = ConcurrentHashMap<String, DiscoveredService>()
        viewModelScope.launch(Dispatchers.IO) {
            discoverServices(SyncCommon.SERVICE_TYPE).collect {
                when (it) {
                    is DiscoveryEvent.Discovered -> {
                        l.info { "on service discovered: ${it.service.addresses}" }
                        services[it.service.key] = it.service
                        // optionally resolve ip addresses of the service
                        it.resolve()
                    }
                    is DiscoveryEvent.Removed -> {
                        l.info { "on service removed: ${it.service.addresses}" }
                        services.remove(it.service.key)
                    }
                    is DiscoveryEvent.Resolved -> {
                        l.info { "on service removed: ${it.service.host}, ${it.service.addresses}" }
                        services[it.service.key] = it.service
                    }
                }
            }
        }

        viewModelScope.launch(Dispatchers.IO) {
            while(true) {
                delay(10000)
                services.forEach { (k, v) ->
                    println("Service $k: $v")
                }
            }
        }
    }
}