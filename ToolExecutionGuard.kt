package com.meshai.tools

import com.meshai.agent.AgentNode
import com.meshai.agent.AgentTask
import com.meshai.agent.safety.KillSwitch
import com.meshai.agent.safety.SafetyGate
import com.meshai.agent.safety.ToolRequest
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Singleton

/**
 * ToolExecutionGuard — the mandatory bridge between ToolRegistry and SafetyGate.
 *
 * SPEC_REF: SAFETY-001 / INV-005
 * ALL tool executions must pass through this guard. ToolRegistry.executeTool()
 * calls this class; it calls SafetyGate before delegating to the actual tool.
 *
 * This structural enforcement means it is architecturally impossible to execute
 * a tool without SafetyGate evaluation — satisfying INV-001 and INV-005.
 */
@Singleton
class ToolExecutionGuard @Inject constructor(
    private val safetyGate: SafetyGate,
    private val killSwitch: KillSwitch
) {

    /**
     * Attempt to execute [tool] with [jsonInput].
     *
     * SPEC_REF: SAFETY-001
     * This method is the single execution choke-point for all tools.
     */
    suspend fun execute(
        tool: AgentTool,
        jsonInput: String,
        task: AgentTask,
        node: AgentNode
    ): ToolResult {
        // SPEC_REF: SAFETY-003 — KillSwitch is checked inside SafetyGate,
        // but we also check here so the guard itself is short-circuited cheaply.
        if (killSwitch.isHalted) {
            return ToolResult.failure("Execution halted by KillSwitch (SPEC_REF: SAFETY-003)")
        }

        val request = ToolRequest(
            toolId = tool.name,
            origin = task.origin,
            ownerPresent = node.isOwnerPresent,
            ownerApproved = task.ownerApproved,
            isIrreversible = tool.isIrreversible
        )

        // SPEC_REF: SAFETY-001 — mandatory gate evaluation
        return when (val decision = safetyGate.evaluate(request)) {
            is SafetyGate.Decision.Allow -> {
                Timber.d("[ToolExecutionGuard] Executing: ${tool.name}")
                tool.execute(jsonInput)
            }
            is SafetyGate.Decision.Deny -> {
                Timber.w("[ToolExecutionGuard] Denied: ${tool.name} — ${decision.reason}")
                ToolResult.failure("Safety gate denied: ${decision.reason}")
            }
        }
    }
}
