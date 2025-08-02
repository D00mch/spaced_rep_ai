package ru.dumch.spaced.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.Job
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.launch
import kotlin.time.Duration.Companion.seconds

interface State
interface Event
interface SideEffect

abstract class BaseViewModel<STATE : State, EVENT : Event, EFFECT : SideEffect> : ViewModel() {
    private val initialState: STATE by lazy { initialState() }
    
    private val _uiState: MutableStateFlow<STATE> = MutableStateFlow(initialState)
    private val _events = MutableSharedFlow<EVENT>()
    private val _effects = MutableSharedFlow<EFFECT>()

    val uiState: StateFlow<STATE> = _uiState
    val effects: SharedFlow<EFFECT> = _effects

    protected val currentState: STATE get() = _uiState.value

    @OptIn(FlowPreview::class)
    protected fun init() {
        viewModelScope.launch { _events.debounce (1.seconds).collect { handleEvent(it) } }
        viewModelScope.launch { _effects.collect { handleSideEffect(it) } }
    }

    fun send(event: EVENT) = viewModelScope.launch { _events.emit(event) }
    protected suspend fun send(effect: EFFECT) = _effects.emit(effect)

    protected suspend fun setState(reduce: STATE.() -> STATE) {
        val newState = currentState.reduce()
        _uiState.emit(newState)
    }

    abstract fun initialState(): STATE
    abstract suspend fun handleEvent(event: EVENT)
    abstract suspend fun handleSideEffect(effect: EFFECT)

    open val ioDispatchers: CoroutineDispatcher = Dispatchers.IO

    fun ioLaunch(block: suspend CoroutineScope.() -> Unit): Job =
        viewModelScope.launch(ioDispatchers) { block() }

    fun <T> ioAsync(block: suspend CoroutineScope.() -> T): Deferred<T> =
        viewModelScope.async(ioDispatchers) { block() }
}
