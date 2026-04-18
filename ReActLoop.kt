package com.meshai.runtime

import com.meshai.ai.LlmMessage
import com.meshai.ai.LlmRole
import com.meshai.ai.ModelProvider
import com.meshai.core.model.AgentNode
import com.meshai.core.model.AgentTask
import com.meshai.core.model.TaskPriority
import com.meshai.storage.AgentMemory
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Singleton

/**
 * ReAct (Reasoning + Acting) loop — production-grade execution engine.
 *
 * ## Safeguards applied (Option B)
 *
 * | Safeguard                | Mechanism                                           |
 * |--------------------------|-----------------------------------------------------|
 * | Concurrent task safety   | [executionMutex] — non-reentrant singleton          |
 * | Context window overflow  | [pruneHistory] — sliding window, ~3k token ceiling  |
 * | Multiline JSON input     | Multi-line Action Input: collector                  |
 * | Garbage max-steps result | Clean error from [AgentMemory], not raw LLM dump    |
 * | Runaway token cost       | [ExecutionBudget] — hard ceiling per run            |
 * | Tool output garbage      | [ToolOutputValidator] — JSON check before ingestion |
 * | Silent failures          | [ExecutionTrace] — structured per-step record       |
 *
 * ## Priority-based budgets
 *
 * | Priority | Token budget |
 * |----------|-------------|
 * | LOW      | 3,000       |
 * | NORMAL   | 6,000       |
 * | HIGH     | 9,000       |
 * | CRITICAL | 12,000      |
 *
 * SPEC_REF: COG-002 / OPTION-B
 */
