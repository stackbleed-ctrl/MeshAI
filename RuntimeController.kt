package com.meshai.service

import com.meshai.agent.AgentNode
import com.meshai.agent.MeshKernel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Singleton

/**
 * RuntimeController — lifecycle gate for the entire agent runtime.
 *
 * ## Responsibility split (Part A)
 *
 * [AgentForegroundService] is now pure Android boilerplate:
 * - Start/stop the foreground notification.
 * - Call [start] and [stop].
 * - Nothing else.
 *
 * [RuntimeController] owns the kernel:
 * - Calls [MeshKernel.loop] — the central execution loop.
 * - Exposes [state] for the notification and dashboard.
 * - [pause] / [resume] for battery-saver or screen-lock events without
 *   tearing down and re-starting the entire service.
 *
 * This means the execution loop is fully testable without an Android Context.
 *
 * SPEC_REF: LIFE-001 / PART-A
 */
@Singleton
class RuntimeController @Inject constructor(
    private val kernel: MeshKernel,
    private val scope: CoroutineScope
) {
    enum class State { STOPPED, RUNNING, PAUSED }

    private val _state = MutableStateFlow(State.STOPPED)
    val state: StateFlow<State> = _state.asStateFlow()

    val isRunning: Boolean get() = _state.value == State.RUNNING

    private var kernelJob: Job? = null

    /**
     * Start the kernel loop for [localNode].
     * Idempotent — safe to call on every START_STICKY restart.
     */
    fun start(localNode: AgentNode) {
        if (_state.value == State.RUNNING) {
            Timber.d("[RuntimeController] Already running — ignoring start()")
            return
        }
        kernelJob?.cancel()
        kernelJob = scope.launch {
            _state.value = State.RUNNING
            Timber.i("[RuntimeController] Kernel loop starting for ${localNode.displayName}")
            try {
                kernel.loop(localNode)
            } catch (e: Exception) {
                Timber.e(e, "[RuntimeController] Kernel loop exited unexpectedly")
            } finally {
                _state.value = State.STOPPED
            }
        }
    }

    fun pause() {
        if (_state.value != State.RUNNING) return
        _state.value = State.PAUSED
        Timber.i("[RuntimeController] Paused")
    }

    fun resume() {
        if (_state.value != State.PAUSED) return
        _state.value = State.RUNNING
        Timber.i("[RuntimeController] Resumed")
    }

    fun stop() {
        kernelJob?.cancel()
        kernelJob = null
        _state.value = State.STOPPED
        Timber.i("[RuntimeController] Stopped")
    }
}
