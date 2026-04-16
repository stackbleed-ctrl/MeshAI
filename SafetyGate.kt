package com.meshai.agent.safety

import com.meshai.agent.AgentNode
import com.meshai.agent.AgentTask
import com.meshai.agent.TaskOrigin
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Singleton

/**
 * SafetyGate — mandatory evaluation point for all tool execution requests.
 *
 * SPEC_REF: SAFETY-001
 * Every tool execution MUST pass through SafetyGate.evaluate() before proceeding.
 * No bypass path exists. Direct calls to AgentTool.execute() are a spec violation.
 *
 * Rules enforced here:
 *  - SAFETY-001: Universal gate (structural — this class must be called)
 *  - SAFETY-002: Deny SMS for remote-origin tasks
 *  - SAFETY-003: KillSwitch halts all paths
 *  - SAFETY-004: Owner-absent escalation prevention
 */
@Singleton
class SafetyGate @Inject constructor(
    private val killSwitch: KillSwitch
) {

    /**
     * Evaluate whether a tool request is permitted.
     *
     * Returns [Decision.ALLOW] if all rules pass.
     * Returns [Decision.DENY] with a reason if any rule rejects the request.
     *
     * SPEC_REF: SAFETY-001
     */
    fun evaluate(request: ToolRequest): Decision {
        // SPEC_REF: SAFETY-003 — KillSwitch check is always first
        if (killSwitch.isHalted) {
            Timber.w("[SafetyGate] DENIED (SAFETY-003): KillSwitch is active. Tool=${request.toolId}")
            return Decision.Deny("KillSwitch is active — all execution halted (SAFETY-003)")
        }

        // SPEC_REF: SAFETY-002 — SMS must not execute for remote-origin tasks
        if (request.toolId == "send_sms" && request.origin == TaskOrigin.REMOTE) {
            val msg = "SPEC VIOLATION: SAFETY-002 — send_sms denied for REMOTE origin task"
            Timber.e("[SafetyGate] $msg")
            // Hard assert so tests catch regressions immediately
            check(false) { msg }
        }

        // SPEC_REF: SAFETY-004 — irreversible actions require owner to be present or task pre-approved
        if (request.isIrreversible && !request.ownerPresent && !request.ownerApproved) {
            val msg = "Irreversible action '${request.toolId}' denied: owner absent and not pre-approved (SAFETY-004)"
            Timber.w("[SafetyGate] DENIED: $msg")
            return Decision.Deny(msg)
        }

        Timber.d("[SafetyGate] ALLOWED: Tool=${request.toolId}, Origin=${request.origin}")
        return Decision.Allow
    }

    // -------------------------------------------------------------------------
    // Decision sealed class
    // -------------------------------------------------------------------------

    sealed class Decision {
        object Allow : Decision()
        data class Deny(val reason: String) : Decision()
    }
}

// -------------------------------------------------------------------------
// ToolRequest — carries all context SafetyGate needs to evaluate
// -------------------------------------------------------------------------

data class ToolRequest(
    val toolId: String,
    val origin: TaskOrigin,
    val ownerPresent: Boolean,
    val ownerApproved: Boolean = false,
    val isIrreversible: Boolean = false
)

// -------------------------------------------------------------------------
// KillSwitch — SPEC_REF: SAFETY-003
// -------------------------------------------------------------------------

/**
 * KillSwitch — when halted, ALL execution paths must stop immediately.
 *
 * SPEC_REF: SAFETY-003
 * Cannot be reset without explicit owner gesture (UI toggle or device restart).
 */
@Singleton
class KillSwitch @Inject constructor() {

    @Volatile
    var isHalted: Boolean = false
        private set

    /** Halt all agent execution. Cannot be reversed programmatically. */
    fun halt(reason: String) {
        isHalted = true
        Timber.e("[KillSwitch] HALTED: $reason — SPEC_REF: SAFETY-003")
    }

    /**
     * Resume from halt. Only valid when called from an explicit owner UI action.
     * SPEC_REF: SAFETY-003 — resumption is owner-gated, never automatic.
     */
    fun resumeByOwner() {
        isHalted = false
        Timber.i("[KillSwitch] Resumed by owner gesture")
    }
}
