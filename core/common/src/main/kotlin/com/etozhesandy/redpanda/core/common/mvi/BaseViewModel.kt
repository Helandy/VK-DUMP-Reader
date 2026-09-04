package com.etozhesandy.redpanda.core.common.mvi

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/** Base MVI ViewModel: a single [state] flow, a one-off [effect] flow, and [onEvent] as the entrypoint. */
abstract class BaseViewModel<S : UiState, E : UiEvent, F : UiEffect> : ViewModel() {

    /** Built lazily, so the initial state can be assembled from the subclass' injected dependencies. */
    protected abstract fun createInitialState(): S

    private val _state by lazy { MutableStateFlow(createInitialState()) }
    val state: StateFlow<S> by lazy { _state.asStateFlow() }

    private val _effect = MutableSharedFlow<F>()
    val effect: SharedFlow<F> = _effect.asSharedFlow()

    protected val currentState: S get() = _state.value

    private val exceptionHandler = CoroutineExceptionHandler { _, throwable ->
        onError(throwable)
    }

    abstract fun onEvent(event: E)

    protected fun setState(reducer: S.() -> S) {
        _state.update(reducer)
    }

    protected fun setEffect(builder: () -> F) {
        viewModelScope.launch { _effect.emit(builder()) }
    }

    protected open fun onError(throwable: Throwable) {
        // Overridden by ViewModels that want to surface the error in their state.
    }

    protected fun launchSafe(block: suspend CoroutineScope.() -> Unit) {
        viewModelScope.launch(exceptionHandler, block = block)
    }
}
