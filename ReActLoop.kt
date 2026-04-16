package com.meshai.agent

import com.meshai.agent.safety.KillSwitch
import com.meshai.llm.LlmEngine
import com.meshai.llm.LlmMessage
import com.meshai.llm.LlmRole
import com.meshai.tools.ToolRegistry
import com.meshai.tools.ToolResult
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Singleton

/**
 * ReAct (Reasoning + Acting) agent loop.
 *
 * The loop follows the pattern:
 *   Thought → Action → Observation → Thought → ... → Final Answer
 *
 * Protocol-Version: 1 (MESHAI_PROTO_V1)
 *
 * Safety invariants enforced here:
 *   SPEC_REF: LOOP-001 — MAX_STEPS = 12 (INV-007)
 *   SPEC_REF: LOOP-002 — FINAL_ANSWER_SIGNAL is the only valid termination signal
 *   SPEC_REF: LOOP-003 — tools are only invoked via ToolRegistry.executeTool()
 *   SPEC_REF: LOOP-004 — observations stored in AgentMemory
 *   SPEC_REF: SAFETY-003 — KillSwitch checked each iteration
 */
@Singleton
class ReActLoop @Inject constructor(
    private val llmEngine: LlmEngine,
    private val toolRegistry: ToolRegistry,
    private val agentMemory: AgentMemory,
    // SPEC_REF: SAFETY-003 — KillSwitch injected to allow per-step halting
    private val killSwitch: KillSwitch
) {

    companion object {
        // SPEC_REF: INV-007 / LOOP-001 — hard ceiling, never increase without spec change
        private const val MAX_STEPS = 12
        private const val FINAL_ANSWER_SIGNAL = "FINAL ANSWER:"
    }

    private val _loopState = MutableStateFlow<LoopState>(LoopState.Idle)
    val loopState: StateFlow<LoopState> = _loopState

    /**
     * Run the ReAct loop for the given task.
     * Returns the final natural-language answer/result.
     */
    suspend fun execute(task: AgentTask, localNode: AgentNode): String {
        _loopState.value = LoopState.Running(task)
        Timber.d("[ReAct] Starting task: ${task.title}")

        val history = mutableListOf<LlmMessage>()
        val systemPrompt = buildSystemPrompt(localNode)

        history.add(LlmMessage(LlmRole.USER, buildTaskPrompt(task)))

        var step = 0
        var finalAnswer: String? = null

        // SPEC_REF: LOOP-001 / INV-007 — MAX_STEPS is enforced as the hard ceiling
        while (step < MAX_STEPS && finalAnswer == null) {
            step++
            Timber.d("[ReAct] Step $step/$MAX_STEPS")

            // SPEC_REF: SAFETY-003 — check KillSwitch at the top of every iteration
            if (killSwitch.isHalted) {
                Timber.e("[ReAct] KillSwitch active — halting loop (SPEC_REF: SAFETY-003)")
                _loopState.value = LoopState.Error(task, "Halted by KillSwitch")
                return "Execution halted by KillSwitch"
            }

            val llmResponse = llmEngine.complete(
                systemPrompt = systemPrompt,
                messages = history
            )
            Timber.d("[ReAct] LLM response: $llmResponse")

            history.add(LlmMessage(LlmRole.ASSISTANT, llmResponse))

            // SPEC_REF: LOOP-002 — only this signal terminates the loop
            if (llmResponse.contains(FINAL_ANSWER_SIGNAL)) {
                finalAnswer = llmResponse
                    .substringAfter(FINAL_ANSWER_SIGNAL)
                    .trim()
                break
            }

            val toolCall = parseToolCall(llmResponse)
            if (toolCall != null) {
                val (toolName, toolInput) = toolCall
                Timber.d("[ReAct] Calling tool: $toolName with input: $toolInput")

                // SPEC_REF: LOOP-003 / INV-005 / SAFETY-001
                // Tools are ONLY invoked via ToolRegistry, which routes through
                // ToolExecutionGuard → SafetyGate. Direct AgentTool calls are forbidden.
                val toolResult: ToolResult = try {
                    toolRegistry.executeTool(toolName, toolInput, task, localNode)
                } catch (e: Exception) {
                    Timber.e(e, "[ReAct] Tool execution failed")
                    ToolResult.failure("Tool error: ${e.message}")
                }

                val observationMsg = "Observation: ${toolResult.summary}"
                history.add(LlmMessage(LlmRole.USER, observationMsg))

                // SPEC_REF: LOOP-004 — persist observation to memory
                agentMemory.store(
                    key = "task_${task.taskId}_step_$step",
                    value = "Tool=$toolName | Result=${toolResult.summary}"
                )
            } else {
                history.add(LlmMessage(LlmRole.USER, "Continue. If you have the answer, prefix it with '$FINAL_ANSWER_SIGNAL'"))
            }
        }

        val result = finalAnswer ?: "Max steps reached. Last response: ${history.lastOrNull()?.content}"
        _loopState.value = LoopState.Completed(task, result)
        Timber.i("[ReAct] Task '${task.title}' completed: $result")
        return result
    }

    // -------------------------------------------------------------------------
    // Private helpers
    // -------------------------------------------------------------------------

    private fun buildSystemPrompt(node: AgentNode): String {
        val tools = toolRegistry.availableTools()
            .joinToString("\n") { " - ${it.name}: ${it.description}" }

        return """
            You are an autonomous AI agent running on an Android device named "${node.displayName}".
            You are part of a decentralized mesh network of AI agents.
            Your job is to complete the user's task by reasoning step by step and calling tools when needed.

            FORMAT YOUR RESPONSE:
            Thought: <your reasoning about what to do next>
            Action: <tool name>
            Action Input: <json input for the tool>

            When you have the final answer, respond with:
            FINAL ANSWER: <your answer>

            AVAILABLE TOOLS:
            $tools

            CURRENT NODE CAPABILITIES: ${node.capabilities.joinToString(", ")}
            BATTERY LEVEL: ${node.batteryLevel}%

            OWNER PRESENT: ${node.isOwnerPresent}
            NOTE: If owner is not present, avoid irreversible actions unless pre-approved.

            Always be helpful, concise, and safe. Never perform irreversible actions without confirmation.
        """.trimIndent()
    }

    private fun buildTaskPrompt(task: AgentTask): String =
        """
            TASK: ${task.title}
            DESCRIPTION: ${task.description}
            PRIORITY: ${task.priority}
            ORIGIN: ${task.origin}
            OWNER_APPROVED: ${task.ownerApproved}

            Begin your reasoning now.
        """.trimIndent()

    private fun parseToolCall(response: String): Pair<String, String>? {
        val actionLine = response.lines()
            .firstOrNull { it.trimStart().startsWith("Action:") }
            ?: return null
        val inputLine = response.lines()
            .firstOrNull { it.trimStart().startsWith("Action Input:") }
            ?: return null
        val toolName = actionLine.substringAfter("Action:").trim()
        val toolInput = inputLine.substringAfter("Action Input:").trim()
        return toolName to toolInput
    }

    // -------------------------------------------------------------------------
    // Loop state
    // -------------------------------------------------------------------------

    sealed class LoopState {
        object Idle : LoopState()
        data class Running(val task: AgentTask) : LoopState()
        data class Completed(val task: AgentTask, val result: String) : LoopState()
        data class Error(val task: AgentTask, val error: String) : LoopState()
    }
}
