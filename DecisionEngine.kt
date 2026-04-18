package com.meshai.agent

import timber.log.Timber
import javax.inject.Inject
import javax.inject.Singleton

/**
 * DecisionEngine — closes the telemetry → policy control loop.
 *
 * Previously PolicyEngine was a static allow/deny gate that never read
 * runtime state. DecisionEngine wraps it with adaptive rules:
 *
 * - Failure rate circuit breaker: >30% recent failures → Defer
 * - Battery emergency gate: <10% battery → only CRITICAL tasks proceed
 * - Cost overspend throttle: avg cost >2× budget → Defer
 *
 * MeshKernel calls shouldExecute() before routing every task.
 *
 * SPEC_REF: CTRL-001
 */
@Singleton
class DecisionEngine @Inject constructor(
    private val policyEngine: PolicyEngine,
    private val telemetryCollector: TelemetryCollector
) {
    companion object {
        private const val FAILURE_RATE_THRESHOLD = 0.30
        private const val COST_OVERSPEND_RATIO   = 2.0
        private const val BATTERY_EMERGENCY_PCT  = 10
        private const val MIN_SAMPLE_SIZE        = 5
    }

    fun shouldExecute(task: AgentTask, batteryLevel: Int = 100): DecisionResult {
        // Static policy gate
        if (!policyEngine.allow(task)) {
            Timber.w("[Decision] '${task.title}' blocked by static policy")
            return DecisionResult.Deny("Policy: cost or remote origin constraint")
        }

        // Battery emergency — only CRITICAL tasks survive
        if (batteryLevel < BATTERY_EMERGENCY_PCT && task.priority != TaskPriority.CRITICAL) {
            Timber.w("[Decision] Battery $batteryLevel% — deferring non-CRITICAL '${task.title}'")
            return DecisionResult.Defer("Battery emergency: only CRITICAL tasks accepted below $BATTERY_EMERGENCY_PCT%")
        }

        val stats = telemetryCollector.stats()
        if (stats.totalTasks < MIN_SAMPLE_SIZE) return DecisionResult.Allow

        // Failure rate circuit breaker
        val failureRate = 1.0 - stats.successRate
        if (failureRate > FAILURE_RATE_THRESHOLD) {
            Timber.w("[Decision] Circuit breaker open — failure rate ${"%.0f".format(failureRate * 100)}%")
            return DecisionResult.Defer("Circuit breaker: ${(failureRate * 100).toInt()}% failure rate")
        }

        // Cost overspend throttle
        if (stats.totalCostUsd > 0 && stats.totalTasks > 0) {
            val avgCost = stats.totalCostUsd / stats.totalTasks
            val maxCost = task.constraints.maxCostUsd
            if (maxCost > 0 && avgCost > maxCost * COST_OVERSPEND_RATIO) {
                Timber.w("[Decision] Cost throttle — avg ${"$%.4f".format(avgCost)}")
                return DecisionResult.Defer("Cost throttle: avg task cost exceeds budget by ${COST_OVERSPEND_RATIO}×")
            }
        }

        return DecisionResult.Allow
    }

    fun healthSnapshot() = ControlPlaneHealth(
        circuitBreakerOpen = (1.0 - telemetryCollector.stats().successRate) > FAILURE_RATE_THRESHOLD
            && telemetryCollector.stats().totalTasks >= MIN_SAMPLE_SIZE,
        successRate        = telemetryCollector.stats().successRate,
        avgLatencyMs       = telemetryCollector.stats().avgLatencyMs,
        activeTransport    = telemetryCollector.stats().activeTransport
    )
}

sealed class DecisionResult {
    object Allow : DecisionResult()
    data class Deny(val reason: String) : DecisionResult()
    data class Defer(val reason: String) : DecisionResult()
}

data class ControlPlaneHealth(
    val circuitBreakerOpen: Boolean,
    val successRate: Double,
    val avgLatencyMs: Long,
    val activeTransport: String
)

/** Static policy gate (cost ceiling + remote origin approval). */
@Singleton
class PolicyEngine @Inject constructor() {
    private val globalMaxCostUsd = 0.10

    fun allow(task: AgentTask): Boolean {
        if (task.constraints.maxCostUsd > globalMaxCostUsd) return false
        if (task.origin == TaskOrigin.REMOTE && !task.ownerApproved) return false
        return true
    }
}
