package com.meshai.agent

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
 * At each step:
 * 1. Build context (task description, memory, available tools, previous steps)
 * 2. Ask LLM to produce a Thought and an Action (tool call or final answer)
 * 3. Execute the action via [ToolRegistry]
 * 4. Record the Observation
 * 5. Repeat until the LLM signals a final answer or max steps reached
 */
@Singleton
class ReActLoop @Inject constructor(
    private val llmEngine: LlmEngine,
    private val toolRegistry: ToolRegistry,
    private val agentMemory: AgentMemory
) {

    companion object {
        private const val MAX_STEPS = 12
        private val FINAL_ANSWER_SIGNAL = "FINAL ANSWER:"
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

        // Initial user message: the task
        history.add(LlmMessage(LlmRole.USER, buildTaskPrompt(task)))

        var step = 0
        var finalAnswer: String? = null

        while (step < MAX_STEPS && finalAnswer == null) {
            step++
            Timber.d("[ReAct] Step $step/$MAX_STEPS")

            // Ask LLM for next thought/action
            val llmResponse = llmEngine.complete(
                systemPrompt = systemPrompt,
                messages = history
            )
            Timber.d("[ReAct] LLM response: $llmResponse")

            // Record assistant turn
            history.add(LlmMessage(LlmRole.ASSISTANT, llmResponse))

            // Check for final answer signal
            if (llmResponse.contains(FINAL_ANSWER_SIGNAL)) {
                finalAnswer = llmResponse
                    .substringAfter(FINAL_ANSWER_SIGNAL)
                    .trim()
                break
            }

            // Parse tool call from response
            val toolCall = parseToolCall(llmResponse)
            if (toolCall != null) {
                val (toolName, toolInput) = toolCall
                Timber.d("[ReAct] Calling tool: $toolName with input: $toolInput")

                val toolResult: ToolResult = try {
                    toolRegistry.executeTool(toolName, toolInput)
                } catch (e: Exception) {
                    Timber.e(e, "[ReAct] Tool execution failed")
                    ToolResult.failure("Tool error: ${e.message}")
                }

                // Store observation in history
                val observationMsg = "Observation: ${toolResult.summary}"
                history.add(LlmMessage(LlmRole.USER, observationMsg))

                // Also save to agent memory for cross-session recall
                agentMemory.store(
                    key = "task_${task.taskId}_step_$step",
                    value = "Tool=$toolName | Result=${toolResult.summary}"
                )
            } else {
                // LLM produced a thought with no tool call — add a prompt to continue
                history.add(LlmMessage(LlmRole.USER, "Continue. If you have the answer, prefix it with '$FINAL_ANSWER_SIGNAL'"))
            }
        }

        val result = finalAnswer ?: "Max steps reached. Last response: ${history.lastOrNull()?.content}"
        _loopState.value = LoopState.Completed(task, result)
        Timber.i("[ReAct] Task '${task.title}' completed: $result")
        return result
    }

    // -----------------------------------------------------------------------
    // Private helpers
    // -----------------------------------------------------------------------

    private fun buildSystemPrompt(node: AgentNode): String {
        val tools = toolRegistry.availableTools()
            .joinToString("\n") { "  - ${it.name}: ${it.description}" }

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
            
            Always be helpful, concise, and safe. Never perform irreversible actions without confirmation.
        """.trimIndent()
    }

    private fun buildTaskPrompt(task: AgentTask): String =
        """
            TASK: ${task.title}
            DESCRIPTION: ${task.description}
            PRIORITY: ${task.priority}
            
            Begin your reasoning now.
        """.trimIndent()

    /**
     * Extract (toolName, toolInput) from LLM response.
     * Expects format:
     *   Action: tool_name
     *   Action Input: {...}
     */
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

    // -----------------------------------------------------------------------
    // Loop state sealed class
    // -----------------------------------------------------------------------

    sealed class LoopState {
        object Idle : LoopState()
        data class Running(val task: AgentTask) : LoopState()
        data class Completed(val task: AgentTask, val result: String) : LoopState()
        data class Error(val task: AgentTask, val error: String) : LoopState()
    }
}