@Singleton
class ReActLoop @Inject constructor(
    private val modelProvider: ModelProvider,
    private val toolRegistry: ToolRegistry,
    private val agentMemory: AgentMemory,
    private val toolOutputValidator: ToolOutputValidator
) {
    companion object {
        private const val MAX_STEPS = 12
        private const val FINAL_ANSWER_SIGNAL = "FINAL ANSWER:"
        private const val MAX_HISTORY_TOKENS  = 3_000
        private const val PRESERVE_PAIRS      = 4

        private val BUDGET_BY_PRIORITY = mapOf(
            TaskPriority.LOW      to 3_000,
            TaskPriority.NORMAL   to ExecutionBudget.DEFAULT_MAX_TOKENS,
            TaskPriority.HIGH     to 9_000,
            TaskPriority.CRITICAL to 12_000
        )
    }

    private val executionMutex = Mutex()

    private val _loopState = MutableStateFlow<LoopState>(LoopState.Idle)
    val loopState: StateFlow<LoopState> = _loopState

    /**
     * Run the ReAct loop for [task] on [localNode].
     * Budget is automatically scaled to [task.priority] unless overridden.
     */
    suspend fun execute(
        task: AgentTask,
        localNode: AgentNode,
        budget: ExecutionBudget = ExecutionBudget(
            BUDGET_BY_PRIORITY[task.priority] ?: ExecutionBudget.DEFAULT_MAX_TOKENS
        )
    ): ExecutionResult {
        executionMutex.withLock {
            val startMs = System.currentTimeMillis()
            _loopState.value = LoopState.Running(task)
            Timber.d("[ReAct] Starting '${task.title}' [${task.priority}] budget=${budget.maxTokens}")

            val history    = mutableListOf<LlmMessage>()
            val systemPrompt = buildSystemPrompt(localNode, budget)
            val stepTraces = mutableListOf<StepTrace>()

            history.add(LlmMessage(LlmRole.USER, buildTaskPrompt(task)))

            var step        = 0
            var finalAnswer: String? = null
            var outcome     = ExecutionTrace.Outcome.MAX_STEPS

            while (step < MAX_STEPS && finalAnswer == null) {
                step++
                val stepStart = System.currentTimeMillis()

                if (budget.isExhausted) {
                    Timber.w("[ReAct] Budget exhausted before step $step")
                    outcome = ExecutionTrace.Outcome.BUDGET_EXCEEDED
                    stepTraces.add(StepTrace(step, StepTrace.StepType.BUDGET_STOP, "", tokensCost = 0, elapsedMs = 0))
                    break
                }

                Timber.d("[ReAct] Step $step/$MAX_STEPS — ${budget.remaining} tokens left")
                pruneHistory(history)

                val response = modelProvider.complete(systemPrompt, history)
                val cost     = budget.charge(systemPrompt + history.joinToString("") { it.content }, response)
                history.add(LlmMessage(LlmRole.ASSISTANT, response))

                // ── Final answer? ──────────────────────────────────────────
                if (response.contains(FINAL_ANSWER_SIGNAL)) {
                    finalAnswer = response.substringAfter(FINAL_ANSWER_SIGNAL).trim()
                    outcome = ExecutionTrace.Outcome.SUCCESS
                    stepTraces.add(StepTrace(step, StepTrace.StepType.FINAL_ANSWER,
                        response.take(200), tokensCost = cost, elapsedMs = System.currentTimeMillis() - stepStart))
                    break
                }

                // ── Tool call? ─────────────────────────────────────────────
                val toolCall = parseToolCall(response)
                if (toolCall != null) {
                    val (toolName, toolInput) = toolCall
                    Timber.d("[ReAct] Tool: $toolName | ${toolInput.take(60)}")

                    val toolResult = runCatching {
                        toolRegistry.executeTool(toolName, toolInput)
                    }.getOrElse {
                        Timber.e(it, "[ReAct] Tool threw")
                        ToolResult.failure("Tool error: ${it.message}")
                    }

                    val validation = toolOutputValidator.validate(toolName, toolResult.summary)
                    val observation = when (validation) {
                        is ValidationResult.Ok      -> "Observation: ${validation.sanitized}"
                        is ValidationResult.Invalid -> {
                            Timber.w("[ReAct] $toolName invalid: ${validation.reason}")
                            "Observation: ${validation.sanitized}"
                        }
                    }
                    history.add(LlmMessage(LlmRole.USER, observation))

                    val toolCost = budget.chargeTool(toolInput, toolResult.summary)
                    agentMemory.store("task_${task.taskId}_step_$step", "Tool=$toolName|Result=${toolResult.summary.take(200)}")

                    stepTraces.add(StepTrace(
                        stepNumber        = step,
                        type              = StepTrace.StepType.TOOL_CALL,
                        llmResponseSnippet = response.take(200),
                        toolName          = toolName,
                        toolInputSnippet  = toolInput.take(100),
                        toolResultSnippet = toolResult.summary.take(100),
                        tokensCost        = cost + toolCost,
                        elapsedMs         = System.currentTimeMillis() - stepStart,
                        validationError   = (validation as? ValidationResult.Invalid)?.reason
                    ))
                } else {
                    history.add(LlmMessage(LlmRole.USER, "Continue. Prefix final answer with '$FINAL_ANSWER_SIGNAL'"))
                    stepTraces.add(StepTrace(step, StepTrace.StepType.THOUGHT_ONLY,
                        response.take(200), tokensCost = cost, elapsedMs = System.currentTimeMillis() - stepStart))
                }
            }

            // ── Finalise ───────────────────────────────────────────────────
            val result = finalAnswer ?: run {
                if (outcome != ExecutionTrace.Outcome.BUDGET_EXCEEDED) outcome = ExecutionTrace.Outcome.MAX_STEPS
                _loopState.value = LoopState.Error(task, outcome.name)
                val lastMem = agentMemory.recall("task_${task.taskId}_step_$step")?.value
                "Agent could not complete '${task.title}' ($outcome, $step steps). " +
                    if (lastMem != null) "Last action: $lastMem" else "No tool actions recorded."
            }

            if (finalAnswer != null) _loopState.value = LoopState.Completed(task, result)

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
    // History pruning
    // -----------------------------------------------------------------------

    private fun pruneHistory(history: MutableList<LlmMessage>) {
        if (history.sumOf { it.content.length / 4 } <= MAX_HISTORY_TOKENS) return
        val keepCount = PRESERVE_PAIRS * 2
        if (history.size <= keepCount + 1) return

        val toSummarise = history.subList(1, history.size - keepCount)
        val summary = "Summary of previous steps: " + toSummarise.mapIndexed { i, m ->
            val prefix = if (m.role == LlmRole.ASSISTANT) "Agent" else "Obs"
            "[$prefix ${i+1}] ${m.content.take(120).replace('\n', ' ')}"
        }.joinToString("; ")

        toSummarise.clear()
        history.add(1, LlmMessage(LlmRole.USER, summary))
        Timber.d("[ReAct] History pruned — ~${history.sumOf { it.content.length / 4 }} tokens")
    }

    // -----------------------------------------------------------------------
    // Multiline Action Input parser
    // -----------------------------------------------------------------------

    private fun parseToolCall(response: String): Pair<String, String>? {
        val lines      = response.lines()
        val actionIdx  = lines.indexOfFirst { it.trimStart().startsWith("Action:") }
        val inputIdx   = lines.indexOfFirst { it.trimStart().startsWith("Action Input:") }
        if (actionIdx < 0 || inputIdx < 0) return null

        val toolName = lines[actionIdx].substringAfter("Action:").trim()
        if (toolName.isBlank()) return null

        val rawJson = lines.drop(inputIdx)
            .mapIndexed { i, l -> if (i == 0) l.substringAfter("Action Input:").trim() else l }
            .takeWhile { t -> !t.trimStart().let { it.startsWith("Thought:") || it.startsWith("Action:") || it.startsWith(FINAL_ANSWER_SIGNAL) } }
            .joinToString("\n").trim()

        return if (rawJson.isNotBlank()) toolName to rawJson else null
    }

    // -----------------------------------------------------------------------
    // Prompt builders
    // -----------------------------------------------------------------------

    private fun buildSystemPrompt(node: AgentNode, budget: ExecutionBudget) = """
You are an autonomous AI agent on Android device "${node.displayName}" in a decentralised mesh network.
Reason step by step. Use tools when needed. Be concise — every token counts.

FORMAT:
Thought: <reasoning>
Action: <tool_name>
Action Input: <json — may span multiple lines>

When done:
FINAL ANSWER: <answer>

TOOLS:
${toolRegistry.availableTools().joinToString("\n") { "  - ${it.name}: ${it.description}" }}

NODE: capabilities=${node.capabilities.joinToString()} battery=${node.batteryLevel}% ownerPresent=${node.isOwnerPresent}
BUDGET: ${budget.remaining} tokens remaining.
    """.trimIndent()

    private fun buildTaskPrompt(task: AgentTask) =
        "TASK: ${task.title}\nDESCRIPTION: ${task.description}\nPRIORITY: ${task.priority}\nBegin."

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
