package ru.dumch.spaced.sync

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.appstractive.dnssd.DiscoveredService
import com.appstractive.dnssd.DiscoveryEvent
import com.appstractive.dnssd.NetService
import com.appstractive.dnssd.discoverServices
import com.appstractive.dnssd.key
import com.diamondedge.logging.logging
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import org.kodein.di.DI
import org.kodein.di.DIAware
import org.kodein.di.instance
import ru.dumch.spaced.sync.SyncCommon.SERVICE_TYPE
import ru.dumch.spaced.ui.ViewModelWithUtils
import java.util.concurrent.ConcurrentHashMap
import kotlin.collections.set

data class SyncState(
    val registered: Boolean = false,
    val registerInProgress: Boolean = false,
    val tabIdx: Int = 0,

    val isScanning: Boolean = false,
    val scannedServices: List<DiscoveredService> = emptyList(),
)

internal sealed interface SyncEvent {
    data class TabSelected(val i: Int) : SyncEvent
    object RegisterOn : SyncEvent
    object RegisterOff : SyncEvent
    object ScanOn : SyncEvent
    object ScanOff : SyncEvent
}

internal sealed interface SyncSideEffect {
    object RegisterConnected : SyncSideEffect
    object RegisterDisconnected : SyncSideEffect
    data class ServiceDiscovered(val event: DiscoveryEvent) : SyncSideEffect
}

internal class SyncViewModel(override val di: DI) : ViewModel(), DIAware {
    private val l = logging(tag = SyncViewModel::class.simpleName)
    private val netService: NetService by di.instance()
    private val _uiState: MutableStateFlow<SyncState> = MutableStateFlow(SyncState())
    private val _events = MutableSharedFlow<SyncEvent>()
    private val _effects = MutableSharedFlow<SyncSideEffect>()

    private val currentState: SyncState get() = uiState.value
    val uiState: StateFlow<SyncState> = _uiState
    val effects: SharedFlow<SyncSideEffect> = _effects

    private var scanJob: Job? = null
    private var discoveredServices = ConcurrentHashMap<String, DiscoveredService>()

    init {
        l.info { "init" }
        viewModelScope.launch { _events.collect(::handleEvent) }
        viewModelScope.launch { _effects.collect(::handleSideEffect) }
        viewModelScope.launch {
            netService.isRegistered.collect { isRegistered ->
                val eff = if (isRegistered) SyncSideEffect.RegisterConnected else SyncSideEffect.RegisterDisconnected
                _effects.emit(eff)
            }
        }
    }

    fun send(event: SyncEvent) = viewModelScope.launch { _events.emit(event) }

    suspend fun setState(reduce: SyncState.() -> SyncState) {
        val newState = currentState.reduce()
        _uiState.emit(newState)
    }

    private suspend fun handleSideEffect(effect: SyncSideEffect) {
        l.info { "handleSideEffect: $effect" }
        when (effect) {
            is SyncSideEffect.RegisterConnected -> setState { copy(registered = true, registerInProgress = false) }
            SyncSideEffect.RegisterDisconnected -> setState { copy(registered = false, registerInProgress = false) }
            is SyncSideEffect.ServiceDiscovered -> onServiceDiscovered(effect)
        }
    }

    private suspend fun onServiceDiscovered(effect: SyncSideEffect.ServiceDiscovered) {
        val discoveryEvent = effect.event
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

    private suspend fun handleEvent(event: SyncEvent) {
        l.info { "handleEvent: $event" }
        when (event) {
            is SyncEvent.TabSelected -> setState { copy(tabIdx = event.i) }
            SyncEvent.RegisterOn -> {
                netService.register()
                setState { copy(registerInProgress = true) }
            }

            SyncEvent.RegisterOff -> {
                netService.unregister()
                setState { copy(registerInProgress = true) }
            }

            SyncEvent.ScanOff -> {
                scanJob?.cancel()
                scanJob = null
                setState { copy(isScanning = false) }
            }

            SyncEvent.ScanOn -> {
                discoveredServices.clear()
                scanJob = viewModelScope.launch(Dispatchers.IO) {
                    discoverServices(SERVICE_TYPE).collect { discoveryEvent: DiscoveryEvent ->
                        _effects.emit(SyncSideEffect.ServiceDiscovered(discoveryEvent))
                    }
                }
                setState { copy(isScanning = true) }
            }
        }
    }
}