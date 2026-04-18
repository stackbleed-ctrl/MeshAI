package com.meshai.runtime

import com.meshai.core.protocol.MeshEvent
import com.meshai.storage.AgentRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.launch
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Singleton

/**
 * TelemetryCollector — wires the control plane to persistent storage.
 *
 * Every MeshEvent is:
 *  1. Broadcast on [events] SharedFlow for live dashboard updates.
 *  2. Persisted to storage via AgentRepository.
 *
 * This unlocks:
 *  - Live sparklines in the dashboard.
 *  - Post-hoc cost/latency analysis.
 *  - PolicyEngine adaptive rules (future).
 *
 * SPEC_REF: TEL-001
 */
@Singleton
class TelemetryCollector @Inject constructor(
    private val agentRepository: AgentRepository
) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    private val _events = MutableSharedFlow<MeshEvent>(extraBufferCapacity = 128)
    val events: SharedFlow<MeshEvent> = _events.asSharedFlow()

    // In-memory rolling window (last 200 events)
    private val _window = ArrayDeque<MeshEvent>(200)
    val recentEvents: List<MeshEvent> get() = _window.toList()

    fun record(event: MeshEvent) {
        if (_window.size >= 200) _window.removeFirst()
        _window.addLast(event)

        scope.launch {
            _events.emit(event)
            runCatching { agentRepository.upsertEvent(event) }
                .onFailure { Timber.e(it, "[Telemetry] Persist failed for task ${event.taskId}") }
        }
        Timber.d("[Telemetry] task=${event.taskId} success=${event.success} lat=${event.latencyMs}ms cost=\$${event.costUsd}")
    }

    /** Aggregate stats for dashboard widgets. */
    fun stats(): TelemetryStats {
        val events = recentEvents
        if (events.isEmpty()) return TelemetryStats()
        val successes = events.count { it.success }
        return TelemetryStats(
            totalTasks     = events.size,
            successRate    = successes.toDouble() / events.size,
            avgLatencyMs   = events.map { it.latencyMs }.average().toLong(),
            totalCostUsd   = events.sumOf { it.costUsd },
            localTaskPct   = events.count { it.transportLayer == "LOCAL" }.toDouble() / events.size,
            activeTransport = events.lastOrNull()?.transportLayer ?: "NONE"
        )
    }
}

data class TelemetryStats(
    val totalTasks: Int       = 0,
    val successRate: Double   = 1.0,
    val avgLatencyMs: Long    = 0L,
    val totalCostUsd: Double  = 0.0,
    val localTaskPct: Double  = 1.0,
    val activeTransport: String = "NONE"
)
