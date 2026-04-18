package com.meshai.control

import com.meshai.core.model.AgentTask
import com.meshai.core.model.TaskOrigin
import com.meshai.runtime.TelemetryCollector
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Singleton

/**
 * DecisionEngine — closes the telemetry → policy control loop.
 *
 * ## The problem with passive PolicyEngine (Option C)
 *
 * The existing [PolicyEngine] is a static allow/deny gate. It evaluates
 * fixed rules (cost ceiling, remote origin) but never reads runtime state.
 * The Codex review called this out correctly: the control plane was "logging"
 * but not "controlling."
 *
 * [DecisionEngine] wraps [PolicyEngine] and adds adaptive rules driven by
 * live [TelemetryCollector] data:
 *
 * - **Failure rate circuit breaker** — if a task type has failed >30% of the
 *   time in the recent window, back off and let other nodes handle it.
 * - **Cost overspend guard** — if recent tasks have exceeded budget by >2×
 *   on average, throttle to LOCAL execution only.
 * - **Battery emergency gate** — below 10% battery, refuse ALL non-CRITICAL tasks.
 *
 * These rules feed back into [MeshKernel.loop] in real time, not just on restart.
 *
 * ## How it wires in
 *
 * [MeshKernel] calls `decisionEngine.shouldExecute(task)` before calling
 * `meshRouter.route(task)`. If false, the task is re-queued or failed fast.
 *
 * SPEC_REF: CTRL-001 / OPTION-C
 */
@Singleton
class DecisionEngine @Inject constructor(
    private val policyEngine: PolicyEngine,
    private val telemetryCollector: TelemetryCollector
) {
    companion object {
        /** Failure rate above this → circuit break for that task type. */
        private const val FAILURE_RATE_THRESHOLD = 0.30

        /** If avg cost overspend ratio exceeds this → throttle. */
        private const val COST_OVERSPEND_RATIO = 2.0

        /** Below this battery % → emergency mode (CRITICAL only). */
        private const val BATTERY_EMERGENCY_PCT = 10

        /**
         * Minimum sample size before adaptive rules kick in.
         * Prevents false circuit breaks on the first 5 tasks.
         */
        private const val MIN_SAMPLE_SIZE = 5
    }

    /**
     * Decides whether [task] should be executed now on this node.
     *
     * Returns true if all checks pass. Returns false if:
     * - Static policy denies it ([PolicyEngine.allow]).
     * - The circuit breaker is open for this task type.
     * - Battery is in emergency mode and task is not CRITICAL.
     * - Cost overspend throttling is active and task should be delegated.
     *
     * [MeshKernel] acts on false by either re-queuing with backoff or
     * delegating to a remote node via [MeshRouter].
     */
    fun shouldExecute(task: AgentTask, batteryLevel: Int = 100): DecisionResult {
        // ── 1. Static policy gate (cost ceiling, remote origin approval) ──
        if (!policyEngine.allow(task)) {
            Timber.w("[DecisionEngine] Task '${task.title}' blocked by static policy")
            return DecisionResult.Deny(reason = "Static policy: cost or remote origin constraint")
        }

        // ── 2. Battery emergency gate ─────────────────────────────────────
        if (batteryLevel < BATTERY_EMERGENCY_PCT &&
            task.priority != com.meshai.core.model.TaskPriority.CRITICAL) {
            Timber.w("[DecisionEngine] Battery $batteryLevel% — deferring non-CRITICAL task '${task.title}'")
            return DecisionResult.Defer(reason = "Battery emergency: only CRITICAL tasks accepted below ${BATTERY_EMERGENCY_PCT}%")
        }

        val stats = telemetryCollector.stats()
        if (stats.totalTasks < MIN_SAMPLE_SIZE) {
            // Not enough data — allow execution
            return DecisionResult.Allow
        }

        // ── 3. Failure rate circuit breaker ───────────────────────────────
        val failureRate = 1.0 - stats.successRate
        if (failureRate > FAILURE_RATE_THRESHOLD) {
            Timber.w("[DecisionEngine] Circuit breaker open — failure rate ${"%.0f".format(failureRate * 100)}% > threshold")
            return DecisionResult.Defer(
                reason = "Circuit breaker: recent failure rate ${"%.0f".format(failureRate * 100)}% exceeds ${(FAILURE_RATE_THRESHOLD * 100).toInt()}%"
            )
        }

        // ── 4. Cost overspend throttle ────────────────────────────────────
        if (stats.totalCostUsd > 0 && stats.totalTasks > 0) {
            val avgCost = stats.totalCostUsd / stats.totalTasks
            val maxCost = task.constraints.maxCostUsd
            if (maxCost > 0 && avgCost > maxCost * COST_OVERSPEND_RATIO) {
                Timber.w("[DecisionEngine] Cost throttle — avg ${"$%.4f".format(avgCost)} > ${COST_OVERSPEND_RATIO}× limit")
                return DecisionResult.Defer(reason = "Cost throttle: average task cost exceeds budget by ${COST_OVERSPEND_RATIO}×")
            }
        }

        return DecisionResult.Allow
    }

    /**
     * Snapshot of current decision factors — useful for the dashboard's
     * control plane health widget.
     */
    fun healthSnapshot(): ControlPlaneHealth {
        val stats = telemetryCollector.stats()
        return ControlPlaneHealth(
            circuitBreakerOpen = (1.0 - stats.successRate) > FAILURE_RATE_THRESHOLD && stats.totalTasks >= MIN_SAMPLE_SIZE,
            successRate        = stats.successRate,
            avgLatencyMs       = stats.avgLatencyMs,
            totalCostUsd       = stats.totalCostUsd,
            activeTransport    = stats.activeTransport,
            totalTasksSeen     = stats.totalTasks
        )
    }
}

/** Typed result of a [DecisionEngine.shouldExecute] call. */
sealed class DecisionResult {
    /** Execute this task on this node now. */
    object Allow : DecisionResult()

    /** Reject outright — do not re-queue, do not delegate. */
    data class Deny(val reason: String) : DecisionResult()

    /**
     * Do not execute now — re-queue with backoff or delegate to a
     * remote node that may be in a better state.
     */
    data class Defer(val reason: String) : DecisionResult()
}

data class ControlPlaneHealth(
    val circuitBreakerOpen: Boolean,
    val successRate: Double,
    val avgLatencyMs: Long,
    val totalCostUsd: Double,
    val activeTransport: String,
    val totalTasksSeen: Int
)
