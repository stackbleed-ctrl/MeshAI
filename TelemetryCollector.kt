package com.meshai.agent

import com.meshai.data.repository.AgentRepository
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
 * TelemetryCollector — wires the control plane to live observability.
 *
 * Every task execution result flows through here. DecisionEngine reads
 * stats() to make adaptive decisions (circuit breaker, cost throttle).
 * DashboardViewModel collects events for live sparklines.
 *
 * SPEC_REF: TEL-001
 */
@Singleton
class TelemetryCollector @Inject constructor(
    private val agentRepository: AgentRepository
) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    private val _events = MutableSharedFlow<TelemetryEvent>(extraBufferCapacity = 128)
    val events: SharedFlow<TelemetryEvent> = _events.asSharedFlow()

    private val window = ArrayDeque<TelemetryEvent>(200)

    fun record(
        taskId: String,
        nodeId: String,
        latencyMs: Long,
        success: Boolean,
        transportLayer: String,
        costUsd: Double = 0.0
    ) {
        val event = TelemetryEvent(taskId, nodeId, latencyMs, costUsd, success, transportLayer)
        if (window.size >= 200) window.removeFirst()
        window.addLast(event)

        scope.launch {
            _events.emit(event)
            runCatching { agentRepository.upsertEvent(event) }
                .onFailure { Timber.e(it, "[Telemetry] Persist failed for $taskId") }
        }

        Timber.d("[Telemetry] task=$taskId success=$success lat=${latencyMs}ms transport=$transportLayer")
    }

    fun stats(): TelemetryStats {
        val events = window.toList()
        if (events.isEmpty()) return TelemetryStats()
        val successes = events.count { it.success }
        return TelemetryStats(
            totalTasks      = events.size,
            successRate     = successes.toDouble() / events.size,
            avgLatencyMs    = events.map { it.latencyMs }.average().toLong(),
            totalCostUsd    = events.sumOf { it.costUsd },
            localTaskPct    = events.count { it.transportLayer == "LOCAL" }.toDouble() / events.size,
            activeTransport = events.lastOrNull()?.transportLayer ?: "NONE"
        )
    }

    fun recentEvents(): List<TelemetryEvent> = window.toList()
}

data class TelemetryEvent(
    val taskId: String,
    val nodeId: String,
    val latencyMs: Long,
    val costUsd: Double,
    val success: Boolean,
    val transportLayer: String,
    val timestampMs: Long = System.currentTimeMillis()
)

data class TelemetryStats(
    val totalTasks: Int       = 0,
    val successRate: Double   = 1.0,
    val avgLatencyMs: Long    = 0L,
    val totalCostUsd: Double  = 0.0,
    val localTaskPct: Double  = 1.0,
    val activeTransport: String = "NONE"
)
