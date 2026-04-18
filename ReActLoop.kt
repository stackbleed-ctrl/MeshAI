package com.meshai.runtime

import com.meshai.ai.LlmMessage
import com.meshai.ai.LlmRole
import com.meshai.ai.ModelProvider
import com.meshai.core.model.AgentNode
import com.meshai.core.model.AgentTask
import com.meshai.storage.AgentMemory
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Singleton

/**
 * ReAct (Reasoning + Acting) loop.
 * Thought → Action → Observation → ... → FINAL ANSWER
 *
 * Each step:
 * 1. Build prompt (task, memory, tools, node context)
 * 2. Ask LLM for Thought + Action
 * 3. Execute action via ToolRegistry
 * 4. Record Observation in AgentMemory
 * 5. Repeat until FINAL ANSWER or MAX_STEPS
 */
@Singleton
class ReActLoop @Inject constructor(
    private val modelProvider: ModelProvider,
    private val toolRegistry: ToolRegistry,
    private val agentMemory: AgentMemory
) {
    companion object {
        private const val MAX_STEPS = 12
        private const val FINAL_ANSWER_SIGNAL = "FINAL ANSWER:"
    }

    private val _loopState = MutableStateFlow<LoopState>(LoopState.Idle)
    val loopState: StateFlow<LoopState> = _loopState

    suspend fun execute(task: AgentTask, localNode: AgentNode): String {
        _loopState.value = LoopState.Running(task)
        Timber.d("[ReAct] Starting: '${task.title}'")

        val history = mutableListOf<LlmMessage>()
        history.add(LlmMessage(LlmRole.USER, buildTaskPrompt(task)))

        val systemPrompt = buildSystemPrompt(localNode)
        var finalAnswer: String? = null
        var step = 0

        while (step < MAX_STEPS && finalAnswer == null) {
            step++
            Timber.d("[ReAct] Step $step/$MAX_STEPS")

            val response = modelProvider.complete(systemPrompt, history)
            history.add(LlmMessage(LlmRole.ASSISTANT, response))

            if (response.contains(FINAL_ANSWER_SIGNAL)) {
                finalAnswer = response.substringAfter(FINAL_ANSWER_SIGNAL).trim()
                break
            }

            val toolCall = parseToolCall(response)
            if (toolCall != null) {
                val (toolName, toolInput) = toolCall
                Timber.d("[ReAct] Tool: $toolName  Input: $toolInput")
                val toolResult = try {
                    toolRegistry.executeTool(toolName, toolInput)
                } catch (e: Exception) {
                    Timber.e(e, "[ReAct] Tool failed")
                    ToolResult.failure("Tool error: ${e.message}")
                }
                val observation = "Observation: ${toolResult.summary}"
                history.add(LlmMessage(LlmRole.USER, observation))
                agentMemory.store("task_${task.taskId}_step_$step", "Tool=$toolName|Result=${toolResult.summary}")
            } else {
                history.add(LlmMessage(LlmRole.USER, "Continue. Prefix final answer with '$FINAL_ANSWER_SIGNAL'"))
            }
        }

        val result = finalAnswer ?: "Max steps reached. Last: ${history.lastOrNull()?.content}"
        _loopState.value = LoopState.Completed(task, result)
        Timber.i("[ReAct] Done: '${task.title}' → $result")
        return result
    }

    private fun buildSystemPrompt(node: AgentNode): String {
        val tools = toolRegistry.availableTools().joinToString("\n") { "  - ${it.name}: ${it.description}" }
        return """
You are an autonomous AI agent on Android device "${node.displayName}" in a decentralised mesh network.
Reason step by step. Use tools when needed.

FORMAT:
Thought: <reasoning>
Action: <tool_name>
Action Input: <json>

When done:
FINAL ANSWER: <answer>

TOOLS:
$tools

NODE: capabilities=${node.capabilities.joinToString()} battery=${node.batteryLevel}% ownerPresent=${node.isOwnerPresent}
Never perform irreversible actions without owner approval.
""".trimIndent()
    }

    private fun buildTaskPrompt(task: AgentTask) =
        "TASK: ${task.title}\nDESCRIPTION: ${task.description}\nPRIORITY: ${task.priority}\nBegin."

    private fun parseToolCall(response: String): Pair<String, String>? {
        val action = response.lines().firstOrNull { it.trimStart().startsWith("Action:") } ?: return null
        val input  = response.lines().firstOrNull { it.trimStart().startsWith("Action Input:") } ?: return null
        return action.substringAfter("Action:").trim() to input.substringAfter("Action Input:").trim()
    }

    sealed class LoopState {
        object Idle : LoopState()
        data class Running(val task: AgentTask) : LoopState()
        data class Completed(val task: AgentTask, val result: String) : LoopState()
        data class Error(val task: AgentTask, val error: String) : LoopState()
    }
}
