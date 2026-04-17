package com.meshai.agent

import com.meshai.llm.LlmEngine
import com.meshai.llm.LlmMessage
import com.meshai.llm.LlmRole
import com.meshai.tools.ToolRegistry
import com.meshai.tools.ToolResult
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Singleton

/**
 * ReAct (Reasoning + Acting) agent loop.
 *
 * The loop follows the pattern:
 * Thought → Action → Observation → Thought → ... → Final Answer
 *
 * At each step:
 * 1. Build context (task description, memory, available tools, previous steps)
 * 2. Ask LLM to produce a Thought and an Action (tool call or final answer)
 * 3. Execute the action via [ToolRegistry]
 * 4. Record the Observation
 * 5. Repeat until the LLM signals a final answer or max steps reached
 *
 * Thread safety: [execute] is protected by [executionMutex] — concurrent calls
 * from mesh-delegated tasks will queue rather than corrupt shared state.
 *
 * Context management: [pruneHistory] enforces a sliding window so the total
 * prompt never exceeds Gemma 2B's ~8k-token context ceiling.
 */
@Singleton
class ReActLoop @Inject constructor(
    private val llmEngine: LlmEngine,
    private val toolRegistry: ToolRegistry,
    private val agentMemory: AgentMemory
) {

    companion object {
        private const val MAX_STEPS = 12
        private const val FINAL_ANSWER_SIGNAL = "FINAL ANSWER:"

        /**
         * Approximate token budget for history before pruning.
         * Leaves headroom for the system prompt (~600 tokens) and next
         * LLM response (~1024 tokens) within an 8192-token context window.
         */
        private const val MAX_HISTORY_TOKENS = 3000

        /**
         * Number of recent history entries (assistant+user pairs) to
         * preserve verbatim when pruning. Older entries are summarised.
         */
        private const val PRESERVE_RECENT_PAIRS = 4
    }

    // -----------------------------------------------------------------------
    // Bug fix 1: execution mutex prevents concurrent tasks from
    // overwriting _loopState on a shared singleton.
    // -----------------------------------------------------------------------
    private val executionMutex = Mutex()

    private val _loopState = MutableStateFlow<LoopState>(LoopState.Idle)
    val loopState: StateFlow<LoopState> = _loopState

    /**
     * Run the ReAct loop for the given task.
     * Returns the final natural-language answer/result.
     *
     * This function is non-reentrant: a second [execute] call will suspend
     * until the first completes.
     */
    suspend fun execute(task: AgentTask, localNode: AgentNode): String {
        // Bug fix 1: wrap entire execution in mutex
        executionMutex.withLock {
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

                // Bug fix 2: prune history before each LLM call to stay
                // within the model's context window
                pruneHistory(history)

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

                // Bug fix 3: parse tool call with multiline Action Input support
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

                    val observationMsg = "Observation: ${toolResult.summary}"
                    history.add(LlmMessage(LlmRole.USER, observationMsg))

                    agentMemory.store(
                        key = "task_${task.taskId}_step_$step",
                        value = "Tool=$toolName | Result=${toolResult.summary}"
                    )
                } else {
                    history.add(
                        LlmMessage(
                            LlmRole.USER,
                            "Continue. If you have the answer, prefix it with '$FINAL_ANSWER_SIGNAL'"
                        )
                    )
                }
            }

            // Bug fix 4: never leak raw LLM reasoning as the task result.
            // Return a structured failure string so upstream code and the
            // owner notification see something meaningful.
            val result = finalAnswer ?: run {
                Timber.w("[ReAct] Max steps hit for task ${task.taskId} ('${task.title}')")
                _loopState.value = LoopState.Error(task, "Max reasoning steps exceeded")
                val lastMemory = agentMemory.recall("task_${task.taskId}_step_$step")?.value
                buildString {
                    append("Agent could not complete '${task.title}' within the reasoning limit ($MAX_STEPS steps). ")
                    if (lastMemory != null) {
                        append("Last recorded action: $lastMemory")
                    } else {
                        append("No tool actions were recorded.")
                    }
                }
            }

            if (finalAnswer != null) {
                _loopState.value = LoopState.Completed(task, result)
            }

            Timber.i("[ReAct] Task '${task.title}' completed: $result")
            return result
        }
    }

    // -----------------------------------------------------------------------
    // Bug fix 2: sliding-window history pruning
    // -----------------------------------------------------------------------

    /**
     * Prunes [history] to keep the estimated token count below [MAX_HISTORY_TOKENS].
     *
     * Strategy:
     * - Always keep the first message (original task prompt).
     * - Always keep the last [PRESERVE_RECENT_PAIRS] * 2 messages verbatim.
     * - Replace everything in between with a one-line summary.
     *
     * Token estimation: ~4 chars per token (conservative for English + JSON).
     */
    private fun pruneHistory(history: MutableList<LlmMessage>) {
        val estimatedTokens = history.sumOf { it.content.length / 4 }
        if (estimatedTokens <= MAX_HISTORY_TOKENS) return

        val keepCount = PRESERVE_RECENT_PAIRS * 2 // assistant + user pairs
        if (history.size <= keepCount + 1) return  // not enough entries to prune

        // Entries to summarise: everything except the first and last keepCount
        val toSummarise = history.subList(1, history.size - keepCount)
        val summary = buildString {
            append("Summary of previous steps: ")
            toSummarise.forEachIndexed { idx, msg ->
                val prefix = if (msg.role == LlmRole.ASSISTANT) "Agent" else "Obs"
                append("[$prefix ${idx + 1}] ${msg.content.take(120).replace('\n', ' ')}")
                if (idx < toSummarise.lastIndex) append("; ")
            }
        }

        // Remove the middle section and replace with summary
        toSummarise.clear()
        history.add(1, LlmMessage(LlmRole.USER, summary))

        Timber.d("[ReAct] History pruned — estimated tokens now ~${history.sumOf { it.content.length / 4 }}")
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

FORMAT YOUR RESPONSE EXACTLY AS:

Thought: <your reasoning about what to do next>
Action: <tool name>
Action Input: <json input for the tool — may span multiple lines>

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

    // -----------------------------------------------------------------------
    // Bug fix 3: multiline Action Input parser
    // -----------------------------------------------------------------------

    /**
     * Extract (toolName, rawJsonInput) from an LLM response.
     *
     * Handles the common Gemma output pattern where Action Input JSON
     * spans multiple lines:
     *
     *   Action: send_sms
     *   Action Input: {
     *     "to": "+1234",
     *     "message": "hello"
     *   }
     *
     * The original single-line approach returned only `{` in this case,
     * causing silent downstream JSON parse failures.
     */
    private fun parseToolCall(response: String): Pair<String, String>? {
        val lines = response.lines()

        val actionIdx = lines.indexOfFirst { it.trimStart().startsWith("Action:") }
        val inputIdx  = lines.indexOfFirst { it.trimStart().startsWith("Action Input:") }

        if (actionIdx < 0 || inputIdx < 0) return null

        val toolName = lines[actionIdx].substringAfter("Action:").trim()
        if (toolName.isBlank()) return null

        // Collect all lines from inputIdx onward, stopping at the next
        // structural keyword or end of string.
        val jsonLines = lines
            .drop(inputIdx)
            .mapIndexed { i, line ->
                if (i == 0) line.substringAfter("Action Input:").trim() else line
            }
            .takeWhile { line ->
                val trimmed = line.trimStart()
                !trimmed.startsWith("Thought:") &&
                !trimmed.startsWith("Action:") &&
                !trimmed.startsWith(FINAL_ANSWER_SIGNAL)
            }

        val rawJson = jsonLines.joinToString("\n").trim()

        return if (rawJson.isNotBlank()) toolName to rawJson else null
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
