package com.meshai.agent

import com.meshai.llm.LlmEngine
import com.meshai.llm.LlmMessage
import com.meshai.llm.LlmRole
import com.meshai.tools.ToolOutputValidator
import com.meshai.tools.ToolRegistry
import com.meshai.tools.ToolResult
import com.meshai.tools.ValidationResult
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Singleton

/**
 * ReAct (Reasoning + Acting) agent loop — production-grade execution engine.
 *
 * Runs the Think → Act → Observe → Think cycle until the LLM signals a final
 * answer, a budget ceiling is hit, or the step limit is reached. Returns both
 * a result string and a structured [ExecutionTrace] for dashboard display and
 * persistent storage alongside the completed [AgentTask].
 *
 * ## Safeguards
 *
 * | Safeguard                | Mechanism                                          |
 * |--------------------------|----------------------------------------------------|
 * | Concurrent task safety   | [executionMutex] — non-reentrant singleton         |
 * | Context window overflow  | [pruneHistory] — sliding window before each LLM call |
 * | Multiline JSON parse     | [parseToolCall] collects all lines after Action Input: |
 * | Garbage max-steps result | Clean error string from agentMemory, not raw LLM dump |
 * | Budget exhaustion        | [ExecutionBudget] — hard token ceiling per run      |
 * | Tool output garbage      | [ToolOutputValidator] — validates JSON before LLM ingestion |
 * | Silent failures          | [ExecutionTrace] — structured per-step observability record |
 */
