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
import ru.dumch.spaced.sync.SyncSideEffect.*
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
    data class HandShake(val service: DiscoveredService) : SyncEvent
}

internal sealed interface SyncSideEffect : SideEffect {
    object RegisterConnected : SyncSideEffect
    object RegisterDisconnected : SyncSideEffect
    data class ServiceDiscovered(val event: DiscoveryEvent) : SyncSideEffect
    data class HandShakeResponse(val response: String): SyncSideEffect
}

internal class SyncViewModel(override val di: DI) :
    BaseViewModel<SyncState, SyncEvent, SyncSideEffect>(), DIAware {

    private val l = logging(tag = SyncViewModel::class.simpleName)

    // DNS discovery related
    private val dnsService: NetService by di.instance()
    private var scanJob: Job? = null
    private var discoveredServices = ConcurrentHashMap<String, DiscoveredService>()

    // P2P communication related
    private val syncServer: SyncDataServer by di.instance()
    private val syncClient: SyncDataClient by di.instance()

    init {
        l.info { "init" }
        init()
        syncServer.start()
        syncClient.start()
        viewModelScope.launch {
            dnsService.isRegistered.collect { isRegistered ->
                val eff = if (isRegistered) RegisterConnected else RegisterDisconnected
                send(eff)
            }
        }
    }

    override fun onCleared() {
        super.onCleared()
        syncServer.stop()
        syncClient.stop()
    }

    override fun initialState() = SyncState()

    override suspend fun handleSideEffect(effect: SyncSideEffect) {
        l.info { "handleSideEffect: $effect" }
        when (effect) {
            is RegisterConnected -> setState { copy(registered = true, registerInProgress = false) }
            is RegisterDisconnected -> setState { copy(registered = false, registerInProgress = false) }
            is ServiceDiscovered -> onServiceDiscovered(ServiceDiscovered(effect.event))
            is HandShakeResponse -> Unit
        }
    }

    override suspend fun handleEvent(event: SyncEvent) {
        l.info { "handleEvent: $event" }
        when (event) {
            is SyncEvent.TabSelected -> setState { copy(tabIdx = event.i) }
            SyncEvent.RegisterOn -> {
                if (currentState.registerInProgress) return
                dnsService.register()
                setState { copy(registerInProgress = true) }
            }

            SyncEvent.RegisterOff -> {
                if (!currentState.registered) return
                dnsService.unregister()
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
                        send(ServiceDiscovered(discoveryEvent))
                    }
                }
                setState { copy(isScanning = true) }
            }

            is SyncEvent.HandShake -> {
                val msg = syncClient.headShake(event.service)
                send(HandShakeResponse(msg))
            }
        }
    }

    private suspend fun onServiceDiscovered(effect: ServiceDiscovered) {
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