package com.meshai.runtime

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Singleton

/**
 * RuntimeController — single gate for lifecycle management.
 *
 * AgentForegroundService calls start/stop/pause here.
 * It NEVER manipulates routing or cognition directly.
 * This separation allows testing lifecycle without an Android Context.
 *
 * SPEC_REF: LIFE-001
 */
@Singleton
class RuntimeController @Inject constructor(
    private val telemetryCollector: TelemetryCollector
) {
    enum class State { STOPPED, RUNNING, PAUSED }

    private val _state = MutableStateFlow(State.STOPPED)
    val state: StateFlow<State> = _state.asStateFlow()

    val isRunning: Boolean get() = _state.value == State.RUNNING

    fun start() {
        if (_state.value == State.RUNNING) return
        _state.value = State.RUNNING
        Timber.i("[RuntimeController] Started")
    }

    fun pause() {
        if (_state.value != State.RUNNING) return
        _state.value = State.PAUSED
        Timber.i("[RuntimeController] Paused")
    }

    fun stop() {
        _state.value = State.STOPPED
        Timber.i("[RuntimeController] Stopped")
    }

    fun resume() {
        if (_state.value != State.PAUSED) return
        _state.value = State.RUNNING
        Timber.i("[RuntimeController] Resumed")
    }
}