@Singleton
class ReActLoop @Inject constructor(
    private val llmEngine: LlmEngine,
    private val toolRegistry: ToolRegistry,
    private val agentMemory: AgentMemory,
    private val toolOutputValidator: ToolOutputValidator
) {

    companion object {
        private const val MAX_STEPS = 12
        private const val FINAL_ANSWER_SIGNAL = "FINAL ANSWER:"
        private const val MAX_HISTORY_TOKENS = 3_000
        private const val PRESERVE_RECENT_PAIRS = 4
    }

    private val executionMutex = Mutex()

    private val _loopState = MutableStateFlow<LoopState>(LoopState.Idle)
    val loopState: StateFlow<LoopState> = _loopState

    // -----------------------------------------------------------------------
    // Public API
    // -----------------------------------------------------------------------

    /**
     * Run the ReAct loop for [task] on [localNode].
     *
     * @param budget  Token budget for this run. Default: [ExecutionBudget.DEFAULT_MAX_TOKENS].
     *                Pass a tighter budget for LOW-priority tasks or a larger one for CRITICAL.
     * @return        [ExecutionResult] — the answer string plus the full [ExecutionTrace].
     */
    suspend fun execute(
        task: AgentTask,
        localNode: AgentNode,
        budget: ExecutionBudget = ExecutionBudget()
    ): ExecutionResult {
        executionMutex.withLock {
            val startMs = System.currentTimeMillis()
            _loopState.value = LoopState.Running(task)
            Timber.d("[ReAct] Starting '${task.title}' (budget: ${budget.maxTokens} tokens)")

            val history = mutableListOf<LlmMessage>()
            val systemPrompt = buildSystemPrompt(localNode, budget)
            val stepTraces = mutableListOf<StepTrace>()

            history.add(LlmMessage(LlmRole.USER, buildTaskPrompt(task)))

            var step = 0
            var finalAnswer: String? = null
            var outcome = ExecutionTrace.Outcome.MAX_STEPS

            while (step < MAX_STEPS && finalAnswer == null) {
                step++
                val stepStart = System.currentTimeMillis()

                // Abort before making another LLM call if budget is already gone
                if (budget.isExhausted) {
                    Timber.w("[ReAct] Budget exhausted before step $step — stopping")
                    outcome = ExecutionTrace.Outcome.BUDGET_EXCEEDED
                    stepTraces.add(
                        StepTrace(
                            stepNumber = step,
                            type = StepTrace.StepType.BUDGET_STOP,
                            llmResponseSnippet = "",
                            tokensCost = 0,
                            elapsedMs = 0
                        )
                    )
                    break
                }

                Timber.d("[ReAct] Step $step/$MAX_STEPS — budget remaining: ${budget.remaining}")
                pruneHistory(history)

                // Build a billing proxy of the full prompt for token estimation
                val promptForBilling = systemPrompt + history.joinToString("") { it.content }

                val llmResponse = llmEngine.complete(
                    systemPrompt = systemPrompt,
                    messages = history
                )

                val stepTokens = budget.charge(promptForBilling, llmResponse)
                Timber.d("[ReAct] LLM response: ${llmResponse.take(120)}")

                history.add(LlmMessage(LlmRole.ASSISTANT, llmResponse))

                // ---- Final answer? ----
                if (llmResponse.contains(FINAL_ANSWER_SIGNAL)) {
                    finalAnswer = llmResponse.substringAfter(FINAL_ANSWER_SIGNAL).trim()
                    outcome = ExecutionTrace.Outcome.SUCCESS
                    stepTraces.add(
                        StepTrace(
                            stepNumber = step,
                            type = StepTrace.StepType.FINAL_ANSWER,
                            llmResponseSnippet = llmResponse.take(200),
                            tokensCost = stepTokens,
                            elapsedMs = System.currentTimeMillis() - stepStart
                        )
                    )
                    break
                }

                // ---- Tool call? ----
                val toolCall = parseToolCall(llmResponse)
                if (toolCall != null) {
                    val (toolName, toolInput) = toolCall
                    Timber.d("[ReAct] Tool: $toolName | input: ${toolInput.take(80)}")

                    val toolResult: ToolResult = try {
                        toolRegistry.executeTool(toolName, toolInput)
                    } catch (e: Exception) {
                        Timber.e(e, "[ReAct] Tool threw: ${e.message}")
                        ToolResult.failure("Tool error: ${e.message}")
                    }

                    // Validate tool output before the LLM sees it
                    val spec = toolRegistry.outputSpec(toolName)
                    val validation = toolOutputValidator.validate(toolName, toolResult.summary, spec)

                    val observationText = when (validation) {
                        is ValidationResult.Ok      -> "Observation: ${validation.sanitized}"
                        is ValidationResult.Invalid -> {
                            Timber.w("[ReAct] $toolName output invalid: ${validation.reason}")
                            "Observation: ${validation.sanitized}"
                        }
                    }

                    history.add(LlmMessage(LlmRole.USER, observationText))

                    val toolTokens = budget.chargeTool(toolInput, toolResult.summary)

                    agentMemory.store(
                        key = "task_${task.taskId}_step_$step",
                        value = "Tool=$toolName | Result=${toolResult.summary.take(200)}"
                    )

                    stepTraces.add(
                        StepTrace(
                            stepNumber = step,
                            type = StepTrace.StepType.TOOL_CALL,
                            llmResponseSnippet = llmResponse.take(200),
                            toolName = toolName,
                            toolInputSnippet = toolInput.take(100),
                            toolResultSnippet = toolResult.summary.take(100),
                            tokensCost = stepTokens + toolTokens,
                            elapsedMs = System.currentTimeMillis() - stepStart,
                            validationError = (validation as? ValidationResult.Invalid)?.reason
                        )
                    )
                } else {
                    // LLM produced a thought with no recognisable tool call
                    history.add(
                        LlmMessage(
                            LlmRole.USER,
                            "Continue. If you have the answer, prefix it with '$FINAL_ANSWER_SIGNAL'"
                        )
                    )
                    stepTraces.add(
                        StepTrace(
                            stepNumber = step,
                            type = StepTrace.StepType.THOUGHT_ONLY,
                            llmResponseSnippet = llmResponse.take(200),
                            tokensCost = stepTokens,
                            elapsedMs = System.currentTimeMillis() - stepStart
                        )
                    )
                }
            }

            // ---- Finalise ----
            val result = finalAnswer ?: run {
                if (outcome != ExecutionTrace.Outcome.BUDGET_EXCEEDED) {
                    outcome = ExecutionTrace.Outcome.MAX_STEPS
                }
                Timber.w("[ReAct] '${task.title}' stopped: $outcome after $step steps")
                _loopState.value = LoopState.Error(task, outcome.name)
                val lastMemory = agentMemory.recall("task_${task.taskId}_step_$step")?.value
                buildString {
                    append("Agent could not complete '${task.title}' ($outcome, $step steps). ")
                    if (lastMemory != null) append("Last action: $lastMemory")
                    else append("No tool actions recorded.")
                }
            }

            if (finalAnswer != null) {
                _loopState.value = LoopState.Completed(task, result)
            }

            val trace = ExecutionTrace(
                taskId         = task.taskId,
                taskTitle      = task.title,
                steps          = stepTraces,
                finalResult    = result,
                outcome        = outcome,
                budget         = budget.snapshot(),
                totalElapsedMs = System.currentTimeMillis() - startMs
            )

            Timber.i("[ReAct] ${trace.summary()}")
            return ExecutionResult(answer = result, trace = trace)
        }
    }

    // -----------------------------------------------------------------------
    // Sliding-window history pruning
    // -----------------------------------------------------------------------

    private fun pruneHistory(history: MutableList<LlmMessage>) {
        val estimatedTokens = history.sumOf { it.content.length / 4 }
        if (estimatedTokens <= MAX_HISTORY_TOKENS) return

        val keepCount = PRESERVE_RECENT_PAIRS * 2
        if (history.size <= keepCount + 1) return

        val toSummarise = history.subList(1, history.size - keepCount)
        val summary = buildString {
            append("Summary of previous steps: ")
            toSummarise.forEachIndexed { idx, msg ->
                val prefix = if (msg.role == LlmRole.ASSISTANT) "Agent" else "Obs"
                append("[$prefix ${idx + 1}] ${msg.content.take(120).replace('\n', ' ')}")
                if (idx < toSummarise.lastIndex) append("; ")
            }
        }
        toSummarise.clear()
        history.add(1, LlmMessage(LlmRole.USER, summary))
        Timber.d("[ReAct] History pruned — ~${history.sumOf { it.content.length / 4 }} tokens")
    }

    // -----------------------------------------------------------------------
    // Prompt builders
    // -----------------------------------------------------------------------

    private fun buildSystemPrompt(node: AgentNode, budget: ExecutionBudget): String {
        val tools = toolRegistry.availableTools()
            .joinToString("\n") { "  - ${it.name}: ${it.description}" }
        return """
You are an autonomous AI agent on Android device "${node.displayName}" in a decentralized mesh network.
Complete the user's task by reasoning step by step and calling tools when needed.

FORMAT YOUR RESPONSE EXACTLY AS:

Thought: <your reasoning>
Action: <tool name>
Action Input: <json — may span multiple lines>

When you have the final answer:
FINAL ANSWER: <your answer>

AVAILABLE TOOLS:
$tools

NODE: capabilities=${node.capabilities.joinToString()}, battery=${node.batteryLevel}%, ownerPresent=${node.isOwnerPresent}
BUDGET: ${budget.remaining} tokens remaining — be concise.
        """.trimIndent()
    }

    private fun buildTaskPrompt(task: AgentTask): String =
        "TASK: ${task.title}\nDESCRIPTION: ${task.description}\nPRIORITY: ${task.priority}\n\nBegin reasoning."

    // -----------------------------------------------------------------------
    // Multiline Action Input parser
    // -----------------------------------------------------------------------

    private fun parseToolCall(response: String): Pair<String, String>? {
        val lines = response.lines()
        val actionIdx = lines.indexOfFirst { it.trimStart().startsWith("Action:") }
        val inputIdx  = lines.indexOfFirst { it.trimStart().startsWith("Action Input:") }
        if (actionIdx < 0 || inputIdx < 0) return null

        val toolName = lines[actionIdx].substringAfter("Action:").trim()
        if (toolName.isBlank()) return null

        val jsonLines = lines.drop(inputIdx)
            .mapIndexed { i, line ->
                if (i == 0) line.substringAfter("Action Input:").trim() else line
            }
            .takeWhile { line ->
                val t = line.trimStart()
                !t.startsWith("Thought:") && !t.startsWith("Action:") && !t.startsWith(FINAL_ANSWER_SIGNAL)
            }

        val rawJson = jsonLines.joinToString("\n").trim()
        return if (rawJson.isNotBlank()) toolName to rawJson else null
    }

    // -----------------------------------------------------------------------
    // State
    // -----------------------------------------------------------------------

    sealed class LoopState {
        object Idle : LoopState()
        data class Running(val task: AgentTask) : LoopState()
        data class Completed(val task: AgentTask, val result: String) : LoopState()
        data class Error(val task: AgentTask, val error: String) : LoopState()
    }
}

/**
 * Return value of [ReActLoop.execute].
 *
 * [answer] is passed to [AgentRepository.completeTask].
 * [trace] is stored alongside the task for dashboard display and debugging.
 */
data class ExecutionResult(
    val answer: String,
    val trace: ExecutionTrace
)
