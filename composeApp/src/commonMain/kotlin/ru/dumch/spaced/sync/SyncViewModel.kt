package ru.dumch.spaced.sync

import androidx.lifecycle.viewModelScope
import com.appstractive.dnssd.DiscoveredService
import com.appstractive.dnssd.DiscoveryEvent
import com.appstractive.dnssd.NetService
import com.appstractive.dnssd.discoverServices
import com.appstractive.dnssd.key
import com.diamondedge.logging.logging
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import org.kodein.di.DI
import org.kodein.di.DIAware
import org.kodein.di.instance
import ru.dumch.spaced.sync.SyncCommon.SERVICE_TYPE
import ru.dumch.spaced.ui.Event
import ru.dumch.spaced.ui.SideEffect
import ru.dumch.spaced.ui.State
import ru.dumch.spaced.ui.BaseViewModel
import java.util.concurrent.ConcurrentHashMap

internal data class SyncState(
    val registered: Boolean = false,
    val registerInProgress: Boolean = false,
    val tabIdx: Int = 0,
    val isScanning: Boolean = false,
    val scannedServices: List<DiscoveredService> = emptyList(),
) : State

internal sealed interface SyncEvent : Event {
    data class TabSelected(val i: Int) : SyncEvent
    object RegisterOn : SyncEvent
    object RegisterOff : SyncEvent
    object ScanOn : SyncEvent
    object ScanOff : SyncEvent
}

internal sealed interface SyncSideEffect : SideEffect {
    object RegisterConnected : SyncSideEffect
    object RegisterDisconnected : SyncSideEffect
    data class ServiceDiscovered(val event: DiscoveryEvent) : SyncSideEffect
}

internal class SyncViewModel(override val di: DI) : 
    BaseViewModel<SyncState, SyncEvent, SyncSideEffect>(), DIAware {
    
    private val l = logging(tag = SyncViewModel::class.simpleName)
    private val netService: NetService by di.instance()
    private var scanJob: Job? = null
    private var discoveredServices = ConcurrentHashMap<String, DiscoveredService>()

    override fun initialState() = SyncState()

    init {
        l.info { "init" }
        init()
        viewModelScope.launch {
            netService.isRegistered.collect { isRegistered ->
                val eff = if (isRegistered) SyncSideEffect.RegisterConnected else SyncSideEffect.RegisterDisconnected
                send(eff)
            }
        }
    }

    override suspend fun handleSideEffect(effect: SyncSideEffect) {
        l.info { "handleSideEffect: $effect" }
        when (effect) {
            is SyncSideEffect.RegisterConnected -> setState { copy(registered = true, registerInProgress = false) }
            is SyncSideEffect.RegisterDisconnected -> setState { copy(registered = false, registerInProgress = false) }
            is SyncSideEffect.ServiceDiscovered -> onServiceDiscovered(SyncSideEffect.ServiceDiscovered(effect.event))
        }
    }

    override suspend fun handleEvent(event: SyncEvent) {
        l.info { "handleEvent: $event" }
        when (event) {
            is SyncEvent.TabSelected -> setState { copy(tabIdx = event.i) }
            SyncEvent.RegisterOn -> {
                if (currentState.registerInProgress) return
                netService.register()
                setState { copy(registerInProgress = true) }
            }

            SyncEvent.RegisterOff -> {
                if (!currentState.registered) return
                netService.unregister()
                setState { copy(registerInProgress = true) }
            }

            SyncEvent.ScanOff -> {
                scanJob?.cancel()
                scanJob = null
                setState { copy(isScanning = false) }
            }

            SyncEvent.ScanOn -> {
                if (currentState.isScanning) return
                discoveredServices.clear()
                scanJob = viewModelScope.launch(Dispatchers.IO) {
                    discoverServices(SERVICE_TYPE).collect { discoveryEvent: DiscoveryEvent ->
                        send(SyncSideEffect.ServiceDiscovered(discoveryEvent))
                    }
                }
                setState { copy(isScanning = true) }
            }
        }
    }

    private suspend fun onServiceDiscovered(effect: SyncSideEffect.ServiceDiscovered) {
        val discoveryEvent: DiscoveryEvent = effect.event
        when (discoveryEvent) {
            is DiscoveryEvent.Discovered -> {
                discoveryEvent.resolve()
            }

            is DiscoveryEvent.Removed -> {
                discoveredServices.remove(discoveryEvent.service.key)
            }

            is DiscoveryEvent.Resolved -> {
                discoveredServices[discoveryEvent.service.key] = discoveryEvent.service
            }
        }
        setState { copy(scannedServices = discoveredServices.values.toList()) }
    }
}