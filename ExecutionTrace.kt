package com.meshai.agent

/**
 * Structured record of a complete ReAct execution run.
 *
 * Captured by [ReActLoop] and returned alongside the final result string.
 * Intended for:
 * - Dashboard display (DashboardViewModel can expose this to the task detail screen)
 * - Debugging (why did the agent fail / burn budget / stall?)
 * - Persistent audit log (store in Room alongside the completed AgentTask)
 *
 * ## Structure
 *
 * One [ExecutionTrace] per [AgentTask] execution.
 * Contains an ordered list of [StepTrace] — one per ReAct loop iteration.
 * Each [StepTrace] records what happened at that step:
 * whether the LLM produced a tool call or a thought-only response,
 * which tool was called, what it returned, how many tokens were spent,
 * and how long the step took.
 */
data class ExecutionTrace(
    val taskId: String,
    val taskTitle: String,
    val steps: List<StepTrace>,
    val finalResult: String,
    val outcome: Outcome,
    val budget: BudgetSnapshot,
    val totalElapsedMs: Long
) {
    enum class Outcome {
        SUCCESS,        // LLM reached FINAL ANSWER
        BUDGET_EXCEEDED, // Budget ceiling hit before final answer
        MAX_STEPS,      // Step limit hit before final answer
        ERROR           // Exception during execution
    }

    val stepCount: Int get() = steps.size

    /** Human-readable one-liner for Timber / notification subtitle. */
    fun summary(): String = buildString {
        append("Task '${taskTitle}': ${outcome.name} ")
        append("in ${stepCount} steps, ${budget.tokensSpent} tokens, ${totalElapsedMs}ms")
        if (outcome != Outcome.SUCCESS) {
            append(" | Result: ${finalResult.take(80)}")
        }
    }
}

/**
 * Record of a single iteration within the ReAct loop.
 */
data class StepTrace(
    val stepNumber: Int,
    val type: StepType,
    val llmResponseSnippet: String,     // First 200 chars of LLM output
    val toolName: String? = null,        // Set if type == TOOL_CALL
    val toolInputSnippet: String? = null, // First 100 chars of JSON input
    val toolResultSnippet: String? = null, // First 100 chars of tool result
    val tokensCost: Int,
    val elapsedMs: Long,
    val validationError: String? = null  // Set if tool output failed validation
) {
    enum class StepType {
        TOOL_CALL,      // LLM produced Action + Action Input
        THOUGHT_ONLY,   // LLM produced Thought with no valid tool call
        FINAL_ANSWER,   // LLM produced FINAL ANSWER signal
        BUDGET_STOP,    // Step aborted due to budget exhaustion
        PARSE_ERROR     // Action Input JSON was malformed
    }
}
